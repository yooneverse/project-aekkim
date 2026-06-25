"""
넷플릭스 공식 홈페이지에서 요금제 정보를 수집하는 크롤러
URL: https://www.netflix.com/kr/signup/planform

요금제 선택 페이지는 초기 랜딩 페이지에서 "다음" 버튼 클릭 후 진입한다.
넷플릭스는 탭 구분 없이 단일 페이지에서 모든 요금제를 제공하며,
결합 이용권이 없으므로 services 는 항상 ["넷플릭스"].

※ sync_playwright는 greenlet 기반이라 컨텍스트 공유 불가 → 메서드마다 독립 인스턴스
"""
from playwright.sync_api import sync_playwright, Page

from base import OTTScraper


PLAN_URL = "https://www.netflix.com/kr/signup/planform"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


class NetflixScraper(OTTScraper):

    # ═══════════════════════════════════════════════════════════════════════
    # 공통 인터페이스 (OTTScraper 공통 5개)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap_logo(self) -> str:
        """
        헤더 <a> 태그 내 <svg> 태그 전체 수집

        Returns:
            SVG 태그 전체 문자열
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(3000)
                return self._parse_logo(page)
            finally:
                browser.close()

    def scrap_plans(self) -> list[dict]:
        """
        구독 요금제 전체 목록 수집

        Returns:
            [
                {
                    "platform":       str,        # "netflix"
                    "tab":            None,        # 넷플릭스는 탭 구분 없음
                    "services":       list[str],   # ["넷플릭스"]
                    "plan_name":      str,
                    "billing_cycle":  str,         # "monthly"
                    "price":          int,
                    "original_price": None,        # 넷플릭스는 정가 판매
                    "description":    list[str],
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(3000)

                next_btn = page.locator('[data-uia="cta-button"]')
                if next_btn.is_visible():
                    next_btn.click()
                    page.wait_for_timeout(2000)

                return self._parse_plans(page)
            finally:
                browser.close()

    def scrap_cautions(self) -> dict[str, list]:
        """
        구독 주의사항 수집

        Returns:
            { "섹션 제목": ["항목1", "항목2", ...], ... }
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(3000)

                next_btn = page.locator('[data-uia="cta-button"]')
                if next_btn.is_visible():
                    next_btn.click()
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
                page.wait_for_timeout(3000)

                next_btn = page.locator('[data-uia="cta-button"]')
                if next_btn.is_visible():
                    next_btn.click()
                    page.wait_for_timeout(2000)

                return self._parse_company_info(page)
            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # 통합 수집 (스케줄러가 직접 호출하지 않음)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap(self) -> dict:
        """
        Returns:
            {
                "logo":         str,
                "plans":        list[dict],
                "cautions":     dict[str, list[str]],
                "company_info": list[str],
            }
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()

            try:
                page.goto(PLAN_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(3000)

                # "다음" 버튼 클릭 후 요금제 페이지로 이동
                next_btn = page.locator('[data-uia="cta-button"]')
                if next_btn.is_visible():
                    next_btn.click()
                    page.wait_for_timeout(2000)

                return {
                    "logo":         self._parse_logo(page),
                    "plans":        self._parse_plans(page),
                    "cautions":     self._parse_cautions(page),
                    "company_info": self._parse_company_info(page),
                }

            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — DOM 파싱
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_logo(self, page: Page) -> str:
        """헤더 <a> 태그 내 <svg> 태그 outer HTML 반환

        구조: <a href="/"> <svg viewBox="0 0 111 30" ...> ... </svg> </a>
        """
        svg = page.query_selector('header a[href="/"] svg')
        if not svg:
            raise NotImplementedError(
                "[netflix] scrap_logo() 미구현 — header a[href='/'] svg 선택자로 SVG를 찾을 수 없음, 선택자 확인 필요"
            )
        return svg.evaluate("el => el.outerHTML") or ""

    def _parse_plans(self, page: Page) -> list[dict]:
        """[data-uia="plan-card"] 에서 요금제 정보 파싱"""
        plans = []

        for card in page.query_selector_all('[data-uia="plan-card"]'):
            try:
                name_el = card.query_selector('[data-uia="plan-name"]')
                if not name_el:
                    continue
                plan_name = name_el.inner_text().strip()

                description, price = self._parse_features(card)

                plans.append({
                    "platform": "netflix",
                    "tab": None,           # 넷플릭스는 탭 구분 없음
                    "services": ["넷플릭스"],
                    "plan_name": plan_name,
                    "billing_cycle": "monthly",
                    "price": price,
                    "original_price": None,  # 넷플릭스는 정가 판매
                    "description": description,
                })

            except Exception as e:
                print(f"[ERROR] 카드 파싱 실패: {e}")
                continue

        return plans

    def _parse_features(self, card) -> tuple[list[str], int | None]:
        """ul[role="tabpanel"] li 에서 (description, price) 추출

        각 li 구조:
            div[data-uia^="feature"]
              div  → key  (예: "월 요금")
              div  → value (예: "7,000원")
        """
        description: list[str] = []
        price: int | None = None

        for li in card.query_selector_all('ul[role="tabpanel"] li'):
            feature = li.query_selector('[data-uia^="feature"]')
            if not feature:
                continue

            divs = feature.query_selector_all(':scope > div')
            if len(divs) < 2:
                continue

            key = divs[0].inner_text().strip()
            value = divs[1].inner_text().strip()
            description.append(f"{key}: {value}")

            if key == "월 요금":
                digits = "".join(filter(str.isdigit, value))
                if digits:
                    price = int(digits)

        return description, price

    # ─── 요금제 파싱 ──────────────────────────────────────────────────────

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """[data-uia="restrictions-disclaimers"] 에서 주의사항 수집

        Returns:
            {
                "광고형 멤버십 안내": ["광고형 멤버십으로 대부분의..."],
                "법적 고지": ["광고형 멤버십을 선택하면...", ...],
            }
        """
        cautions: dict[str, list[str]] = {}

        section = page.query_selector('[data-uia="restrictions-disclaimers"]')
        if not section:
            return cautions

        # ① lock 아이콘 옆 광고형 멤버십 안내 텍스트
        icon_lock = section.query_selector('[data-uia="icon-lock"]')
        if icon_lock:
            text = icon_lock.evaluate('''el => {
                const container = el.closest('[data-layout="container"]');
                if (!container) return '';
                const items = container.querySelectorAll('[data-layout="item"]');
                return items.length > 1
                    ? items[items.length - 1].innerText.trim()
                    : '';
            }''')
            if text:
                cautions["광고형 멤버십 안내"] = [text]

        # ② legal-content 내 p 태그 법적 고지
        legal_container = section.query_selector('[data-uia="legal-content+container"]')
        if legal_container:
            legal_texts = [
                p.inner_text().strip()
                for p in legal_container.query_selector_all("p")
                if p.inner_text().strip()
            ]
            if legal_texts:
                cautions["법적 고지"] = legal_texts

        return cautions

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer 내 사업자 정보 텍스트 수집

        Returns:
            ["넷플릭스서비시스코리아 유한회사 통신판매업신고번호: ...", "대표: ...", ...]
        """
        info: list[str] = []

        container = page.query_selector("footer div.ec09bek0")
        if not container:
            return info

        stack = container.query_selector('[data-layout="stack"]')
        if not stack:
            return info

        for item in stack.query_selector_all(':scope > [data-layout="item"]'):
            text = (item.inner_text() or "").strip()
            if text:
                info.append(text)

        return info


if __name__ == "__main__":
    scraper = NetflixScraper()

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