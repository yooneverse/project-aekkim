"""
Gemini 구독 요금제 스크래퍼
URL: https://gemini.google/kr/subscriptions/?hl=ko

플랜 구성 (카드 4장)
  무료          — price=0, SERVICE_CATALOG 미포함 → 수집 제외
  Google AI Plus  — ₩11,000/월   (SERVICE_CATALOG: GEMINI / Plus)
  Google AI Pro   — ₩29,000/월   (SERVICE_CATALOG: GEMINI / Pro)
  Google AI Ultra — ₩360,000/월  (SERVICE_CATALOG: GEMINI / Ultra)

DOM 특징
  카드 컨테이너 : div._card_18nk1_75
  플랜명        : div._cardLogoText_18nk1_112
  가격 영역     : div._cardPricing_18nk1_308
    ① 정가 텍스트 : div._cardPricingMonthly_18nk1_315
                    → "매월 ₩11,000 KRW" 처럼 단일 문자열
                    → Pro는 이 div가 비어 있고 ②에 가격이 들어옴
    ② 프로모/실제가: div._cardPricingPromo_18nk1_338
                    → <span class="price-amount"> 안에 숫자
  기능 목록     : div._feature_18nk1_183
                    → div._featureTitle_  (기능 카테고리명)
                    → div._featureBody_   (기능 설명)

가격 파싱 규칙
  - _cardPricingMonthly_ 텍스트에 "₩N" 패턴이 있으면 → 정가
  - 없으면(Pro처럼 비어있는 경우) _cardPricingPromo_ > span.price-amount → 실제가
  - SERVICE_CATALOG 기준 plan_name 매핑:
      "Google AI Plus"  → "Plus"
      "Google AI Pro"   → "Pro"
      "Google AI Ultra" → "Ultra"

※ 이 페이지는 SSR(Next.js) 렌더링이므로 Playwright 불필요.
  requests + BeautifulSoup 으로 충분하지만,
  일관성을 위해 Playwright headless 방식 유지.
"""

import re
from playwright.sync_api import sync_playwright, Page
from bs4 import BeautifulSoup

from base import AIScraper


PRICING_URL = "https://gemini.google/kr/subscriptions/?hl=ko"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# 페이지 표시 플랜명 → SERVICE_CATALOG plan_name 매핑
PLAN_NAME_MAP: dict[str, str] = {
    "Google AI Plus":  "Plus",
    "Google AI Pro":   "Pro",
    "Google AI Ultra": "Ultra",
}


class GeminiScraper(AIScraper):

    # ════════════════════════════════════════════════════════════════════
    # 공통 인터페이스
    # ════════════════════════════════════════════════════════════════════

    def scrap_plans(self) -> list[dict]:
        """
        Returns:
            [
                {
                    "platform":       "gemini",
                    "tab":            None,
                    "services":       ["Gemini"],
                    "plan_name":      "Plus" | "Pro" | "Ultra",
                    "billing_cycle":  "monthly",
                    "price":          int,          # 월 원화
                    "original_price": int | None,   # 정가 (프로모 할인 시)
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
        페이지 하단 각주(footnote) 텍스트를 주의사항으로 수집.

        Returns:
            {"이용 안내": ["...", ...]}
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
        Google은 해외 법인이므로 국내 사업자 등록 정보 없음.
        footer 내 운영사 관련 텍스트 반환.

        Returns:
            ["Google LLC", ...] 또는 []
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
        soup = BeautifulSoup(page.content(), "html.parser")

        plans: list[dict] = []
        cards = soup.find_all(
            "div", class_=lambda c: c and "_card_18nk1_75" in c
        )

        for card in cards:
            plan = self._parse_card(card)
            if plan:
                plans.append(plan)

        return plans

    def _parse_card(self, card) -> dict | None:
        """
        카드 하나에서 플랜 정보 추출.

        가격 파싱 우선순위:
          1. _cardPricingMonthly_ 텍스트에 ₩ 숫자가 있으면 → 정가로 사용
          2. 없으면 _cardPricingPromo_ > span.price-amount → 실제가로 사용
          무료 플랜(price=0) 및 PLAN_NAME_MAP 미등록 플랜은 None 반환
        """
        # ── 플랜명 ──────────────────────────────────────────────────────
        logo_div = card.find(
            "div", class_=lambda c: c and "_cardLogoText_18nk1_112" in c
        )
        if not logo_div:
            return None

        # footnote 링크(<sup>) 텍스트 제거 후 플랜명만 추출
        for sup in logo_div.find_all("sup"):
            sup.decompose()
        raw_name = logo_div.get_text(strip=True)

        plan_name = PLAN_NAME_MAP.get(raw_name)
        if not plan_name:
            return None  # 무료 플랜 또는 미등록 플랜 제외

        # ── 가격 영역 ────────────────────────────────────────────────────
        pricing_div = card.find(
            "div", class_=lambda c: c and "_cardPricing_18nk1_308" in c
        )
        if not pricing_div:
            return None

        price, original_price = self._parse_price(pricing_div)
        if price is None:
            return None

        # ── 기능 목록 ────────────────────────────────────────────────────
        description = self._parse_features(card)

        return {
            "platform":       "gemini",
            "tab":            None,
            "services":       ["Gemini"],
            "plan_name":      plan_name,
            "billing_cycle":  "monthly",
            "price":          price,
            "original_price": original_price,
            "description":    description,
        }

    def _parse_price(self, pricing_div) -> tuple[int | None, int | None]:
        """
        가격 영역(div._cardPricing_18nk1_308)에서 (price, original_price) 반환.

        케이스별 동작:
          ┌─────────────────────────────────────────────────────────────┐
          │ 케이스 A — Monthly에 정가, Promo에 할인가 (Plus / Ultra)    │
          │   Monthly: "매월 ₩11,000 KRW"  → original_price = 11000    │
          │   Promo:   price-amount = "5,500"  → price = 5500           │
          │                                                             │
          │ 케이스 B — Monthly 비어있음, Promo에만 가격 (Pro)           │
          │   Monthly: ""  → original_price = None                     │
          │   Promo:   price-amount = "29,000"  → price = 29000         │
          └─────────────────────────────────────────────────────────────┘

        Returns:
            (price, original_price)
            price         : 실제 결제 금액 (프로모 or 정가)
            original_price: 할인 전 정가 (프로모 없으면 None)
        """
        original_price: int | None = None
        price: int | None = None

        # ① _cardPricingMonthly_ — 정가 텍스트 파싱
        monthly_div = pricing_div.find(
            "div", class_=lambda c: c and "_cardPricingMonthly_18nk1_315" in c
        )
        if monthly_div:
            monthly_text = monthly_div.get_text(strip=True)
            # "매월 ₩11,000 KRW" 형태에서 숫자 추출
            m = re.search(r"₩([\d,]+)", monthly_text)
            if m:
                original_price = int(m.group(1).replace(",", ""))

        # ② _cardPricingPromo_ — 실제 결제가 (price-amount span)
        promo_div = pricing_div.find(
            "div", class_=lambda c: c and "_cardPricingPromo_18nk1_338" in c
        )
        if promo_div:
            amount_span = promo_div.find("span", class_="price-amount")
            if amount_span:
                digits = amount_span.get_text(strip=True).replace(",", "")
                if digits.isdigit():
                    price = int(digits)

        # ③ 케이스 B: Promo에만 가격이 있을 때 (original_price 없음)
        if price is not None and original_price is None:
            # 정가 없이 Promo에만 가격 → original_price 불필요
            pass
        elif price is None and original_price is not None:
            # Promo 없이 정가만 → price = original_price, original_price = None
            price = original_price
            original_price = None

        return price, original_price

    def _parse_features(self, card) -> list[str]:
        """
        _feature_18nk1_183 내 Title + Body 쌍을 "Title: Body" 형식으로 수집.
        Body가 없으면 Title만 수집.
        """
        features: list[str] = []

        for feature_div in card.find_all(
            "div", class_=lambda c: c and "_feature_18nk1_183" in c
        ):
            # footnote sup 태그 제거
            for sup in feature_div.find_all(["sup", "a"]):
                if sup.name == "a" and "footnoteButton" in sup.get("class", []):
                    sup.decompose()

            title_div = feature_div.find(
                "div", class_=lambda c: c and "_featureTitle_18nk1_195" in c
            )
            body_div = feature_div.find(
                "div", class_=lambda c: c and "_featureBody_18nk1_195" in c
            )

            title = title_div.get_text(strip=True) if title_div else ""
            body = body_div.get_text(strip=True) if body_div else ""

            if title and body:
                features.append(f"{title}: {body}")
            elif title:
                features.append(title)

        return features

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """
        페이지 하단 footnote 항목 수집.
        각주 컨테이너: id가 "footnote:" 로 시작하는 요소들.
        """
        soup = BeautifulSoup(page.content(), "html.parser")
        items: list[str] = []

        # footnote 컨테이너 탐색 (id="footnote:xxx")
        for el in soup.find_all(id=re.compile(r"^footnote:")):
            text = el.get_text(separator=" ", strip=True)
            if text:
                items.append(text)

        return {"이용 안내": items} if items else {}

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer 내 텍스트 수집. Google은 국내 사업자 정보 없음."""
        info: list[str] = []
        try:
            soup = BeautifulSoup(page.content(), "html.parser")
            footer = soup.find("footer")
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
    scraper = GeminiScraper()

    print("=== [scrap_plans] ===")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 플랜 수집")
    for pl in plans:
        orig = f" (정가 ₩{pl['original_price']:,})" if pl["original_price"] else ""
        print(
            f"  {pl['plan_name']} / {pl['billing_cycle']}"
            f" / ₩{pl['price']:,}{orig}"
        )
        for feat in pl["description"][:2]:
            print(f"    - {feat[:80]}")

    print("\n=== [scrap_cautions] ===")
    cautions = scraper.scrap_cautions()
    for section, items in cautions.items():
        print(f"  [{section}] {len(items)}건")
        for item in items[:2]:
            print(f"    {item[:80]}...")

    print("\n=== [scrap_company_info] ===")
    for line in scraper.scrap_company_info():
        print(f"  {line}")