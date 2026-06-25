# ============================================================
# 카드고릴라 스크래핑 → normalize_card_benefit 전체 파이프라인 테스트
# 실행 위치: scraper/ 디렉토리
#
#   python test_card.py              # 신용+체크 첫 번째 카드 1장씩 (대화형)
#   python test_card.py credit       # 신용카드만
#   python test_card.py check        # 체크카드만
#   python test_card.py --all        # 신용+체크 전체 카드 (시간 오래 걸림)
#   python test_card.py --save       # 비대화형 + 결과 txt 저장
#   python test_card.py credit --save
#
# 흐름: 링크 수집
#         → (대화형) 엔터
#         → 카드 상세 스크랩
#         → (대화형) 엔터
#         → normalize_card_benefit 결과 출력
#         → (--save) test_card_result_YYYYMMDD_HHMMSS.txt 저장
# ============================================================
import sys
import io
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
logger = logging.getLogger("test_card")

# ── ANSI 색상 ─────────────────────────────────────────────
ANSI_ESCAPE = re.compile(r"\033\[[0-9;]*m")

def strip_ansi(text: str) -> str:
    return ANSI_ESCAPE.sub("", text)

GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
RESET  = "\033[0m"

def ok(msg):   print(f"  {GREEN}✅ {msg}{RESET}")
def fail(msg): print(f"  {RED}❌ {msg}{RESET}")
def warn(msg): print(f"  {YELLOW}⚠️  {msg}{RESET}")
def info(msg): print(f"  {CYAN}ℹ️  {msg}{RESET}")

# ── Tee 출력 ──────────────────────────────────────────────
class TeeOutput:
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

# ── 전역 설정 ─────────────────────────────────────────────
SAVE_MODE: bool = False
ALL_MODE:  bool = False  # --all: 전체 카드 수집

results = []  # (card_type, step, status, detail)

def record(card_type, step, status, detail=""):
    results.append((card_type, step, status, detail))

def pause(msg="다음 단계로 진행하려면 엔터를 누르세요..."):
    if SAVE_MODE:
        print(f"\n  [ {msg} — 자동 진행 ]")
        return
    print(f"\n  {YELLOW}[ {msg} ]{RESET}")
    input()


# ── STEP 1: 링크 수집 ─────────────────────────────────────
def test_links(card_type: str, scraper) -> list[str]:
    from card.card_gorilla import SEARCH_URLS

    print(f"\n{'═'*55}")
    print(f"  🔗 [{card_type.upper()}] STEP 1 — 카드 링크 수집")
    print(f"{'═'*55}")

    try:
        url   = SEARCH_URLS[card_type]
        links = scraper.get_card_links(url)

        if not links:
            fail("링크 수집 결과 0개")
            record(card_type, "get_card_links", "FAIL", "0개")
            return []

        ok(f"get_card_links() — {len(links)}개")
        record(card_type, "get_card_links", "OK", f"{len(links)}개")

        # 링크 샘플 출력
        sample = links[:3]
        for i, link in enumerate(sample, 1):
            print(f"    [{i}] {link}")
        if len(links) > 3:
            info(f"... 외 {len(links) - 3}개")

        return links

    except Exception as e:
        fail(f"get_card_links() 오류: {e}")
        record(card_type, "get_card_links", "FAIL", str(e))
        traceback.print_exc()
        return []


# ── STEP 2: 카드 상세 스크랩 ──────────────────────────────
def test_scrape(card_type: str, links: list[str], scraper) -> list[dict]:
    print(f"\n{'═'*55}")
    print(f"  🃏 [{card_type.upper()}] STEP 2 — 카드 상세 스크랩")
    print(f"{'═'*55}")

    targets = links if ALL_MODE else links[:1]
    info(f"스크랩 대상: {len(targets)}장{'  (--all 플래그로 전체 수집 가능)' if not ALL_MODE else ''}")

    details = []
    for i, link in enumerate(targets, 1):
        print(f"\n  {'─'*50}")
        print(f"  [{i}/{len(targets)}] {link}")
        print(f"  {'─'*50}")
        try:
            detail = scraper.scrap_card_detail(link, card_type=card_type)
            details.append(detail)

            # 필수 필드 검증
            required = ["card_name", "brand", "img_url", "meta_info", "benefits", "source_url", "detail_url"]
            missing  = [f for f in required if f not in detail]
            if missing:
                warn(f"필드 누락: {missing}")
                record(card_type, f"scrap_detail_{i}", "WARN", f"누락: {missing}")
            else:
                ok(f"{detail.get('brand')} {detail.get('card_name')}")
                record(card_type, f"scrap_detail_{i}", "OK", detail.get("card_name", ""))

            # source_url fallback 확인
            if not detail.get("source_url"):
                warn(f"source_url 미수집 → detail_url 사용: {detail.get('detail_url')}")
            else:
                ok(f"source_url: {detail['source_url']}")

            print(f"\n  [RAW 상세 출력]")
            # benefits는 길어서 건수만 표시
            display = {**detail, "benefits": f"[{len(detail.get('benefits', []))}건 — 아래 별도 출력]"}
            print(json.dumps(display, ensure_ascii=False, indent=2))

            print(f"\n  [benefits 상세 — {len(detail.get('benefits', []))}건]")
            print(json.dumps(detail.get("benefits", []), ensure_ascii=False, indent=2))

        except Exception as e:
            fail(f"scrap_card_detail() 오류: {e}")
            record(card_type, f"scrap_detail_{i}", "FAIL", str(e))
            traceback.print_exc()

    return details


# ── STEP 3: normalize_card_benefit ────────────────────────
def test_normalize(card_type: str, details: list[dict], normalizer) -> None:
    print(f"\n{'═'*55}")
    print(f"  🤖 [{card_type.upper()}] STEP 3 — normalize_card_benefit")
    print(f"{'═'*55}")

    if not details:
        warn("스크랩 결과 없음 — 정규화 스킵")
        record(card_type, "normalize_card_benefit", "SKIP", "detail 없음")
        return

    VALID_CODES = {"NETFLIX", "DISNEY_PLUS", "TVING", "WAVVE", "WATCHA", "COUPANG_PLAY"}

    for i, detail in enumerate(details, 1):
        card_label = f"{detail.get('brand', '')} {detail.get('card_name', '')}".strip()
        print(f"\n  {'─'*50}")
        print(f"  [{i}] {card_label}")
        print(f"  {'─'*50}")
        try:
            result = normalizer.normalize_card_benefit(detail)

            # ── 구조 검증 ──────────────────────────────────
            assert result.get("jobType") == "PROMOTION_CRAWL", "jobType 불일치"
            assert result.get("collectedAt"),                  "collectedAt 없음"
            promotions = result.get("promotions", [])
            assert len(promotions) == 1,                       f"promotions 수 이상: {len(promotions)}"

            promo = promotions[0]
            assert promo.get("promotionType") == "CARD_BENEFIT",         "promotionType 불일치"
            assert promo.get("originalPrice") is None,                   "originalPrice가 None이어야 함"
            assert promo.get("discountPrice") is None,                   "discountPrice가 None이어야 함"
            assert promo.get("endsAt") == "2099-12-31T23:59:59+09:00",  "endsAt 고정값 불일치"

            ok(f"구조 검증 통과")
            record(card_type, f"normalize_{i}", "OK", card_label)

            # ── 각 필드 확인 ───────────────────────────────
            title      = promo.get("title", "")
            source_url = promo.get("sourceUrl", "")
            img_url    = promo.get("imgUrl", "")
            services   = promo.get("services", [])
            svc_codes  = [s.get("serviceCode") for s in services]

            ok(f"title     : {title}")
            ok(f"sourceUrl : {source_url or '(없음 — detail_url fallback 사용)'}")
            ok(f"imgUrl    : {img_url or '(없음)'}")

            # services 검증
            invalid_codes = [c for c in svc_codes if c not in VALID_CODES]
            if invalid_codes:
                fail(f"잘못된 serviceCode 포함: {invalid_codes}")
                record(card_type, f"services_{i}", "FAIL", str(invalid_codes))
            elif services:
                ok(f"services  : {svc_codes}")
            else:
                info("services  : [] (OTT 혜택 없음)")

            # summary(meta_info) 확인
            summary = promo.get("summary", "")
            if summary:
                ok(f"summary   : {summary[:80]}{'...' if len(summary) > 80 else ''}")
            else:
                warn("summary 비어 있음 (meta_info 수집 확인 필요)")

            print(f"\n  [전체 정규화 출력]")
            print(json.dumps(result, ensure_ascii=False, indent=2))

        except AssertionError as e:
            fail(f"구조 검증 실패: {e}")
            record(card_type, f"normalize_{i}", "FAIL", str(e))
        except Exception as e:
            fail(f"normalize_card_benefit() 오류: {e}")
            record(card_type, f"normalize_{i}", "FAIL", str(e))
            traceback.print_exc()


# ── 최종 요약 ─────────────────────────────────────────────
def print_summary():
    print(f"\n{'='*55}")
    print("📋 전체 테스트 결과 요약")
    print(f"{'='*55}")

    card_types = {}
    for ct, step, status, detail in results:
        card_types.setdefault(ct, []).append((step, status, detail))

    for ct, steps in card_types.items():
        ok_cnt   = sum(1 for _, s, _ in steps if s == "OK")
        fail_cnt = sum(1 for _, s, _ in steps if s == "FAIL")
        skip_cnt = sum(1 for _, s, _ in steps if s == "SKIP")
        warn_cnt = sum(1 for _, s, _ in steps if s == "WARN")

        icon = GREEN+"✅"+RESET if fail_cnt == 0 else RED+"❌"+RESET
        print(f"\n  {icon} {ct.upper()}")
        print(f"     OK:{ok_cnt}  FAIL:{fail_cnt}  SKIP:{skip_cnt}  WARN:{warn_cnt}  / 총{len(steps)}건")
        for step, status, detail in steps:
            if status == "FAIL":
                print(f"     {RED}FAIL{RESET} {step}: {detail[:80]}")
            elif status == "SKIP":
                print(f"     {YELLOW}SKIP{RESET} {step}")
            elif status == "WARN":
                print(f"     {YELLOW}WARN{RESET} {step}: {detail[:80]}")

    total_fail = sum(1 for _, _, s, _ in results if s == "FAIL")
    print(f"\n{'─'*55}")
    if total_fail == 0:
        print(f"{GREEN}  전체 테스트 통과 ✅{RESET}")
    else:
        print(f"{RED}  FAIL {total_fail}건 — 위 내용 확인 필요{RESET}")
    print(f"{'='*55}\n")


# ── 메인 ──────────────────────────────────────────────────
def main():
    global SAVE_MODE, ALL_MODE

    raw_args  = [a.lower() for a in sys.argv[1:]]
    SAVE_MODE = "--save" in raw_args
    ALL_MODE  = "--all"  in raw_args
    args      = [a for a in raw_args if a not in ("--save", "--all")]

    # 대상 card_type 결정
    available = ["credit", "check"]
    targets   = [a for a in args if a in available] or available

    tee = None
    if SAVE_MODE:
        tee = TeeOutput()
        sys.stdout = tee

    run_start = datetime.now()

    print(f"\n{'='*55}")
    mode_label = "비대화형·파일저장" if SAVE_MODE else "단계별 확인"
    all_label  = " · 전체 카드" if ALL_MODE else " · 첫 번째 카드 1장만"
    print(f"🚀 카드 스크래핑 파이프라인 테스트 ({mode_label}{all_label})")
    print(f"   대상: {targets}")
    print(f"   시작: {run_start.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'='*55}")

    # ── 스크래퍼 / 정규화기 로드 ─────────────────────────
    scraper    = None
    normalizer = None

    print(f"\n{'─'*55}")
    print("  모듈 로드 중...")
    print(f"{'─'*55}")
    try:
        from card.card_gorilla import CardGorillaScraper
        scraper = CardGorillaScraper()
        ok("CardGorillaScraper 로드 완료")
    except Exception as e:
        fail(f"CardGorillaScraper 로드 실패: {e}")
        traceback.print_exc()
        print_summary()
        if tee: tee.restore()
        return

    try:
        from llm.normalizer import LLMNormalizer
        normalizer = LLMNormalizer()
        ok("LLMNormalizer 로드 완료")
    except Exception as e:
        fail(f"LLMNormalizer 로드 실패: {e}")
        traceback.print_exc()
        print_summary()
        if tee: tee.restore()
        return

    # ── card_type별 순차 실행 ─────────────────────────────
    for idx, card_type in enumerate(targets, 1):
        print(f"\n\n{'#'*55}")
        print(f"  [{idx}/{len(targets)}] {card_type.upper()}")
        print(f"{'#'*55}")

        # STEP 1: 링크 수집
        links = test_links(card_type, scraper)
        if not links:
            continue

        pause(f"[{card_type}] 링크 수집 완료 — 엔터를 누르면 상세 스크랩을 시작합니다")

        # STEP 2: 상세 스크랩
        details = test_scrape(card_type, links, scraper)
        if not details:
            continue

        pause(f"[{card_type}] 스크랩 완료 — 엔터를 누르면 LLM 정규화를 시작합니다")

        # STEP 3: 정규화
        test_normalize(card_type, details, normalizer)

        if idx < len(targets):
            pause(f"[{card_type}] 완료 — 엔터를 누르면 다음 [{targets[idx]}]으로 넘어갑니다")

    print_summary()

    # ── --save: 파일 저장 ─────────────────────────────────
    if tee:
        tee.restore()
        run_end  = datetime.now()
        elapsed  = (run_end - run_start).total_seconds()
        content  = tee.getvalue()

        timestamp = run_start.strftime("%Y%m%d_%H%M%S")
        filename  = f"test_card_result_{timestamp}.txt"

        header = (
            f"{'='*55}\n"
            f"  카드 테스트 결과 저장 파일\n"
            f"  실행 시작 : {run_start.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  실행 종료 : {run_end.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  소요 시간 : {elapsed:.1f}초\n"
            f"  대상      : {targets}\n"
            f"{'='*55}\n\n"
        )
        with open(filename, "w", encoding="utf-8") as f:
            f.write(header + content)

        print(f"\n✅ 결과 저장 완료 → {filename}  ({elapsed:.1f}초 소요)")


if __name__ == "__main__":
    main()