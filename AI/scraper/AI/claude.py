"""
Claude 구독 요금제 스크래퍼
URL: https://support.claude.com/ko/articles/11049762-claude-플랜-선택하기

페이지 구조
  Anthropic 공식 요금 페이지가 아닌 고객지원 문서 기반.
  div.intercom-interblocks-table > table > tbody > tr 구조로
  플랜명 / 가격(USD) / 청구 주기 / 사용 용량 / 추천 대상 5개 열을 가짐.

수집 대상 (SERVICE_CATALOG 기준 CLAUDE 플랜)
  ┌──────────────┬───────────┬──────────────┬────────────────────────────┐
  │ 페이지 표시  │ USD 가격  │ billing      │ SERVICE_CATALOG plan_name  │
  ├──────────────┼───────────┼──────────────┼────────────────────────────┤
  │ Pro          │ $20/월    │ monthly      │ Pro                        │
  │ Pro          │ $200/년   │ yearly       │ Pro  (÷12 → 월 환산)       │
  │ Max 5x       │ $100/월   │ monthly      │ Max 5x                     │
  │ Max 20x      │ $200/월   │ monthly      │ Max 10x  ※ 아래 참고       │
  └──────────────┴───────────┴──────────────┴────────────────────────────┘

  ※ SERVICE_CATALOG의 "Max 10x"는 페이지의 "Max 20x"와 동일 상품.
     (카탈로그가 용량 배수를 10x로, 페이지는 20x로 표기하는 차이)
     → PLAN_NAME_MAP 으로 명시적 매핑.

USD → KRW 환율
  - 런타임에 환율 API(exchangerate-api.com)를 호출해 실시간 환율 적용.
  - API 호출 실패 시 FALLBACK_RATE(상수)를 사용하고 경고 로그 출력.
  - 원화 변환 후 100원 단위 반올림 (실제 청구 금액과 근사치 맞춤).

연간 요금 처리
  - billing_cycle = 'yearly', price = USD 연간 금액 ÷ 12 → KRW 월 환산.
  - original_price = None  (Claude 연간은 할인 전 월정가 미표시).

※ 이 페이지는 Intercom 기반 정적 HTML → Playwright 불필요.
   requests + BeautifulSoup 으로 충분하지만 일관성을 위해 Playwright 유지.
"""

import re
import requests
from playwright.sync_api import sync_playwright, Page
from bs4 import BeautifulSoup

from base import AIScraper


PRICING_URL = (
    "https://support.claude.com/ko/articles/"
    "11049762-claude-%ED%94%8C%EB%9E%9C-%EC%84%A0%ED%83%9D%ED%95%98%EA%B8%B0"
)

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

# 환율 API 호출 실패 시 사용하는 고정 환율 (수동 업데이트 필요)
FALLBACK_RATE: float = 1_450.0

# 페이지 표시 플랜명 → SERVICE_CATALOG plan_name 매핑
# (Free / Max 20x 는 카탈로그와 표기가 달라 명시적 처리)
PLAN_NAME_MAP: dict[str, str] = {
    "Pro":     "Pro",
    "Max 5x":  "Max 5x",
    "Max 20x": "Max 10x",   # 페이지: 20x 표기 / 카탈로그: 10x 표기
}

# 청구 주기 텍스트 → billing_cycle 코드
BILLING_CYCLE_MAP: dict[str, str] = {
    "월간":           "monthly",
    "연간":           "yearly",
    "월간 또는 연간": None,      # Pro: monthly/yearly 두 행으로 분리 처리
}


class ClaudeScraper(AIScraper):

    # ════════════════════════════════════════════════════════════════════
    # 공통 인터페이스
    # ════════════════════════════════════════════════════════════════════

    def scrap_plans(self) -> list[dict]:
        """
        Returns:
            [
                {
                    "platform":       "claude",
                    "tab":            None,
                    "services":       ["Claude"],
                    "plan_name":      "Pro" | "Max 5x" | "Max 10x",
                    "billing_cycle":  "monthly" | "yearly",
                    "price":          int,   # KRW 월 환산
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
        이 페이지는 고객지원 문서이므로 별도 주의사항 섹션이 없음.
        관련 자료 링크 목록을 반환.

        Returns:
            {"관련 자료": ["Pro 플랜이란 무엇입니까?", ...]}
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
        Anthropic은 해외 법인이므로 국내 사업자 등록 정보 없음.
        빈 리스트 반환.
        """
        return []

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
                    "company_info": self.scrap_company_info(),
                }
            finally:
                browser.close()

    # ════════════════════════════════════════════════════════════════════
    # Private — 환율
    # ════════════════════════════════════════════════════════════════════

    def _get_usd_to_krw(self) -> float:
        """
        실시간 USD→KRW 환율 조회.
        exchangerate-api.com 무료 엔드포인트 사용 (API 키 불필요).
        실패 시 FALLBACK_RATE 반환.
        """
        try:
            resp = requests.get(
                "https://api.exchangerate-api.com/v4/latest/USD",
                timeout=5,
            )
            resp.raise_for_status()
            rate = resp.json()["rates"]["KRW"]
            print(f"[INFO] 환율 조회 성공: 1 USD = {rate:,.1f} KRW")
            return float(rate)
        except Exception as e:
            print(f"[WARN] 환율 조회 실패 ({e}), 고정값 사용: {FALLBACK_RATE} KRW/USD")
            return FALLBACK_RATE

    def _usd_to_krw(self, usd: float, rate: float) -> int:
        """
        USD → KRW 변환 후 100원 단위 반올림.
        예) $20 × 1,450 = 29,000 → 29,000
            $16.67 × 1,450 = 24,171 → 24,200
        """
        krw = usd * rate
        return int(round(krw / 100) * 100)

    # ════════════════════════════════════════════════════════════════════
    # Private — DOM 파싱
    # ════════════════════════════════════════════════════════════════════

    def _parse_plans(self, page: Page) -> list[dict]:
        soup = BeautifulSoup(page.content(), "html.parser")
        rate = self._get_usd_to_krw()

        table_div = soup.find("div", class_="intercom-interblocks-table")
        if not table_div:
            print("[ERROR] 요금제 테이블을 찾을 수 없음")
            return []

        plans: list[dict] = []
        rows = table_div.find_all("tr")

        for row in rows:
            cells = row.find_all("td")
            if not cells or len(cells) < 3:
                continue  # 헤더 행 skip

            # ── 셀 텍스트 추출 ──────────────────────────────────────────
            raw_plan    = cells[0].get_text(separator="\n", strip=True)
            raw_price   = cells[1].get_text(separator="\n", strip=True)
            raw_cycle   = cells[2].get_text(separator="\n", strip=True)
            raw_usage   = cells[3].get_text(separator=" ",  strip=True) if len(cells) > 3 else ""
            raw_target  = cells[4].get_text(separator=" ",  strip=True) if len(cells) > 4 else ""

            # ── 플랜명 매핑 ──────────────────────────────────────────────
            plan_name = PLAN_NAME_MAP.get(raw_plan)
            if not plan_name:
                continue  # Free 등 미수집 플랜 skip

            # ── 가격 파싱 및 플랜 생성 ──────────────────────────────────
            parsed = self._parse_price_cell(
                plan_name=plan_name,
                raw_price=raw_price,
                raw_cycle=raw_cycle,
                raw_usage=raw_usage,
                raw_target=raw_target,
                rate=rate,
            )
            plans.extend(parsed)

        return plans

    def _parse_price_cell(
        self,
        plan_name: str,
        raw_price: str,
        raw_cycle: str,
        raw_usage: str,
        raw_target: str,
        rate: float,
    ) -> list[dict]:
        """
        가격 셀 텍스트를 파싱해 1~2개의 플랜 dict 리스트 반환.

        처리 케이스:
          ① 단일 월간  : "$100"         → monthly 1건
          ② 월간+연간  : "$20/월\n$200/년" → monthly + yearly 2건
        """
        description = []
        if raw_usage:
            description.append(f"사용 용량: {raw_usage}")
        if raw_target:
            description.append(f"추천 대상: {raw_target}")

        base = {
            "platform":       "claude",
            "tab":            None,
            "services":       ["Claude"],
            "plan_name":      plan_name,
            "original_price": None,
            "description":    description,
        }

        plans = []
        price_lines = [l.strip() for l in raw_price.splitlines() if l.strip()]

        for line in price_lines:
            usd, cycle = self._extract_usd_and_cycle(line, raw_cycle)
            if usd is None or cycle is None:
                continue

            # 연간 요금: 연 총액 ÷ 12 → 월 환산 KRW
            if cycle == "yearly":
                monthly_usd = usd / 12
            else:
                monthly_usd = usd

            krw = self._usd_to_krw(monthly_usd, rate)

            plans.append({
                **base,
                "billing_cycle": cycle,
                "price":         krw,
            })

        return plans

    def _extract_usd_and_cycle(
        self,
        price_line: str,
        raw_cycle: str,
    ) -> tuple[float | None, str | None]:
        """
        가격 줄 텍스트 + 청구 주기 텍스트에서 (USD 금액, billing_cycle) 추출.

        price_line 형식 예시:
          "$20/월"  → (20.0, "monthly")
          "$200/년" → (200.0, "yearly")
          "$100"    → (100.0, "monthly")  ← raw_cycle로 보완
        """
        m = re.search(r"\$([\d,]+(?:\.\d+)?)", price_line)
        if not m:
            return None, None

        usd = float(m.group(1).replace(",", ""))

        # 줄 자체에 /월 or /년 명시된 경우
        if "/월" in price_line:
            return usd, "monthly"
        if "/년" in price_line:
            return usd, "yearly"

        # 줄에 명시 없으면 raw_cycle 텍스트로 판단
        if "연간" in raw_cycle and "월간" not in raw_cycle:
            return usd, "yearly"
        return usd, "monthly"

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """
        '관련 자료' 섹션의 링크 제목 목록 수집.
        """
        soup = BeautifulSoup(page.content(), "html.parser")
        items: list[str] = []

        related = soup.find("section", class_=lambda c: c and "related_articles" in c)
        if related:
            for a in related.find_all("a", attrs={"data-testid": "article-link"}):
                title = a.get_text(strip=True)
                href  = a.get("href", "")
                if title:
                    items.append(f"{title} — {href}")

        return {"관련 자료": items} if items else {}


# ════════════════════════════════════════════════════════════════════════
# 단독 실행 테스트
# ════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    scraper = ClaudeScraper()

    print("=== [scrap_plans] ===")
    plans = scraper.scrap_plans()
    print(f"총 {len(plans)}개 플랜 수집")
    for pl in plans:
        print(
            f"  {pl['plan_name']:<10} / {pl['billing_cycle']:<8}"
            f" / ₩{pl['price']:>10,}"
        )
        for d in pl["description"]:
            print(f"    - {d}")

    print("\n=== [scrap_cautions] ===")
    cautions = scraper.scrap_cautions()
    for section, items in cautions.items():
        print(f"  [{section}]")
        for item in items:
            print(f"    {item}")

    print("\n=== [scrap_company_info] ===")
    info = scraper.scrap_company_info()
    print(f"  {info if info else '(해외 법인 — 국내 사업자 정보 없음)'}")