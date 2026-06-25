# ============================================================
# 전 플랫폼 RAW 수집 → LLM 정규화 전체 파이프라인 테스트
# 실행 위치: scraper/ 디렉토리
#   python test.py              # 전 플랫폼 (대화형)
#   python test.py tving        # 특정 플랫폼만 (대화형)
#   python test.py tving wavve  # 복수 지정 (대화형)
#   python test.py --save       # 전 플랫폼 비대화형 + 결과 txt 저장
#   python test.py tving --save # 특정 플랫폼 비대화형 + 결과 txt 저장
#
# 흐름: [플랫폼 N] RAW 수집 전체 출력
#         → (대화형) 엔터 / (--save) 자동 진행
#         → LLM 정규화 결과 출력
#         → [플랫폼 N+1] ...
#         → (--save) test_result_YYYYMMDD_HHMMSS.txt 저장
# ============================================================
import sys
import io
import os
import re
import json
import logging
import traceback
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

sys.path.insert(0, ".")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger("test_pipeline")

# ── ANSI 색상 제거 헬퍼 ───────────────────────────────────
ANSI_ESCAPE = re.compile(r"\033\[[0-9;]*m")

def strip_ansi(text: str) -> str:
    return ANSI_ESCAPE.sub("", text)

# ── 파일 동시 출력 (Tee) ──────────────────────────────────
class TeeOutput:
    """stdout을 화면과 내부 버퍼에 동시에 기록한다."""
    def __init__(self):
        self._original = sys.stdout
        self._buf = io.StringIO()

    def write(self, text):
        self._original.write(text)
        self._buf.write(text)

    def flush(self):
        self._original.flush()

    def getvalue(self) -> str:
        return strip_ansi(self._buf.getvalue())

    def restore(self):
        sys.stdout = self._original

# ── 전역 설정 (main에서 채움) ─────────────────────────────
SAVE_MODE: bool = False   # --save 플래그

# ── 색상 출력 헬퍼 ────────────────────────────────────────
GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
RESET  = "\033[0m"

def ok(msg):   print(f"  {GREEN}✅ {msg}{RESET}")
def fail(msg): print(f"  {RED}❌ {msg}{RESET}")
def warn(msg): print(f"  {YELLOW}⚠️  {msg}{RESET}")
def info(msg): print(f"  {CYAN}ℹ️  {msg}{RESET}")

def pause(msg="다음 단계로 진행하려면 엔터를 누르세요..."):
    if SAVE_MODE:
        print(f"\n  [ {msg} — 자동 진행 ]")
        return
    print(f"\n  {YELLOW}[ {msg} ]{RESET}")
    input()

# ── 결과 누산 ─────────────────────────────────────────────
results = []   # (platform, step, status, detail)

def record(platform, step, status, detail=""):
    results.append((platform, step, status, detail))


# ── STEP 1: RAW 수집 ──────────────────────────────────────
def test_raw(name, scraper):
    print(f"\n{'═'*55}")
    print(f"  🌐 [{name.upper()}] STEP 1 — RAW 수집")
    print(f"{'═'*55}")

    raw_plans = None

    # scrap_plans
    print(f"\n{'─'*50}")
    print(f"  scrap_plans()")
    print(f"{'─'*50}")
    try:
        raw_plans = scraper.scrap_plans()
        ok(f"scrap_plans() — {len(raw_plans)}건")

        required = ["platform", "plan_name", "billing_cycle", "price", "services"]
        missing_fields = set()
        for item in raw_plans:
            for f in required:
                if f not in item:
                    missing_fields.add(f)
        if missing_fields:
            warn(f"일부 항목에 필드 누락: {missing_fields}")
            record(name, "scrap_plans", "WARN", f"필드 누락: {missing_fields}")
        else:
            ok("필수 필드 모두 존재 (platform/plan_name/billing_cycle/price/services)")
            record(name, "scrap_plans", "OK", f"{len(raw_plans)}건")

        # 전체 출력
        print(f"\n  [전체 RAW 출력 — {len(raw_plans)}건]")
        print(json.dumps(raw_plans, ensure_ascii=False, indent=2))

        bundles = [r for r in raw_plans if len(r.get("services", [])) >= 2]
        if bundles:
            info(f"번들 상품 {len(bundles)}건 포함 → LLM에서 PROMOTION_CRAWL 분기 예상")

    except NotImplementedError as e:
        warn(f"scrap_plans() 미구현: {e}")
        record(name, "scrap_plans", "SKIP", str(e))
    except Exception as e:
        fail(f"scrap_plans() 오류: {e}")
        record(name, "scrap_plans", "FAIL", str(e))
        traceback.print_exc()

    # scrap_cautions
    print(f"\n{'─'*50}")
    print(f"  scrap_cautions()")
    print(f"{'─'*50}")
    try:
        cautions = scraper.scrap_cautions()
        ok(f"scrap_cautions() — {len(cautions)}개 섹션")
        for heading, items in cautions.items():
            print(f"    [{heading}] {len(items)}건")
            for item in items[:3]:
                print(f"      • {item[:80]}")
        record(name, "scrap_cautions", "OK", f"{len(cautions)}섹션")
    except NotImplementedError as e:
        warn(f"scrap_cautions() 미구현: {e}")
        record(name, "scrap_cautions", "SKIP", str(e))
    except Exception as e:
        fail(f"scrap_cautions() 오류: {e}")
        record(name, "scrap_cautions", "FAIL", str(e))

    # scrap_company_info
    print(f"\n{'─'*50}")
    print(f"  scrap_company_info()")
    print(f"{'─'*50}")
    company_info = []
    try:
        company_info = scraper.scrap_company_info()
        ok(f"scrap_company_info() — {len(company_info)}줄")
        for line in company_info:
            print(f"    {line}")
        record(name, "scrap_company_info", "OK", f"{len(company_info)}줄")
    except NotImplementedError as e:
        warn(f"scrap_company_info() 미구현: {e}")
        record(name, "scrap_company_info", "SKIP", str(e))
    except Exception as e:
        fail(f"scrap_company_info() 오류: {e}")
        record(name, "scrap_company_info", "FAIL", str(e))

    # scrap_logo
    print(f"\n{'─'*50}")
    print(f"  scrap_logo()")
    print(f"{'─'*50}")
    try:
        logo = scraper.scrap_logo()
        ok(f"scrap_logo() — {len(logo)}자")
        print(f"    {logo[:100]}{'...' if len(logo) > 100 else ''}")
        record(name, "scrap_logo", "OK", f"{len(logo)}자")
    except NotImplementedError as e:
        warn(f"scrap_logo() 미구현: {e}")
        record(name, "scrap_logo", "SKIP", str(e))
    except Exception as e:
        fail(f"scrap_logo() 오류: {e}")
        record(name, "scrap_logo", "FAIL", str(e))

    return raw_plans, company_info


# ── STEP 2: LLM 정규화 ───────────────────────────────────
def test_llm(name, raw_plans, company_info, normalizer):
    print(f"\n{'═'*55}")
    print(f"  🤖 [{name.upper()}] STEP 2 — LLM 정규화")
    print(f"{'═'*55}")

    if not raw_plans:
        warn("RAW 데이터 없음 — LLM 테스트 스킵")
        record(name, "normalize_plans", "SKIP", "raw 없음")
        return

    # scheduler.py와 동일하게 service_info 조립
    import re
    platform_to_code = {
        "tving":      "TVING",
        "netflix":    "NETFLIX",
        "wavve":      "WAVVE",
        "disneyplus": "DISNEY_PLUS",
        "watcha":     "WATCHA",
    }
    service_code = platform_to_code.get(name, name.upper())

    phone = None
    email = None
    for line in (company_info or []):
        if not phone:
            m = re.search(r"[\d]{2,4}-[\d]{3,4}-[\d]{4}", line)
            if m:
                phone = m.group()
        if not email:
            m = re.search(r"[\w.+-]+@[\w-]+\.[a-zA-Z]+", line)
            if m:
                email = m.group()

    service_info = {
        "service_code": service_code,
        "service_name": name,
        "phone":        phone,
        "email":        email,
    }

    try:
        norms = normalizer.normalize_plans(raw_plans, service_info=service_info)
        ok(f"normalize_plans() — {len(norms)}건 반환")
        record(name, "normalize_plans", "OK", f"{len(norms)}건")

        for norm in norms:
            job_type = norm.get("jobType")
            print(f"\n  jobType: {CYAN}{job_type}{RESET}")

            if job_type == "SERVICE_CATALOG_CRAWL":
                plans = norm.get("servicePlans", [])
                ok(f"servicePlans {len(plans)}건")

                for p in plans:
                    bc    = p.get("billingCycle")
                    price = p.get("monthlyPrice")
                    name_ = p.get("planName", "")
                    if bc == "YEARLY":
                        raw_match = next(
                            (r for r in raw_plans
                             if r.get("plan_name") == name_ and r.get("billing_cycle") == "yearly"),
                            None,
                        )
                        if raw_match:
                            expected = raw_match["price"] // 12
                            if price == expected:
                                ok(f"YEARLY 월환산 정확: {name_} → {price}원/월 (연 {raw_match['price']}÷12)")
                            else:
                                fail(f"YEARLY 월환산 오류: {name_} → {price}원 (기대: {expected}원)")
                                record(name, "yearly_calc", "FAIL",
                                       f"{name_}: {price} != {expected}")
                    print(f"    {p}")

            elif job_type == "PROMOTION_CRAWL":
                promos = norm.get("promotions", [])
                ok(f"promotions {len(promos)}건")
                for promo in promos:
                    p_type    = promo.get("promotionType")
                    svc_codes = [s["serviceCode"] for s in promo.get("services", [])]
                    print(f"    [{p_type}] {promo.get('title')} | services: {svc_codes}")

                    if p_type == "BUNDLE" and len(svc_codes) < 2:
                        fail(f"BUNDLE인데 services가 1개 이하: {svc_codes}")
                        record(name, "bundle_services", "FAIL", str(svc_codes))
                    elif p_type == "BUNDLE":
                        ok(f"BUNDLE services 정상: {svc_codes}")

            else:
                fail(f"알 수 없는 jobType: {job_type}")
                record(name, "normalize_plans", "FAIL", f"unknown jobType: {job_type}")

        print(f"\n  [전체 LLM 출력]")
        print(json.dumps(norms, ensure_ascii=False, indent=2))

    except Exception as e:
        fail(f"normalize_plans() 오류: {e}")
        record(name, "normalize_plans", "FAIL", str(e))
        traceback.print_exc()


# ── 최종 요약 ─────────────────────────────────────────────
def print_summary():
    print(f"\n{'='*55}")
    print("📋 전체 테스트 결과 요약")
    print(f"{'='*55}")

    platforms = {}
    for platform, step, status, detail in results:
        platforms.setdefault(platform, []).append((step, status, detail))

    for platform, steps in platforms.items():
        total    = len(steps)
        ok_cnt   = sum(1 for _, s, _ in steps if s == "OK")
        fail_cnt = sum(1 for _, s, _ in steps if s == "FAIL")
        skip_cnt = sum(1 for _, s, _ in steps if s == "SKIP")
        warn_cnt = sum(1 for _, s, _ in steps if s == "WARN")

        status_icon = GREEN+"✅"+RESET if fail_cnt == 0 else RED+"❌"+RESET
        print(f"\n  {status_icon} {platform}")
        print(f"     OK:{ok_cnt}  FAIL:{fail_cnt}  SKIP:{skip_cnt}  WARN:{warn_cnt}  / 총{total}건")
        for step, status, detail in steps:
            if status == "FAIL":
                print(f"     {RED}FAIL{RESET} {step}: {detail[:80]}")
            elif status == "SKIP":
                print(f"     {YELLOW}SKIP{RESET} {step}")

    total_fail = sum(1 for _, _, s, _ in results if s == "FAIL")
    print(f"\n{'─'*55}")
    if total_fail == 0:
        print(f"{GREEN}  전체 테스트 통과 ✅{RESET}")
    else:
        print(f"{RED}  FAIL {total_fail}건 — 위 내용 확인 필요{RESET}")
    print(f"{'='*55}\n")


# ── 메인 ──────────────────────────────────────────────────
def main():
    global SAVE_MODE

    available = {
        "tving":      ("ott.tving",      "TvingScraper"),
        "netflix":    ("ott.netflix",     "NetflixScraper"),
        "wavve":      ("ott.wavve",       "WavveScraper"),
        "disneyplus": ("ott.disneyplus",  "DisneyPlusScraper"),
        "watcha":     ("ott.watcha",      "WatchaScraper"),
    }

    # ── 인수 파싱 ─────────────────────────────────────────
    raw_args   = sys.argv[1:]
    SAVE_MODE  = "--save" in [a.lower() for a in raw_args]
    args       = [a.lower() for a in raw_args if a.lower() != "--save"]
    targets    = {k: v for k, v in available.items() if k in args} if args else available

    # ── --save 모드: stdout → TeeOutput ──────────────────
    tee = None
    if SAVE_MODE:
        tee = TeeOutput()
        sys.stdout = tee

    run_start = datetime.now()

    print(f"\n{'='*55}")
    mode_label = "비대화형 · 파일 저장 모드" if SAVE_MODE else "단계별 확인 모드"
    print(f"🚀 전 플랫폼 파이프라인 테스트 ({mode_label})")
    print(f"   대상: {list(targets.keys())}")
    print(f"   시작: {run_start.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'='*55}")

    # ── 스크래퍼 인스턴스 생성 ───────────────────────────
    scrapers = {}
    for name, (module_path, class_name) in targets.items():
        try:
            import importlib
            mod = importlib.import_module(module_path)
            scrapers[name] = getattr(mod, class_name)()
            ok(f"{class_name} 로드 완료")
        except Exception as e:
            fail(f"{class_name} 로드 실패: {e}")
            record(name, "import", "FAIL", str(e))

    # ── LLM 미리 로드 ────────────────────────────────────
    print(f"\n{'─'*55}")
    print("  LLM 모델 로드 중...")
    print(f"{'─'*55}")
    normalizer = None
    try:
        from llm.normalizer import LLMNormalizer
        normalizer = LLMNormalizer()
        ok("LLMNormalizer 초기화 완료")
    except Exception as e:
        fail(f"LLMNormalizer 로드 실패: {e}")
        traceback.print_exc()
        print_summary()
        if tee:
            tee.restore()
        return

    # ── 플랫폼별 순차 실행 ───────────────────────────────
    platform_list = list(scrapers.items())
    for idx, (name, scraper) in enumerate(platform_list, 1):
        print(f"\n\n{'#'*55}")
        print(f"  [{idx}/{len(platform_list)}] {name.upper()}")
        print(f"{'#'*55}")

        # STEP 1: RAW 수집
        raw_plans, company_info = test_raw(name, scraper)

        # 엔터 대기 (대화형) 또는 자동 진행 (--save)
        pause(f"[{name}] RAW 수집 완료 — 엔터를 누르면 LLM 정규화를 시작합니다")

        # STEP 2: LLM 정규화
        test_llm(name, raw_plans, company_info, normalizer)

        # 마지막 플랫폼이 아니면 다음 플랫폼으로
        if idx < len(platform_list):
            pause(f"[{name}] 완료 — 엔터를 누르면 다음 플랫폼 [{platform_list[idx][0]}]으로 넘어갑니다")

    print_summary()

    # ── --save: 결과 파일 저장 ───────────────────────────
    if tee:
        tee.restore()
        run_end  = datetime.now()
        elapsed  = (run_end - run_start).total_seconds()
        content  = tee.getvalue()

        timestamp = run_start.strftime("%Y%m%d_%H%M%S")
        filename  = f"test_result_{timestamp}.txt"

        header = (
            f"{'='*55}\n"
            f"  테스트 결과 저장 파일\n"
            f"  실행 시작 : {run_start.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  실행 종료 : {run_end.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  소요 시간 : {elapsed:.1f}초\n"
            f"  대상 플랫폼: {list(targets.keys())}\n"
            f"{'='*55}\n\n"
        )

        with open(filename, "w", encoding="utf-8") as f:
            f.write(header + content)

        print(f"\n✅ 결과 저장 완료 → {filename}  ({elapsed:.1f}초 소요)")


if __name__ == "__main__":
    main()