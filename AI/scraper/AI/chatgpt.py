"""
ChatGPT 요금제 스크래퍼
URL: https://chatgpt.com/ko-KR/pricing/

플랜 구성
  개인: Free / Go / Plus / Pro  — 탭 없이 단일 행
  팀/기업: Business(연간·월간 토글) / Enterprise(가격 문의)

수집 대상 (SERVICE_CATALOG 기준)
  CHATGPT: Go / Plus / Pro / Business(yearly) / Business(monthly)
  Enterprise는 가격이 없으므로 수집 제외

DOM 특징
  - 플랜 카드: <div id="free|go|plus|pro|business|enterprise">
  - 가격: span.tabular-nums  →  ₩N,NNN 형식
  - Business 월간 가격: 페이지 최초 로드 시 연간 기준으로 렌더링.
    Playwright로 '월간 결제' 라디오 버튼 클릭 후 재파싱.
  - 기능 목록: li.text-mkt-p2 > p
  - 주의사항: FAQ 아코디언 (aria-controls="content_*")
  - 사업자 정보: footer 내 텍스트 (해외 법인, 국내 등록 정보 없음)

※ sync_playwright는 greenlet 기반 → 메서드마다 독립 인스턴스
"""

import re
from playwright.sync_api import sync_playwright, Page
from bs4 import BeautifulSoup

from base import AIScraper

PRICING_URL = "https://chatgpt.com/ko-KR/pricing/"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# 수집 대상 카드 id → (plan_name, tab, billing_cycle)
# Enterprise는 가격 문의 방식이므로 제외
PLAN_IDS: dict[str, tuple[str, str | None, str]] = {
    "go":       ("Go",       "개인",  "monthly"),
    "plus":     ("Plus",     "개인",  "monthly"),
    "pro":      ("Pro",      "개인",  "monthly"),
    "business": ("Business", "팀",    "yearly"),   # 연간 기본 렌더링
}


class ChatGPTScraper(AIScraper):

    # ════════════════════════════════════════════════════════════════════
    # 공통 인터페이스
    # ════════════════════════════════════════════════════════════════════

    def scrap_plans(self) -> list[dict]:
        """
        Returns:
            [
                {
                    "platform":       "chatgpt",
                    "tab":            "개인" | "팀" | None,
                    "services":       ["ChatGPT"],
                    "plan_name":      str,          # Go / Plus / Pro / Business
                    "billing_cycle":  "monthly" | "yearly",
                    "price":          int,          # 월 환산 원화
                    "original_price": None,
                    "description":    list[str],
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(3_000)
                return self._parse_plans(page)
            finally:
                browser.close()

    def scrap_cautions(self) -> dict[str, list[str]]:
        """
        FAQ 아코디언에서 주의사항 수집.
        모든 아코디언 항목을 클릭해 펼친 뒤 Q/A 쌍으로 반환.

        Returns:
            {"자주 묻는 질문": ["Q: ...\nA: ...", ...]}
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(3_000)
                return self._parse_cautions(page)
            finally:
                browser.close()

    def scrap_company_info(self) -> list[str]:
        """
        OpenAI는 해외 법인이므로 국내 사업자 등록 정보 없음.
        footer에서 운영사 관련 텍스트만 수집.

        Returns:
            ["OpenAI, L.L.C.", ...]  또는 []
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(3_000)
                return self._parse_company_info(page)
            finally:
                browser.close()

    def scrap(self) -> dict:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(3_000)
                return {
                    "plans":        self._parse_plans(page),
                    "cautions":     self._parse_cautions(page),
                    "company_info": self._parse_company_info(page),
                }
            finally:
                browser.close()

    # ════════════════════════════════════════════════════════════════════
    # Private — DOM 파싱
    # ════════════════════════════════════════════════════════════════════

    def _parse_plans(self, page: Page) -> list[dict]:
        """
        개인 플랜(Go/Plus/Pro) + Business(연간/월간) 수집.

        Business 카드는 최초 로드 시 '연간 결제' 가격만 렌더링되므로
        '월간 결제' 라디오 버튼을 클릭한 뒤 가격을 추가로 파싱한다.
        """
        plans: list[dict] = []

        # ── 1. 개인 플랜 (Go / Plus / Pro) ───────────────────────────
        for plan_id in ("go", "plus", "pro"):
            plan_name, tab, billing_cycle = PLAN_IDS[plan_id]
            card_html = page.inner_html(f"#{plan_id}")
            plan = self._parse_card(
                card_html=card_html,
                plan_name=plan_name,
                tab=tab,
                billing_cycle=billing_cycle,
            )
            if plan:
                plans.append(plan)

        # ── 2. Business 연간 (기본 렌더링) ───────────────────────────
        biz_html_yearly = page.inner_html("#business")
        biz_yearly = self._parse_card(
            card_html=biz_html_yearly,
            plan_name="Business",
            tab="팀",
            billing_cycle="yearly",
        )
        if biz_yearly:
            plans.append(biz_yearly)

        # ── 3. Business 월간 (라디오 버튼 클릭 후 재파싱) ─────────────
        try:
            # '월간 결제' 라디오 버튼: Business 카드 내부 role=radio 중 두 번째
            monthly_btn = page.locator(
                "#business [role='group'][aria-label='가격 옵션 토글'] button[role='radio']"
            ).nth(1)
            monthly_btn.click()
            page.wait_for_timeout(1_000)

            biz_html_monthly = page.inner_html("#business")
            biz_monthly = self._parse_card(
                card_html=biz_html_monthly,
                plan_name="Business",
                tab="팀",
                billing_cycle="monthly",
            )
            if biz_monthly:
                plans.append(biz_monthly)
        except Exception as e:
            print(f"[WARN] Business 월간 가격 파싱 실패: {e}")

        return plans

    def _parse_card(
        self,
        card_html: str,
        plan_name: str,
        tab: str | None,
        billing_cycle: str,
    ) -> dict | None:
        """
        카드 HTML에서 가격과 기능 목록을 추출해 표준 dict 반환.
        가격 파싱 실패 시 None 반환.
        """
        soup = BeautifulSoup(card_html, "html.parser")

        # 가격: span.tabular-nums  →  "₩29,000"
        price_span = soup.find(
            "span", class_=lambda c: c and "tabular-nums" in c
        )
        if not price_span:
            return None

        price = self._parse_price(price_span.get_text(strip=True))
        if price is None:
            return None

        # 기능 목록: li.text-mkt-p2 > p
        description: list[str] = []
        for li in soup.find_all("li", class_=lambda c: c and "text-mkt-p2" in c):
            p_tag = li.find("p")
            if p_tag:
                text = p_tag.get_text(strip=True)
                if text:
                    description.append(text)

        return {
            "platform":       "chatgpt",
            "tab":            tab,
            "services":       ["ChatGPT"],
            "plan_name":      plan_name,
            "billing_cycle":  billing_cycle,
            "price":          price,
            "original_price": None,
            "description":    description,
        }

    def _parse_price(self, text: str) -> int | None:
        """
        "₩29,000" 또는 "₩0" 형식을 int로 변환.
        숫자가 없으면 None 반환.
        """
        digits = re.sub(r"[^\d]", "", text)
        return int(digits) if digits else None

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """
        FAQ 아코디언 전체 항목 수집.
        기본적으로 첫 번째 항목만 펼쳐져 있으므로 나머지를 순차 클릭한다.

        Returns:
            {"자주 묻는 질문": ["Q: ...\nA: ...", ...]}
        """
        faq_items: list[str] = []

        # 아코디언 트리거 버튼 전체 클릭해서 펼치기
        triggers = page.locator("button[aria-controls^='content_']")
        count = triggers.count()
        for i in range(count):
            try:
                btn = triggers.nth(i)
                if btn.get_attribute("aria-expanded") == "false":
                    btn.click()
                    page.wait_for_timeout(400)
            except Exception:
                continue

        # 펼쳐진 아코디언 내용 수집
        page_html = page.content()
        soup = BeautifulSoup(page_html, "html.parser")

        for btn in soup.find_all("button", attrs={"aria-controls": re.compile(r"^content_")}):
            question = btn.get_text(strip=True)
            content_id = btn.get("aria-controls")
            content_div = soup.find("div", id=content_id)
            if not content_div:
                continue
            answer = content_div.get_text(separator=" ", strip=True)
            if question and answer:
                faq_items.append(f"Q: {question}\nA: {answer}")

        return {"자주 묻는 질문": faq_items} if faq_items else {}

    def _parse_company_info(self, page: Page) -> list[str]:
        """
        footer에서 운영사 정보 수집.
        OpenAI는 국내 사업자 등록 정보가 없으므로 footer 내
        저작권/회사명 텍스트만 반환한다.
        """
        info: list[str] = []
        try:
            footer = page.query_selector("footer")
            if not footer:
                return info
            text = footer.inner_text().strip()
            for line in text.splitlines():
                line = line.strip()
                if line:
                    info.append(line)
        except Exception as e:
            print(f"[WARN] company_info 파싱 실패: {e}")
        return info


# ════════════════════════════════════════════════════════════════════════
# 단독 실행 테스트
# ════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    scraper = ChatGPTScraper()

    print("=== [scrap_plans] ===")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 플랜 수집")
    for pl in plans:
        print(
            f"  [{pl['tab']}] {pl['plan_name']} / {pl['billing_cycle']}"
            f" / ₩{pl['price']:,} / {pl['description'][:1]}"
        )

    print("\n=== [scrap_cautions] ===")
    cautions = scraper.scrap_cautions()
    for section, items in cautions.items():
        print(f"  [{section}] {len(items)}건")
        for item in items[:2]:
            print(f"    {item[:80]}...")

    print("\n=== [scrap_company_info] ===")
    for line in scraper.scrap_company_info():
        print(f"  {line}")