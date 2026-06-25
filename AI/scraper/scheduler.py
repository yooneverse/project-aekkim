"""
APScheduler 진입점

호출 주기:
    매일 03:00      scrap_plans()       → LLM 정규화 → DB UPSERT
    매주 월요일      PromotionScraper.scrap() → LLM 정규화 → DB UPSERT
    매일 03:00      NaverMembershipScraper.scrap() → normalize_card_benefit → DB UPSERT
    매월 1일         scrap_company_info() → DB UPSERT (정규화 없이 직접 저장, 로고는 고정 경로)

LLM 호출 단위:
    normalize_plans():      플랫폼 1개씩 호출
    normalize_promotions(): 공지/프로모션 1건씩 호출
"""
import logging

from apscheduler.schedulers.blocking import BlockingScheduler

from ott.tving      import TvingScraper
from ott.netflix    import NetflixScraper
from ott.wavve      import WavveScraper
from ott.disneyplus import DisneyPlusScraper
from ott.watcha     import WatchaScraper
from promotion.tving           import TvingPromotionScraper
from promotion.wavve           import WavvePromotionScraper
from promotion.disneyplus      import DisneyPlusPromotionScraper
from promotion.watcha          import WatchaPromotionScraper
from promotion.naver_membership import NaverMembershipScraper
from llm.normalizer import LLMNormalizer
from db.client      import DBClient
from base           import SERVICE_LOGO_URLS

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

scrapers   = [
    TvingScraper(),
    NetflixScraper(),
    WavveScraper(),
    DisneyPlusScraper(),
    WatchaScraper(),
]
promotion_scrapers = [
    TvingPromotionScraper(),
    WavvePromotionScraper(),
    DisneyPlusPromotionScraper(),
    WatchaPromotionScraper(),
]
naver_membership_scraper = NaverMembershipScraper()
normalizer = LLMNormalizer()
db         = DBClient()


# ── 스케줄 작업 ────────────────────────────────────────────────────────────

def run_daily_plans():
    """매일 03:00 — 요금제 + 회사 정보 수집 → LLM 정규화 → DB UPSERT

    scrap_plans()와 scrap_company_info()를 함께 수집한 뒤 normalizer에 전달한다.
    services[] (logoUrl, customerServicePhone 등)는 LLM이 아닌 Python이 직접 조립.

    normalize_plans()가 list[dict]를 반환하므로 jobType별로 분기 저장:
        SERVICE_CATALOG_CRAWL → upsert_service_plans()
        PROMOTION_CRAWL       → upsert_promotions()  (번들 상품)
    """
    logger.info("=== [daily] run_daily_plans 시작 ===")
    for scraper in scrapers:
        platform     = scraper.__class__.__name__
        service_code = _platform_to_code(platform)
        try:
            raw = scraper.scrap_plans()

            # 회사 정보 수집 (실패해도 plans 저장은 계속)
            phone = None
            try:
                company_info = scraper.scrap_company_info()
                phone = _extract_phone(company_info)
            except NotImplementedError:
                pass
            except Exception as e:
                logger.warning("[daily] %s scrap_company_info 오류 (무시): %s", platform, e)

            service_info = {
                "service_code": service_code,
                "service_name": platform,
                "phone":        phone,
            }

            norms = normalizer.normalize_plans(raw, service_info=service_info)

            for norm in norms:
                job_type = norm.get("jobType")
                if job_type == "SERVICE_CATALOG_CRAWL":
                    db.upsert_service_plans(norm)
                elif job_type == "PROMOTION_CRAWL":
                    db.upsert_promotions(norm)
                else:
                    logger.warning("[daily] %s 알 수 없는 jobType 스킵: %s", platform, job_type)

            logger.info("[daily] %s 완료 — raw %d건, norm %d건", platform, len(raw), len(norms))
        except NotImplementedError as e:
            logger.warning("[daily] %s 스킵 (미구현): %s", platform, e)
        except Exception as e:
            logger.error("[daily] %s 오류: %s", platform, e)
    logger.info("=== [daily] run_daily_plans 완료 ===")


def run_weekly_promotions():
    """매주 월요일 — 프로모션 수집 → 정규화 → DB UPSERT

    스크래퍼별 전용 normalizer 메서드 사용:
        TvingPromotionScraper      → normalize_tving_promotion()   (LLM, 필터링)
        WavvePromotionScraper      → normalize_wavve_promotion()   (Python 직접 조립)
        DisneyPlusPromotionScraper → normalize_promotions()        (기존 LLM)
        WatchaPromotionScraper     → normalize_promotions()        (기존 LLM)
        NaverMembershipScraper     → normalize_naver_membership()  (쿠폰 summary 포함)
    """
    logger.info("=== [weekly] run_weekly_promotions 시작 ===")

    # ── 스크래퍼별 normalizer 매핑 ────────────────────────────────────────
    def _normalize(scraper_name: str, notice: dict) -> dict:
        if scraper_name == "TvingPromotionScraper":
            return normalizer.normalize_tving_promotion(notice)
        elif scraper_name == "WavvePromotionScraper":
            return normalizer.normalize_wavve_promotion(notice)
        else:
            return normalizer.normalize_promotions(notice)

    # ── OTT 공지/이벤트 ───────────────────────────────────────────────────
    for scraper in promotion_scrapers:
        platform = scraper.__class__.__name__
        try:
            notices = scraper.scrap()
            saved = 0
            for notice in notices:
                try:
                    norm = _normalize(platform, notice)
                    # promotions 빈 리스트면 저장 불필요 (tving 필터링 결과)
                    if not norm.get("promotions"):
                        logger.info("[weekly] %s 공지 — 프로모션 없음, 스킵", platform)
                        continue
                    db.upsert_promotions(norm)
                    saved += 1
                except Exception as e:
                    logger.error("[weekly] %s 공지 정규화/저장 오류: %s", platform, e)
            logger.info("[weekly] %s 완료 — 수집 %d건 / 저장 %d건", platform, len(notices), saved)
        except Exception as e:
            logger.error("[weekly] %s 오류: %s", platform, e)

    # ── 네이버 멤버십 ─────────────────────────────────────────────────────
    try:
        details = naver_membership_scraper.scrap()
        for detail in details:
            try:
                norm = normalizer.normalize_naver_membership(detail)
                if norm.get("promotions"):
                    db.upsert_promotions(norm)
            except Exception as e:
                logger.error("[weekly] NaverMembership 정규화/저장 오류: %s", e)
        logger.info("[weekly] NaverMembership 완료 — %d건", len(details))
    except Exception as e:
        logger.error("[weekly] NaverMembership 오류: %s", e)

    logger.info("=== [weekly] run_weekly_promotions 완료 ===")


def run_monthly_meta():
    """매월 1일 — 사업자 정보 수집 → DB UPSERT (LLM 정규화 없음)
    로고 URL은 백엔드 assets 고정 경로 사용 — 스크래핑 불필요.
    """
    logger.info("=== [monthly] run_monthly_meta 시작 ===")
    for scraper in scrapers:
        platform     = scraper.__class__.__name__
        service_code = _platform_to_code(platform)
        company_info = []

        try:
            company_info = scraper.scrap_company_info()
        except NotImplementedError as e:
            logger.warning("[monthly] %s scrap_company_info 스킵 (미구현): %s", platform, e)
        except Exception as e:
            logger.error("[monthly] %s scrap_company_info 오류: %s", platform, e)

        logo_url = SERVICE_LOGO_URLS.get(service_code)

        try:
            db.upsert_service_plans({
                "jobType":      "SERVICE_CATALOG_CRAWL",
                "collectedAt":  _now_kst(),
                "services":     [{"code":                 service_code,
                                  "name":                 platform,
                                  "category":             "OTT",
                                  "logoUrl":              logo_url,
                                  "cancelGuideUrl":       None,
                                  "customerServicePhone": _extract_phone(company_info),
                                  "contactEmail":         None}],
                "servicePlans": [],
            })
            logger.info("[monthly] %s 메타 저장 완료 (logoUrl=%s)", platform, logo_url)
        except Exception as e:
            logger.error("[monthly] %s 메타 저장 오류: %s", platform, e)

    logger.info("=== [monthly] run_monthly_meta 완료 ===")


# ── 유틸 ──────────────────────────────────────────────────────────────────

def _now_kst() -> str:
    from datetime import datetime, timezone, timedelta
    return datetime.now(timezone(timedelta(hours=9))).isoformat()


def _platform_to_code(class_name: str) -> str:
    """스크래퍼 클래스명 → 서비스 코드 변환"""
    mapping = {
        "TvingScraper":       "TVING",
        "NetflixScraper":     "NETFLIX",
        "WavveScraper":       "WAVVE",
        "DisneyPlusScraper":  "DISNEY_PLUS",
        "WatchaScraper":      "WATCHA",
    }
    return mapping.get(class_name, class_name.upper())


def _extract_phone(company_info: list[str]) -> str | None:
    """사업자 정보 줄 목록에서 전화번호 추출 (없으면 None)"""
    import re
    for line in company_info:
        match = re.search(r"[\d]{2,4}-[\d]{3,4}-[\d]{4}", line)
        if match:
            return match.group()
    return None


# ── 스케줄러 실행 ─────────────────────────────────────────────────────────

scheduler = BlockingScheduler(timezone="Asia/Seoul")

# ── 운영 스케줄 ───────────────────────────────────────────────────────────
# scheduler.add_job(run_daily_plans,       "cron", hour=3,            id="daily_plans")
# scheduler.add_job(run_weekly_promotions, "cron", day_of_week="mon", id="weekly_promotions")
# scheduler.add_job(run_monthly_meta,      "cron", day=1,             id="monthly_meta")

# ── 테스트 스케줄 (매일 1회) ─────────────────────────────────────────────
# scheduler.add_job(run_daily_plans,       "cron", hour=9,  id="daily_plans")
# scheduler.add_job(run_weekly_promotions, "cron", hour=9,  id="weekly_promotions")
# scheduler.add_job(run_monthly_meta,      "cron", hour=9,  id="monthly_meta")


# ── 수동 실행 ─────────────────────────────────────────────────────────────

JOBS = {
    "plans":      run_daily_plans,
    "promotions": run_weekly_promotions,
    "meta":       run_monthly_meta,
}

def run_manual(job: str | None = None) -> None:
    """스케줄러 없이 특정 job을 즉시 실행.

    Args:
        job: "plans" | "promotions" | "meta" | None (None이면 전체 실행)

    사용 예:
        python scheduler.py plans
        python scheduler.py promotions
        python scheduler.py meta
        python scheduler.py all
    """
    targets = JOBS if job is None else {job: JOBS[job]}
    for name, fn in targets.items():
        logger.info("=== [manual] %s 수동 실행 시작 ===", name)
        try:
            fn()
        except Exception as e:
            logger.error("[manual] %s 오류: %s", name, e)
        logger.info("=== [manual] %s 수동 실행 완료 ===", name)


if __name__ == "__main__":
    import sys

    args = [a.lower() for a in sys.argv[1:]]

    # ── 수동 실행 모드 ────────────────────────────────────────────────────
    # python scheduler.py plans
    # python scheduler.py promotions
    # python scheduler.py meta
    # python scheduler.py all
    if args:
        target = args[0]
        if target == "all":
            run_manual(None)
        elif target in JOBS:
            run_manual(target)
        else:
            print(f"알 수 없는 job: {target}")
            print(f"사용 가능한 job: {list(JOBS.keys())} | all")
            sys.exit(1)
    else:
        # ── 스케줄러 시작 모드 ────────────────────────────────────────────
        logger.info("스케줄러 시작")
        try:
            scheduler.start()
        except (KeyboardInterrupt, SystemExit):
            logger.info("스케줄러 종료")