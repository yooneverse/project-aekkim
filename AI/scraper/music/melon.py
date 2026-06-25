"""
멜론 구독 요금제 스크래퍼
URL: https://www.melon.com/buy/pamphlet/all.htm

수집 대상
  h4.title 텍스트에 '정기결제'가 포함된 product_purchase 카드만 수집.
  '티켓 30일', '자동충전' 등 정기구독이 아닌 일회성 상품은 제외.
  plan_name은 '정기결제' 제거 후 페이지 표시명 그대로 사용.

페이지 구조
  div.wrap_section02  (카테고리 섹션, 복수)
    h3                → 카테고리명 (예: '무제한 듣기', 'MP3 다운')
    div.product_purchase  (상품 카드, 복수)
      h4.title              → 상품명 (예: '스트리밍클럽 정기결제')
      p.text                → 상품 설명 (예: '무제한 듣기')
      div.regular_payment
        span.sale_price     → 가격 (예: '8,690원')

SERVICE_CATALOG 기준 수집 플랜 (plan_name 매핑)
  페이지 표시명                → plan_name
  ─────────────────────────────────────────
  스트리밍클럽 정기결제        → 스트리밍클럽
  스트리밍 플러스 정기결제     → 스트리밍 플러스
  Hi-Fi스트리밍클럽 정기결제   → Hi-Fi 스트리밍클럽
  모바일 스트리밍클럽 정기결제 → 모바일 스트리밍클럽

  ※ MP3 다운, 횟수 듣기 계열은 SERVICE_CATALOG 미등록.
     SCRAPE_ALL_JEONGGI 플래그로 전체 수집 여부 제어 (기본 False).

※ 이 페이지는 서버사이드 렌더링 정적 HTML.
   Playwright 없이 requests + BeautifulSoup 으로 충분하나
   일관성을 위해 Playwright headless 방식 유지.
"""

import re
from playwright.sync_api import sync_playwright, Page
from bs4 import BeautifulSoup

from base import MusicScraper


PRICING_URL = "https://www.melon.com/buy/pamphlet/all.htm"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)




class MelonScraper(MusicScraper):

    # ════════════════════════════════════════════════════════════════════
    # 공통 인터페이스
    # ════════════════════════════════════════════════════════════════════

    def scrap_plans(self) -> list[dict]:
        """
        Returns:
            [
                {
                    "platform":       "melon",
                    "tab":            None,
                    "services":       ["멜론"],
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
        페이지 내 주의사항/유의사항 텍스트 수집.

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
        """
        footer 사업자 정보 수집.

        Returns:
            ["(주)카카오엔터테인먼트 ...", ...]
        """
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

        for section in soup.find_all("div", class_="wrap_section02"):
            # 섹션 카테고리명 (h3)
            h3 = section.find("h3")
            category = h3.get_text(strip=True) if h3 else ""
            # h3 내 span.tag 제거 후 순수 카테고리명만 추출
            if h3:
                for tag in h3.find_all("span", class_="tag"):
                    tag.decompose()
                category = h3.get_text(strip=True)

            for product in section.find_all("div", class_="product_purchase"):
                plan = self._parse_product(product, category)
                if plan:
                    plans.append(plan)

        return plans

    def _parse_product(self, product, category: str) -> dict | None:
        """
        product_purchase 카드 하나를 파싱해 표준 dict 반환.
        h4.title에 '정기결제'가 없으면 skip (티켓 30일, 자동충전 등).
        plan_name은 '정기결제' 제거 후 페이지 표시명 그대로 사용.
        """
        # ── 상품명 ────────────────────────────────────────────────────
        title_el = product.find("h4", class_="title")
        if not title_el:
            return None

        # '이용권 자세히 알기' 링크 텍스트 제거
        for a in title_el.find_all("a"):
            a.decompose()
        raw_title = title_el.get_text(strip=True)

        # '정기결제' 없으면 skip (티켓 30일, 자동충전 등)
        if "정기결제" not in raw_title:
            return None

        # '정기결제' 제거 후 순수 상품명을 plan_name으로 사용
        plan_name = raw_title.replace("정기결제", "").strip()

        # ── 가격 ──────────────────────────────────────────────────────
        price_el = product.find("span", class_="sale_price")
        if not price_el:
            return None
        price = self._parse_price(price_el.get_text(strip=True))
        if price is None:
            return None

        # ── 설명 ──────────────────────────────────────────────────────
        desc_el = product.find("p", class_="text")
        description: list[str] = []
        if desc_el:
            desc_text = desc_el.get_text(strip=True)
            if desc_text:
                description.append(desc_text)
        if category:
            description.append(f"카테고리: {category}")

        return {
            "platform":       "melon",
            "tab":            None,
            "services":       ["멜론"],
            "plan_name":      plan_name,
            "billing_cycle":  "monthly",
            "price":          price,
            "original_price": None,
            "description":    description,
        }

    def _parse_price(self, text: str) -> int | None:
        """'8,690원' → 8690"""
        digits = re.sub(r"[^\d]", "", text)
        return int(digits) if digits else None

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """
        페이지 내 유의사항 관련 텍스트 수집.
        wrap_notice, list_notice 등의 클래스명을 탐색.
        """
        soup = BeautifulSoup(page.content(), "html.parser")
        items: list[str] = []

        # 멜론 페이지의 유의사항 영역 공통 탐색
        for el in soup.find_all(
            class_=lambda c: c and any(
                kw in " ".join(c) for kw in ("notice", "caution", "guide", "info")
            )
        ):
            text = el.get_text(separator=" ", strip=True)
            if text and len(text) > 10:
                items.append(text)

        # 중복 제거 (부모/자식 관계로 같은 텍스트가 반복될 수 있음)
        seen: set[str] = set()
        unique_items: list[str] = []
        for item in items:
            if item not in seen:
                seen.add(item)
                unique_items.append(item)

        return {"유의사항": unique_items} if unique_items else {}

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
    scraper = MelonScraper()

    print("=== [scrap_plans] ===")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 플랜 수집 (SCRAPE_ALL_JEONGGI={SCRAPE_ALL_JEONGGI})")
    for pl in plans:
        print(f"  {pl['plan_name']:<22} / ₩{pl['price']:>8,}  | {pl['description']}")

    print("\n=== [scrap_cautions] ===")
    cautions = scraper.scrap_cautions()
    for section, items in cautions.items():
        print(f"  [{section}] {len(items)}건")
        for item in items[:2]:
            print(f"    {item[:80]}...")

    print("\n=== [scrap_company_info] ===")
    for line in scraper.scrap_company_info()[:5]:
        print(f"  {line}")