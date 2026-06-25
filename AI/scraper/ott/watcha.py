"""
왓챠 공식 홈페이지에서 요금제 정보를 수집하는 크롤러

① 월간 요금 + description:
   URL: https://watcha.com/ko-KR/payment/choose_grade?redirect_uri=%2Fsettings
   div[role='group'] > label[data-select] 구조 파싱

② 연간 요금:
   URL: https://watcha.com/ko-KR/payment/choose_duration?redirect_uri=%2Fsettings&delimiter={Grande|Tall}
   label[data-select="Ticket::GrandeYearly"] / label[data-select="Ticket::TallYearly"] 파싱
   description은 ①에서 수집한 월간 요금제 것을 재사용

주의사항: data-select="notification-item-container" ul 파싱 (안정 선택자)
사업자 정보:
  - ul.GlobalFooter_contact: 고객센터, 제휴, B2B 문의
  - div.GlobalFooter_extraBlock > ul.GlobalFooter_extraInfo: 사업자 등록 정보


※ sync_playwright는 greenlet 기반이라 컨텍스트 공유 불가 → 메서드마다 독립 인스턴스
"""
import re
from playwright.sync_api import sync_playwright, Page

from base import OTTScraper


GRADE_URL    = "https://watcha.com/ko-KR/payment/choose_grade?redirect_uri=%2Fsettings"
DURATION_URL = "https://watcha.com/ko-KR/payment/choose_duration?redirect_uri=%2Fsettings&delimiter={}"
HOME_URL     = "https://watcha.com/ko-KR"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# (delimiter, plan_name, yearly label의 data-select 값)
YEARLY_PLANS = [
    ("Grande", "프리미엄", "Ticket::GrandeYearly"),
    ("Tall",   "베이직",   "Ticket::TallYearly"),
]



class WatchaScraper(OTTScraper):

    # ═══════════════════════════════════════════════════════════════════════
    # 공통 인터페이스 (OTTScraper 공통 5개)
    # ═══════════════════════════════════════════════════════════════════════

    def scrap_logo(self) -> str:
        """
        헤더 로고 수집

        Returns:
            SVG 태그 전체 문자열 또는 로고 이미지 URL
        """
        raise NotImplementedError(
            "[watcha] scrap_logo() 미구현 — 헤더 로고 선택자 확인 후 구현 필요"
        )

    def scrap_plans(self) -> list[dict]:
        """
        구독 요금제 전체 목록 수집 (월간 + 연간)

        Returns:
            [
                {
                    "platform":       str,        # "watcha"
                    "tab":            None,        # 왓챠는 탭 구분 없음
                    "services":       list[str],   # ["왓챠"]
                    "plan_name":      str,
                    "billing_cycle":  str,         # "monthly" | "yearly"
                    "price":          int,
                    "original_price": int | None,  # 연간: 월간 × 12, 월간: None
                    "description":    list[str],
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            context = browser.new_context(user_agent=USER_AGENT)
            page = context.new_page()
            try:
                # ① 월간 요금 수집
                page.goto(GRADE_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_selector("div[role='group'] > label[data-select]", timeout=30000)
                plans = self._parse_plans(page)

                # ② 연간 요금 수집 (description은 월간에서 재사용)
                description_by_name   = {p["plan_name"]: p["description"] for p in plans}
                monthly_price_by_name = {p["plan_name"]: p["price"] for p in plans}

                for delimiter, plan_name, yearly_selector in YEARLY_PLANS:
                    yearly_plan = self._parse_yearly_plan(
                        page, delimiter, plan_name, yearly_selector,
                        description_by_name.get(plan_name, []),
                        monthly_price_by_name.get(plan_name),
                    )
                    if yearly_plan:
                        plans.append(yearly_plan)

                return plans
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
                page.goto(GRADE_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_selector("div[role='group'] > label[data-select]", timeout=30000)
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
                page.goto(GRADE_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_selector("div[role='group'] > label[data-select]", timeout=30000)
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
            context = browser.new_context(user_agent=USER_AGENT)
            page = context.new_page()

            try:
                # ① 월간 요금 + description + cautions + company_info
                page.goto(GRADE_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_selector("div[role='group'] > label[data-select]", timeout=30000)

                plans        = self._parse_plans(page)
                cautions     = self._parse_cautions(page)
                company_info = self._parse_company_info(page)

                # ② 연간 요금 추가
                description_by_name   = {p["plan_name"]: p["description"] for p in plans}
                monthly_price_by_name = {p["plan_name"]: p["price"] for p in plans}

                for delimiter, plan_name, yearly_selector in YEARLY_PLANS:
                    yearly_plan = self._parse_yearly_plan(
                        page, delimiter, plan_name, yearly_selector,
                        description_by_name.get(plan_name, []),
                        monthly_price_by_name.get(plan_name),
                    )
                    if yearly_plan:
                        plans.append(yearly_plan)

                return {
                    "logo":         "",   # scrap_logo() NotImplementedError — 선택자 확인 필요
                    "plans":        plans,
                    "cautions":     cautions,
                    "company_info": company_info,
                }

            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 프로모션 감지
    # ═══════════════════════════════════════════════════════════════════════

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — DOM 파싱
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_plans(self, page: Page) -> list[dict]:
        """div[role='group'] > label[data-select] 구조에서 월간 구독권 파싱

        label 내부 구조:
            input[type='radio'] + div       → 요금제명 (예: 프리미엄)
            input[type='radio'] + div + div → 가격 (예: 12,900원)
            ul > li                         → description 항목
                li > div:nth-child(2)       → 항목 제목
                li > div:nth-child(3)       → 서브텍스트 (선택)
        """
        plans = []

        for label in page.locator("div[role='group'] > label[data-select]").all():
            name_el = label.locator("input[type='radio'] + div")
            if not name_el.count():
                continue
            plan_name = name_el.inner_text().strip()

            price_el = label.locator("input[type='radio'] + div + div")
            if not price_el.count():
                continue
            price_text = price_el.inner_text().strip()
            price = int(re.sub(r"[^\d]", "", price_text)) if re.search(r"\d", price_text) else None

            description = []
            for li in label.locator("ul > li").all():
                title_el = li.locator("div:nth-child(2)")
                sub_el   = li.locator("div:nth-child(3)")
                title    = title_el.inner_text().strip() if title_el.count() else ""
                if not title:
                    continue
                if sub_el.count():
                    sub = sub_el.inner_text().strip()
                    description.append(f"{title} ({sub})")
                else:
                    description.append(title)

            plans.append({
                "platform":       "watcha",
                "tab":            None,
                "services":       ["왓챠"],
                "plan_name":      plan_name,
                "billing_cycle":  "monthly",
                "price":          price,
                "original_price": None,
                "description":    description,
            })

        return plans

    def _parse_yearly_plan(
        self,
        page: Page,
        delimiter: str,
        plan_name: str,
        yearly_selector: str,
        description: list[str],
        monthly_price: int | None = None,
    ) -> dict | None:
        """choose_duration 페이지에서 연간 요금제 파싱

        label[data-select="{yearly_selector}"] 에서 연간 가격 추출.
        div.price → 총 결제 금액 (예: 129,900원)
        original_price = monthly_price * 12 (할인 전 원가)

        Args:
            delimiter:       URL 파라미터 (Grande / Tall)
            plan_name:       요금제명 (프리미엄 / 베이직)
            yearly_selector: label의 data-select 값
            description:     월간 요금제에서 재사용할 description
            monthly_price:   월간 요금 (original_price 계산용)
        """
        try:
            url = DURATION_URL.format(delimiter)
            page.goto(url, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_selector(f"label[data-select='{yearly_selector}']", timeout=30000)

            label = page.locator(f"label[data-select='{yearly_selector}']")
            if not label.count():
                print(f"[WARN] 연간 요금제 label을 찾을 수 없음: {yearly_selector}")
                return None

            price_el = label.locator("div.price")
            if not price_el.count():
                return None
            price_text = price_el.inner_text().strip()
            price = int(re.sub(r"[^\d]", "", price_text)) if re.search(r"\d", price_text) else None

            # original_price = 월간 요금 × 12 (할인 전 원가)
            original_price = monthly_price * 12 if monthly_price else None

            return {
                "platform":       "watcha",
                "tab":            None,
                "services":       ["왓챠"],
                "plan_name":      plan_name,
                "billing_cycle":  "yearly",
                "price":          price,
                "original_price": original_price,
                "description":    description,
            }

        except Exception as e:
            print(f"[ERROR] 연간 요금제 파싱 실패 ({plan_name}): {e}")
            return None

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """data-select="notification-item-container" ul 에서 주의사항 수집

        ul 바로 이전 형제 요소의 텍스트를 섹션 제목으로 사용.

        Returns:
            {"구독 안내": [...]}
        """
        cautions: dict[str, list[str]] = {}

        ul = page.query_selector("[data-select='notification-item-container']")
        if not ul:
            return cautions

        heading: str = page.evaluate("""
            () => {
                const ul = document.querySelector("[data-select='notification-item-container']");
                return ul?.previousElementSibling?.innerText?.trim() || '구독 안내';
            }
        """)

        items = [
            " ".join((li.inner_text() or "").split())
            for li in ul.query_selector_all("li")
            if (li.inner_text() or "").strip()
        ]

        if items:
            cautions[heading] = items

        return cautions

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer 내 연락처 및 사업자 정보 수집

        수집 범위:
            1) ul.GlobalFooter_contact: 고객센터 / 제휴 및 대외 협력 / B2B 구독권 구매 문의
               → "제목: 내용" 형식으로 병합
            2) div.GlobalFooter_extraBlock > ul.GlobalFooter_extraInfo: 회사명, 대표, 주소,
               사업자등록번호, 통신판매업 신고번호

        Returns:
            [
                "고객센터(이용 및 결제 문의): cs@watcha.com / 02-515-9985 (유료)",
                "제휴 및 대외 협력: https://watcha.team/contact",
                "B2B 구독권 구매 문의: 쿠프마케팅 (jinu1005@coopnc.com)",
                "주식회사 왓챠",
                "대표 박태훈",
                ...
                "통신판매업 신고번호 제 2019-서울서초-0965호",
            ]
        """
        info: list[str] = []

        footer = page.query_selector("footer.GlobalFooter_footer__5RoXg")
        if not footer:
            return info

        # ① 연락처 정보 (고객센터 / 제휴 / B2B)
        contact_ul = footer.query_selector("ul.GlobalFooter_contact__7RU8m")
        if contact_ul:
            for li in contact_ul.query_selector_all("li"):
                title_el = li.query_selector(".GlobalFooter_contactTitle__vhKoi")
                desc_el  = li.query_selector(".GlobalFooter_contactDescription__yHXsF")
                title = (title_el.inner_text() or "").strip() if title_el else ""
                desc  = (desc_el.inner_text() or "").strip() if desc_el else ""
                if title and desc:
                    info.append(f"{title}: {desc}")
                elif title:
                    info.append(title)

        # ② 사업자 정보 (회사명, 대표, 주소, 사업자등록번호, 통신판매업 신고번호)
        extra_block = footer.query_selector("div.GlobalFooter_extraBlock__OKW_k")
        if extra_block:
            for ul in extra_block.query_selector_all("ul.GlobalFooter_extraInfo__9XViL"):
                for li in ul.query_selector_all("li"):
                    text = (li.inner_text() or "").strip()
                    if text:
                        info.append(text)

        return info


if __name__ == "__main__":
    scraper = WatchaScraper()

    print("=== 개별 메서드 테스트 ===\n")

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