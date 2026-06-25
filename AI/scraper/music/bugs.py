"""
벅스 구독 요금제 스크래퍼
URL: https://music.bugs.co.kr/pay/public

페이지 구조
  ul.productList  (섹션별, 복수)
    ├── ul.offlineStreaming  : 무제한 듣기 + 오프라인 재생 플랜
    ├── ul.streaming        : 무제한 듣기 플랜
    └── ul.mp3Download      : MP3 다운로드 (수집 제외)

  각 li > div.productItem__wrap
    div.productItem__name > strong          → 플랜명
    div.productItem__description > span > em → 플랜 설명
    ul.productItem__options > li
      div.productItem__subscription
        span.productItem__subscriptionType  → "자동결제" | "30일" | ""
        strong.productItem__totalPrice      → 가격 (예: "11,990원")

수집 규칙
  - subscriptionType == "자동결제" 인 option만 수집 → 정기결제 금액
  - mp3Download 섹션 전체 제외
  - 가격이 비어있는 option 제외
  - plan_name은 페이지 표시명 그대로 사용 (별도 매핑 없음)

※ 이 페이지는 정적 HTML. Playwright 일관성 유지.
"""

import re
from playwright.sync_api import sync_playwright, Page
from bs4 import BeautifulSoup

from base import MusicScraper


PRICING_URL = "https://music.bugs.co.kr/pay/public"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# 수집 제외 섹션 클래스
EXCLUDE_SECTIONS: set[str] = {"mp3Download"}


class BugsScraper(MusicScraper):

    # ════════════════════════════════════════════════════════════════════
    # 공통 인터페이스
    # ════════════════════════════════════════════════════════════════════

    def scrap_plans(self) -> list[dict]:
        """
        Returns:
            [
                {
                    "platform":       "bugs",
                    "tab":            None,
                    "services":       ["벅스"],
                    "plan_name":      str,
                    "billing_cycle":  "monthly",
                    "price":          int,
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
                page.wait_for_timeout(2_000)
                return self._parse_plans(page)
            finally:
                browser.close()

    def scrap_cautions(self) -> dict[str, list[str]]:
        """
        페이지 하단 유의사항 수집.

        Returns:
            {"유의사항": ["...", ...]}
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(2_000)
                return self._parse_cautions(page)
            finally:
                browser.close()

    def scrap_company_info(self) -> list[str]:
        """footer 사업자 정보 수집."""
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(2_000)
                return self._parse_company_info(page)
            finally:
                browser.close()

    def scrap(self) -> dict:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(PRICING_URL, wait_until="domcontentloaded", timeout=60_000)
                page.wait_for_timeout(2_000)
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
        soup = BeautifulSoup(page.content(), "html.parser")
        plans: list[dict] = []

        for ul in soup.find_all("ul", class_="productList"):
            ul_classes = set(ul.get("class", []))

            # 다운로드 섹션 제외
            if ul_classes & EXCLUDE_SECTIONS:
                continue

            for li in ul.find_all("li", recursive=False):
                plan = self._parse_item(li)
                if plan:
                    plans.append(plan)

        return plans

    def _parse_item(self, li) -> dict | None:
        """
        li 하나에서 자동결제 option을 찾아 플랜 dict 반환.
        자동결제 option이 없거나 가격이 없으면 None.
        """
        # ── 플랜명 (페이지 표시명 그대로 사용) ───────────────────────
        name_el = li.find("div", class_="productItem__name")
        if not name_el:
            return None
        plan_name = name_el.get_text(strip=True)
        if not plan_name:
            return None

        # ── 설명 ──────────────────────────────────────────────────────
        desc_el = li.find("em")
        description: list[str] = []
        if desc_el:
            desc = desc_el.get_text(strip=True)
            if desc:
                description.append(desc)

        # ── 자동결제 option 탐색 ──────────────────────────────────────
        for option in li.find_all("div", class_="productItem__subscription"):
            type_el  = option.find("span", class_="productItem__subscriptionType")
            price_el = option.find("strong", class_="productItem__totalPrice")

            sub_type    = type_el.get_text(strip=True)  if type_el  else ""
            price_text  = price_el.get_text(strip=True) if price_el else ""

            if sub_type != "자동결제" or not price_text:
                continue

            price = self._parse_price(price_text)
            if price is None:
                continue

            return {
                "platform":       "bugs",
                "tab":            None,
                "services":       ["벅스"],
                "plan_name":      plan_name,
                "billing_cycle":  "monthly",
                "price":          price,
                "original_price": None,
                "description":    description,
            }

        return None

    def _parse_price(self, text: str) -> int | None:
        """'11,990원' → 11990"""
        digits = re.sub(r"[^\d]", "", text)
        return int(digits) if digits else None

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """
        페이지 하단 유의사항 li 항목 수집.
        벅스는 ul.listNotice 또는 div.guide 내 li로 유의사항을 표시.
        """
        soup = BeautifulSoup(page.content(), "html.parser")
        items: list[str] = []

        # 유의사항 영역 탐색
        for container in soup.find_all(
            class_=lambda c: c and any(
                kw in " ".join(c) for kw in ("notice", "caution", "guide", "info", "tip")
            )
        ):
            for li in container.find_all("li"):
                text = li.get_text(separator=" ", strip=True)
                if text and len(text) > 10:
                    items.append(text)

        # 중복 제거
        seen: set[str] = set()
        unique: list[str] = []
        for item in items:
            if item not in seen:
                seen.add(item)
                unique.append(item)

        return {"유의사항": unique} if unique else {}

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer 내 사업자 정보 수집."""
        info: list[str] = []
        try:
            soup = BeautifulSoup(page.content(), "html.parser")
            footer = soup.find("footer") or soup.find(id="footer")
            if not footer:
                return info
            for line in footer.get_text(separator="\n", strip=True).splitlines():
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
    scraper = BugsScraper()

    print("=== [scrap_plans] ===")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 플랜 수집")
    for pl in plans:
        print(f"  {pl['plan_name']:<38} / ₩{pl['price']:>8,}  | {pl['description']}")

    print("\n=== [scrap_cautions] ===")
    cautions = scraper.scrap_cautions()
    for section, items in cautions.items():
        print(f"  [{section}] {len(items)}건")
        for item in items[:3]:
            print(f"    {item[:80]}...")

    print("\n=== [scrap_company_info] ===")
    for line in scraper.scrap_company_info()[:5]:
        print(f"  {line}")