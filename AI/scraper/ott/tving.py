import json
import threading
from playwright.sync_api import sync_playwright, Page

from base import OTTScraper


"""
티빙 스크래퍼 (TvingScraper)

OTTScraper 공통 인터페이스 5개 + Tving 전용 FAQ 메서드 2개로 구성.

공통 메서드 (스케줄러 직접 호출):
    scrap_logo()         : PageHeader <svg> 태그 수집           (월 1회)
    scrap_plans()        : 구독 요금제 목록 수집                (일 1회)
    scrap_cautions()     : 구독 주의사항 수집                   (주 1회)
    scrap_company_info() : 사업자 정보 수집                     (월 1회)

Tving 전용 메서드 (스케줄러 직접 호출 안 함):
    scrap_faq_payment()  : FAQ 이용권/결제 탭 수집              (주 1회)
    scrap_faq_refund()   : FAQ 해지/환불 탭 수집                (주 1회)

요금제 데이터  : Next.js RSC 페이로드(window.__next_f)에서 JSON 직접 추출
               → UI 변경에 강건하게 동작
주의사항·사업자: DOM 파싱으로 수집
공지사항       : 공지 태그 항목만 필터링 후 상세 페이지 본문까지 수집
FAQ            : URL 직접 접근 후 페이지네이션 전체 순회

※ sync_playwright는 greenlet 기반이라 컨텍스트 공유 불가 → 메서드마다 독립 인스턴스
"""


PLAN_URL         = "https://www.tving.com/bill/subscription/plan"
NOTICE_URL       = "https://www.tving.com/help/notice"
FAQ_PAYMENT_URL  = "https://www.tving.com/help/faq?page=1&categoryCode=FAQN0002"
FAQ_REFUND_URL   = "https://www.tving.com/help/faq?page=1&categoryCode=FAQN0004"
BASE_URL         = "https://www.tving.com"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


class TvingScraper(OTTScraper):

    # RSC payload의 category type → 탭 표시명
    CATEGORY_NAMES = {
        "d2c":    "티빙",
        "wavve":  "티빙X웨이브",
        "disney": "티빙X디즈니+",
        "plus":   "티빙플러스",
    }

    # 중복 데이터이므로 skip
    SKIP_CATEGORIES = {"recommended"}

    # ═══════════════════════════════════════════════════════════════════════
    # 공통 인터페이스 (OTTScraper 공통 5개)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap_logo(self) -> str:
        """
        PageHeader 영역의 <svg> 태그 전체 수집

        Returns:
            SVG 태그 전체 문자열, 없으면 빈 문자열
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)
                return self._parse_logo(page)
            finally:
                browser.close()

    def scrap_plans(self) -> list[dict]:
        """
        구독 요금제 전체 목록 수집

        Returns:
            [
                {
                    "platform":       str,        # "tving"
                    "tab":            str | None,  # 카테고리 탭 표시명
                    "services":       list[str],   # 서비스명 리스트
                    "plan_name":      str,
                    "billing_cycle":  str,         # "monthly" | "yearly"
                    "price":          int,          # 실제 결제 금액 (원)
                    "original_price": int | None,  # 할인 전 정가
                    "description":    list[str],   # 스펙 요약 문자열 리스트
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)

                rsc_content = page.evaluate("""() => {
                    return (window.__next_f || [])
                        .filter(c => c[0] === 1)
                        .map(c => c[1])
                        .join('');
                }""")

                return self._parse_rsc(rsc_content)
            finally:
                browser.close()

    def scrap_cautions(self) -> dict[str, list[str]]:
        """
        구독 주의사항 수집

        Returns:
            { "섹션 제목": ["항목1", "항목2", ...], ... }
            h2 태그가 없으면 "기타" 키로 수집
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)
                return self._parse_cautions(page)
            finally:
                browser.close()

    def scrap_company_info(self) -> list[str]:
        """
        사업자 정보 수집

        Returns:
            ["사업자 정보 줄1", "줄2", ...]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)
                return self._parse_company_info(page)
            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # Tving 전용 메서드 (공통 인터페이스 범위 외)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap_faq_payment(self) -> list[dict]:
        """
        FAQ 이용권/결제 탭 전체 수집

        Returns:
            [{"question": str, "answer": str}, ...]
        """
        return self._scrap_faq(FAQ_PAYMENT_URL)

    def scrap_faq_refund(self) -> list[dict]:
        """
        FAQ 해지/환불 탭 전체 수집

        Returns:
            [{"question": str, "answer": str}, ...]
        """
        return self._scrap_faq(FAQ_REFUND_URL)

    # ═══════════════════════════════════════════════════════════════════════
    # 통합 수집 (스케줄러가 직접 호출하지 않음)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap(self) -> dict:
        """
        공통 5개 + Tving 전용 FAQ 2개를 일괄 수집
        scrap_plans / scrap_faq_* 는 병렬 실행

        Returns:
            {
                "logo":         str,
                "plans":        list[dict],
                "cautions":     dict[str, list[str]],
                "company_info": list[str],
                "faq_payment":  list[dict],
                "faq_refund":   list[dict],
            }
        """
        # logo / cautions / company_info 는 같은 페이지(PLAN_URL)에서 수집
        # → 브라우저 1개로 묶어서 처리
        plan_page_result: dict      = {}
        faq_payment_result: list[dict] = []
        faq_refund_result:  list[dict] = []
        errors: list[str] = []

        def fetch_plan_page():
            try:
                plan_page_result.update(self._scrap_plan_page())
            except Exception as e:
                errors.append(f"[plan_page] {e}")

        def fetch_faq_payment():
            try:
                faq_payment_result.extend(self.scrap_faq_payment())
            except Exception as e:
                errors.append(f"[faq_payment] {e}")

        def fetch_faq_refund():
            try:
                faq_refund_result.extend(self.scrap_faq_refund())
            except Exception as e:
                errors.append(f"[faq_refund] {e}")

        threads = [
            threading.Thread(target=fetch_plan_page),
            threading.Thread(target=fetch_faq_payment),
            threading.Thread(target=fetch_faq_refund),
        ]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        if errors:
            for err in errors:
                print(f"[TvingScraper] 수집 오류: {err}")

        return {
            "logo":         plan_page_result.get("logo", ""),
            "plans":        plan_page_result.get("plans", []),
            "cautions":     plan_page_result.get("cautions", {}),
            "company_info": plan_page_result.get("company_info", []),
            "faq_payment":  faq_payment_result,
            "faq_refund":   faq_refund_result,
        }

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 브라우저 세션 묶음
    # ═══════════════════════════════════════════════════════════════════════

    def _scrap_plan_page(self) -> dict:
        """
        PLAN_URL 한 번 진입으로 logo / plans / cautions / company_info 일괄 수집
        scrap() 내부의 병렬 스레드에서 호출
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)

                rsc_content = page.evaluate("""() => {
                    return (window.__next_f || [])
                        .filter(c => c[0] === 1)
                        .map(c => c[1])
                        .join('');
                }""")

                return {
                    "logo":         self._parse_logo(page),
                    "plans":        self._parse_rsc(rsc_content),
                    "cautions":     self._parse_cautions(page),
                    "company_info": self._parse_company_info(page),
                }
            finally:
                browser.close()

    def _scrap_faq(self, url: str) -> list[dict]:
        """
        FAQ URL 직접 접근 후 페이지네이션 전체 순회
        scrap_faq_payment / scrap_faq_refund 공통 로직

        Args:
            url: FAQ 카테고리 URL (categoryCode 포함)

        Returns:
            [{"question": str, "answer": str}, ...]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(url, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)

                results:  list[dict] = []
                page_num: int = 1

                while True:
                    results.extend(self._parse_faq_items(page))

                    next_btn = page.locator(
                        "div.btn__pagenum-wrap button.btn__pagenum:not(.click_on)",
                        has_text=str(page_num + 1),
                    ).first

                    if not next_btn.is_visible():
                        break

                    next_btn.click()
                    page.wait_for_timeout(1500)
                    page_num += 1

                return results
            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — DOM / RSC 파싱
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_logo(self, page: Page) -> str:
        """PageHeader 영역의 <svg> 태그 outer HTML 반환"""
        svg = page.query_selector(
            "div[data-sentry-component='PageHeader'] svg"
        )
        if not svg:
            raise NotImplementedError(
                "[tving] scrap_logo() 미구현 — PageHeader SVG를 찾을 수 없음, 선택자 확인 필요"
            )
        return svg.evaluate("el => el.outerHTML") or ""

    def _parse_rsc(self, rsc_content: str) -> list[dict]:
        """RSC 페이로드에서 categoryData JSON 객체를 모두 찾아 파싱"""
        plans = []
        decoder = json.JSONDecoder()
        search_key = '"categoryData":'
        seen_types = set()
        pos = 0

        while True:
            idx = rsc_content.find(search_key, pos)
            if idx == -1:
                break

            json_start = idx + len(search_key)
            try:
                category_data, _ = decoder.raw_decode(rsc_content, json_start)
                cat_type = category_data.get("type", "")

                if cat_type in self.SKIP_CATEGORIES or cat_type in seen_types:
                    pos = idx + 1
                    continue

                seen_types.add(cat_type)

                for product in category_data.get("products") or []:
                    plans.extend(self._parse_product(product, cat_type))

            except (json.JSONDecodeError, ValueError):
                pass

            pos = idx + 1

        return plans

    def _parse_product(self, product: dict, category: str) -> list[dict]:
        """단일 상품의 기간별 요금제 리스트 반환"""
        results = []
        product_name = product.get("productName", "")
        detail_list  = product.get("productDetailList") or []
        period_list  = product.get("periodProductList") or []

        services    = self._extract_services(product, detail_list)
        description = self._build_description(detail_list)

        for period in period_list:
            price = period.get("recurPrice")
            if price is None:
                continue

            billing_cycle  = "yearly" if period.get("type") == "YEAR" else "monthly"
            raw_price      = period.get("price")
            original_price = raw_price if (raw_price and raw_price != price) else None

            results.append({
                "platform":       "tving",
                "tab":            self.CATEGORY_NAMES.get(category, category),
                "services":       services,
                "plan_name":      product_name,
                "billing_cycle":  billing_cycle,
                "price":          price,
                "original_price": original_price,
                "description":    description,
            })

        return results

    def _extract_services(self, product: dict, detail_list: list) -> list[str]:
        """productDetailList의 title에서 서비스명 추출"""
        if not detail_list:
            product_type = product.get("productType", "")
            return ["Apple TV+"] if product_type == "PLUS" else ["티빙"]

        services = []
        for detail in detail_list:
            service = (detail.get("title") or "").split()[0]
            if service and service not in services:
                services.append(service)

        return services

    def _build_description(self, detail_list: list) -> list[str]:
        """각 서비스의 spec을 요약 문자열 리스트로 변환"""
        description = []
        for detail in detail_list:
            parts = []
            if detail.get("watchMaxCount"):
                parts.append(f"동시시청 {detail['watchMaxCount']}")
            if detail.get("quality"):
                parts.append(detail["quality"])
            if detail.get("downloadCount"):
                parts.append(f"다운로드 {detail['downloadCount']}")
            if detail.get("supportedDevices"):
                parts.append(detail["supportedDevices"])
            if detail.get("advertisement"):
                parts.append("광고 포함")
            if detail.get("appleTvPlus"):
                parts.append("Apple TV+ 제공")
            if parts:
                description.append(" | ".join(parts))

        return description

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """section#subscription-plan-caution 에서 주의사항 수집"""
        cautions: dict[str, list[str]] = {}

        section = page.query_selector("section#subscription-plan-caution")
        if not section:
            return cautions

        current_heading = "기타"
        for el in section.query_selector_all("h2, p"):
            tag:  str = el.evaluate("e => e.tagName")
            text: str = (el.inner_text() or "").strip()
            if not text:
                continue

            if tag == "H2":
                current_heading = text
                cautions.setdefault(current_heading, [])
            else:
                cautions.setdefault(current_heading, []).append(text)

        return cautions

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer 내 FooterInfoLine p 태그에서 사업자 정보 수집"""
        info: list[str] = []

        for p_el in page.query_selector_all(
            "p[data-sentry-component='FooterInfoLine']"
        ):
            text = (p_el.inner_text() or "").strip()
            if text:
                info.append(text)

        return info

    def _parse_faq_items(self, page: Page) -> list[dict]:
        """
        현재 페이지의 FAQ 테이블에서 질문/답변 쌍 수집

        DOM 구조:
            <tr class="accordion-title ...">  ← 질문 행 (h5 태그)
            <tr class="accordion-menu text">  ← 답변 행 (p 태그)
        """
        items: list[dict] = []
        rows = page.query_selector_all("table.table-faq-column tbody tr")

        i = 0
        while i < len(rows):
            row        = rows[i]
            class_attr = row.get_attribute("class") or ""

            if "accordion-title" in class_attr:
                h5       = row.query_selector("h5")
                question = (h5.inner_text() if h5 else "").strip()

                answer = ""
                if i + 1 < len(rows):
                    next_row   = rows[i + 1]
                    next_class = next_row.get_attribute("class") or ""
                    if "accordion-menu" in next_class:
                        answer = (next_row.inner_text() or "").strip()
                        i += 1  # 답변 행 소비

                if question:
                    items.append({"question": question, "answer": answer})

            i += 1

        return items


if __name__ == "__main__":
    scraper = TvingScraper()

    print("=== 개별 메서드 테스트 ===\n")

    print("[scrap_logo]")
    logo = scraper.scrap_logo()
    print(logo[:80], "...\n" if logo else "(없음)\n")

    print("[scrap_plans]")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 요금제 수집")
    for p in plans[:2]:
        print(" ", p)
    print()

    print("[scrap_cautions]")
    cautions = scraper.scrap_cautions()
    for heading, items in cautions.items():
        print(f"  [{heading}] {len(items)}건")
    print()

    print("[scrap_company_info]")
    for line in scraper.scrap_company_info():
        print(" ", line)
    print()



    print("[scrap_faq_payment]")
    faq_pay = scraper.scrap_faq_payment()
    print(f"총 {len(faq_pay)}건")
    print()

    print("[scrap_faq_refund]")
    faq_ref = scraper.scrap_faq_refund()
    print(f"총 {len(faq_ref)}건")