# ============================================================
# 프로모션 스크래퍼 → LLM 정규화 전체 파이프라인 테스트
# 실행 위치: scraper/ 디렉토리
#
#   python test_promotion.py                    # 전체 (대화형)
#   python test_promotion.py tving              # 특정 스크래퍼만
#   python test_promotion.py tving wavve        # 복수 지정
#   python test_promotion.py --save             # 비대화형 + 결과 txt 저장
#   python test_promotion.py tving --save
#
# 흐름:
#   [스크래퍼 N] RAW 수집 출력
#     → (대화형) 엔터
#     → LLM 정규화 결과 출력
#     → [스크래퍼 N+1] ...
#     → (--save) test_promotion_result_YYYYMMDD_HHMMSS.txt 저장
#
# 스크래퍼별 normalizer 매핑:
#   tving          → normalize_tving_promotion()   (LLM, 필터링)
#   wavve          → normalize_wavve_promotion()   (Python 직접 조립)
#   disneyplus     → normalize_promotions()        (기존 LLM)
#   watcha         → normalize_promotions()        (기존 LLM)
#   naver          → normalize_naver_membership()  (쿠폰 summary 포함)
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
logger = logging.getLogger("test_promotion")

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

results = []  # (name, step, status, detail)

def record(name, step, status, detail=""):
    results.append((name, step, status, detail))

def pause(msg="다음 단계로 진행하려면 엔터를 누르세요..."):
    if SAVE_MODE:
        print(f"\n  [ {msg} — 자동 진행 ]")
        return
    print(f"\n  {YELLOW}[ {msg} ]{RESET}")
    input()


# ── STEP 1: RAW 수집 ──────────────────────────────────────
def test_raw(name: str, scraper) -> list[dict]:
    print(f"\n{'═'*55}")
    print(f"  🌐 [{name.upper()}] STEP 1 — RAW 수집")
    print(f"{'═'*55}")

    try:
        notices = scraper.scrap()
        ok(f"scrap() — {len(notices)}건")
        record(name, "scrap", "OK", f"{len(notices)}건")

        if not notices:
            warn("수집 결과 0건 (프로모션 없음 또는 수집 실패)")
            return []

        # 필수 필드 확인 (naver_membership은 card_detail 형식이라 별도 처리)
        is_naver = (name == "naver")
        if is_naver:
            required = ["card_name", "brand", "meta_info", "benefits", "source_url"]
        else:
            required = ["platform", "title", "date", "content"]

        missing_fields = set()
        for item in notices:
            for f in required:
                if f not in item:
                    missing_fields.add(f)

        if missing_fields:
            warn(f"필드 누락: {missing_fields}")
            record(name, "fields", "WARN", f"누락: {missing_fields}")
        else:
            ok(f"필수 필드 모두 존재")
            record(name, "fields", "OK")

        # 수집 결과 출력
        print(f"\n  [RAW 수집 결과 — {len(notices)}건]")
        if is_naver:
            # naver는 benefits가 길어서 요약 출력
            for i, item in enumerate(notices, 1):
                print(f"\n  [{i}] {item.get('brand')} {item.get('card_name')}")
                print(f"    meta_info : {item.get('meta_info', '(없음)')}")
                print(f"    img_url   : {item.get('img_url', '(없음)')}")
                for b in item.get("benefits", []):
                    desc = b.get("description", "")
                    print(f"    [{b.get('category')}] {desc[:100]}{'...' if len(desc) > 100 else ''}")
        else:
            for i, item in enumerate(notices, 1):
                print(f"\n  [{i}] [{item.get('date','')}] {item.get('title','')}")
                content = item.get("content", "")
                if content:
                    print(f"    content: {content[:120]}{'...' if len(content) > 120 else ''}")

        print(f"\n  [전체 JSON 출력 — 첫 번째 건]")
        first = notices[0]
        if is_naver:
            # benefits description 앞부분만 출력
            display = {**first, "benefits": [
                {**b, "description": b.get("description","")[:150]}
                for b in first.get("benefits", [])
            ]}
            print(json.dumps(display, ensure_ascii=False, indent=2))
        else:
            display = {**first, "content": first.get("content","")[:200] + ("..." if len(first.get("content","")) > 200 else "")}
            print(json.dumps(display, ensure_ascii=False, indent=2))

        return notices

    except Exception as e:
        fail(f"scrap() 오류: {e}")
        record(name, "scrap", "FAIL", str(e))
        traceback.print_exc()
        return []


# ── STEP 2: LLM 정규화 ───────────────────────────────────
def test_normalize(name: str, notices: list[dict], normalizer) -> None:
    print(f"\n{'═'*55}")
    print(f"  🤖 [{name.upper()}] STEP 2 — LLM 정규화")
    print(f"{'═'*55}")

    if not notices:
        warn("RAW 데이터 없음 — 정규화 스킵")
        record(name, "normalize", "SKIP", "raw 없음")
        return

    # 스크래퍼별 normalizer 매핑
    def _normalize(notice: dict) -> dict:
        if name == "tving":
            return normalizer.normalize_tving_promotion(notice)
        elif name == "wavve":
            return normalizer.normalize_wavve_promotion(notice)
        elif name == "naver":
            return normalizer.normalize_naver_membership(notice)
        else:
            return normalizer.normalize_promotions(notice)

    VALID_CODES = {"NETFLIX", "DISNEY_PLUS", "TVING", "WAVVE", "WATCHA", "COUPANG_PLAY"}
    saved = 0
    skipped = 0

    for i, notice in enumerate(notices, 1):
        title = notice.get("title") or notice.get("card_name", f"#{i}")
        print(f"\n  {'─'*50}")
        print(f"  [{i}/{len(notices)}] {title}")
        print(f"  {'─'*50}")

        try:
            result = _normalize(notice)

            # jobType 확인
            assert result.get("jobType") == "PROMOTION_CRAWL", \
                f"jobType 불일치: {result.get('jobType')}"
            assert result.get("collectedAt"), "collectedAt 없음"

            promotions = result.get("promotions", [])

            # tving은 빈 리스트가 정상 (필터링 결과)
            if not promotions:
                if name == "tving":
                    info("promotions: [] — 프로모션 없음으로 판단 (정상)")
                    record(name, f"normalize_{i}", "OK", "필터링됨 (promotions 없음)")
                    skipped += 1
                else:
                    warn("promotions 비어 있음")
                    record(name, f"normalize_{i}", "WARN", "promotions 없음")
                    skipped += 1
                print(json.dumps(result, ensure_ascii=False, indent=2))
                continue

            # 각 promotion 검증
            for j, promo in enumerate(promotions, 1):
                p_type    = promo.get("promotionType")
                svc_codes = [s.get("serviceCode") for s in promo.get("services", [])]

                assert p_type in ("CARD_BENEFIT", "BUNDLE", "PROMO"), \
                    f"promotionType 불일치: {p_type}"
                assert promo.get("startsAt"), "startsAt 없음"
                assert promo.get("endsAt"),   "endsAt 없음"
                assert promo.get("title"),    "title 없음"
                assert svc_codes,             "services 비어 있음"

                invalid = [c for c in svc_codes if c not in VALID_CODES]
                if invalid:
                    fail(f"잘못된 serviceCode: {invalid}")
                    record(name, f"services_{i}_{j}", "FAIL", str(invalid))
                else:
                    ok(f"[{p_type}] {promo.get('title','')[:50]}")
                    ok(f"services  : {svc_codes}")

                summary = promo.get("summary")
                if summary:
                    ok(f"summary   : {summary[:80]}{'...' if len(summary) > 80 else ''}")
                else:
                    info("summary   : null")

                if name == "naver" and p_type == "CARD_BENEFIT":
                    assert promo.get("endsAt") == "2099-12-31T23:59:59+09:00", \
                        "naver endsAt 고정값 불일치"
                    ok("endsAt 고정값 정상 (2099-12-31)")

            ok(f"구조 검증 통과 — promotions {len(promotions)}건")
            record(name, f"normalize_{i}", "OK", f"{len(promotions)}건")
            saved += 1

            print(f"\n  [전체 정규화 출력]")
            print(json.dumps(result, ensure_ascii=False, indent=2))

        except AssertionError as e:
            fail(f"구조 검증 실패: {e}")
            record(name, f"normalize_{i}", "FAIL", str(e))
        except Exception as e:
            fail(f"normalize 오류: {e}")
            record(name, f"normalize_{i}", "FAIL", str(e))
            traceback.print_exc()

    print(f"\n  {'─'*50}")
    info(f"정규화 완료 — 저장 대상: {saved}건 / 스킵: {skipped}건 / 전체: {len(notices)}건")


# ── 최종 요약 ─────────────────────────────────────────────
def print_summary():
    print(f"\n{'='*55}")
    print("📋 전체 테스트 결과 요약")
    print(f"{'='*55}")

    platforms = {}
    for name, step, status, detail in results:
        platforms.setdefault(name, []).append((step, status, detail))

    for name, steps in platforms.items():
        ok_cnt   = sum(1 for _, s, _ in steps if s == "OK")
        fail_cnt = sum(1 for _, s, _ in steps if s == "FAIL")
        skip_cnt = sum(1 for _, s, _ in steps if s == "SKIP")
        warn_cnt = sum(1 for _, s, _ in steps if s == "WARN")

        icon = GREEN+"✅"+RESET if fail_cnt == 0 else RED+"❌"+RESET
        print(f"\n  {icon} {name.upper()}")
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
    global SAVE_MODE

    available = {
        "tving":      ("promotion.tving",            "TvingPromotionScraper"),
        "wavve":      ("promotion.wavve",             "WavvePromotionScraper"),
        "disneyplus": ("promotion.disneyplus",        "DisneyPlusPromotionScraper"),
        "watcha":     ("promotion.watcha",            "WatchaPromotionScraper"),
        "naver":      ("promotion.naver_membership",  "NaverMembershipScraper"),
    }

    raw_args  = sys.argv[1:]
    SAVE_MODE = "--save" in [a.lower() for a in raw_args]
    args      = [a.lower() for a in raw_args if a.lower() != "--save"]
    targets   = {k: v for k, v in available.items() if k in args} if args else available

    tee = None
    if SAVE_MODE:
        tee = TeeOutput()
        sys.stdout = tee

    run_start = datetime.now()

    print(f"\n{'='*55}")
    mode_label = "비대화형·파일저장" if SAVE_MODE else "단계별 확인"
    print(f"🚀 프로모션 파이프라인 테스트 ({mode_label})")
    print(f"   대상: {list(targets.keys())}")
    print(f"   시작: {run_start.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'='*55}")

    # ── 스크래퍼 로드 ─────────────────────────────────────
    scrapers = {}
    print(f"\n{'─'*55}")
    print("  모듈 로드 중...")
    print(f"{'─'*55}")
    for name, (module_path, class_name) in targets.items():
        try:
            import importlib
            mod = importlib.import_module(module_path)
            scrapers[name] = getattr(mod, class_name)()
            ok(f"{class_name} 로드 완료")
        except Exception as e:
            fail(f"{class_name} 로드 실패: {e}")
            record(name, "import", "FAIL", str(e))

    # ── LLM 로드 ──────────────────────────────────────────
    print(f"\n{'─'*55}")
    print("  LLMNormalizer 로드 중...")
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

    # ── 스크래퍼별 순차 실행 ──────────────────────────────
    scraper_list = [(n, s) for n, s in scrapers.items()]
    for idx, (name, scraper) in enumerate(scraper_list, 1):
        print(f"\n\n{'#'*55}")
        print(f"  [{idx}/{len(scraper_list)}] {name.upper()}")
        print(f"{'#'*55}")

        # STEP 1: RAW 수집
        notices = test_raw(name, scraper)

        pause(f"[{name}] RAW 수집 완료 — 엔터를 누르면 LLM 정규화를 시작합니다")

        # STEP 2: LLM 정규화
        test_normalize(name, notices, normalizer)

        if idx < len(scraper_list):
            pause(f"[{name}] 완료 — 엔터를 누르면 다음 [{scraper_list[idx][0]}]으로 넘어갑니다")

    print_summary()

    # ── --save: 파일 저장 ─────────────────────────────────
    if tee:
        tee.restore()
        run_end  = datetime.now()
        elapsed  = (run_end - run_start).total_seconds()
        content  = tee.getvalue()

        timestamp = run_start.strftime("%Y%m%d_%H%M%S")
        filename  = f"test_promotion_result_{timestamp}.txt"

        header = (
            f"{'='*55}\n"
            f"  프로모션 테스트 결과 저장 파일\n"
            f"  실행 시작 : {run_start.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  실행 종료 : {run_end.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"  소요 시간 : {elapsed:.1f}초\n"
            f"  대상      : {list(targets.keys())}\n"
            f"{'='*55}\n\n"
        )
        with open(filename, "w", encoding="utf-8") as f:
            f.write(header + content)

        print(f"\n✅ 결과 저장 완료 → {filename}  ({elapsed:.1f}초 소요)")


if __name__ == "__main__":
    main()