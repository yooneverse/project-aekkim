"""
디즈니플러스 프로모션 스크래퍼

공지사항 탭이 없으므로 홈페이지 대문(HOME_URL) 텍스트에서
PROMO_KEYWORDS('할인', '프로모션') 키워드 감지 방식으로 구현.

수집 주기: 주 1회
"""
from datetime import datetime

from playwright.sync_api import sync_playwright, Page

from base import PromotionScraper


HOME_URL = "https://www.disneyplus.com/ko-kr"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

PROMO_KEYWORDS = ['할인', '프로모션']


class DisneyPlusPromotionScraper(PromotionScraper):

    def scrap(self) -> list[dict]:
        """
        홈페이지 대문 텍스트에서 '할인', '프로모션' 키워드 감지

        PROMO_KEYWORDS 포함 텍스트가 있으면 raw dict 1건으로 반환,
        없으면 [] 반환

        Returns:
            [
                {
                    "platform": "disneyplus",
                    "title":    str,   # 감지된 키워드 포함 텍스트 요약
                    "date":     str,   # 수집일 (예: "2026.03.04")
                    "content":  str,   # 홈페이지 대문 전체 텍스트
                },
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT, locale="ko-KR").new_page()
            try:
                page.goto(HOME_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(3000)
                return self._detect_promotions(page)
            finally:
                browser.close()

    def _detect_promotions(self, page: Page) -> list[dict]:
        """홈페이지 대문 전체 텍스트에서 PROMO_KEYWORDS 감지

        키워드가 발견되면 홈페이지 텍스트를 raw dict 1건으로 반환.
        발견되지 않으면 [] 반환.
        """
        results: list[dict] = []
        try:
            full_text = (page.inner_text("body") or "").strip()
            today = datetime.now().strftime("%Y.%m.%d")

            for keyword in PROMO_KEYWORDS:
                if keyword in full_text:
                    results.append({
                        "platform": "disneyplus",
                        "title":    f"디즈니+ 홈페이지 '{keyword}' 키워드 감지",
                        "date":     today,
                        "content":  full_text,
                    })
                    break  # 키워드 하나라도 감지되면 전체 텍스트 1건으로 반환

        except Exception as e:
            print(f"[ERROR] 디즈니+ 프로모션 감지 실패: {e}")

        return results


if __name__ == "__main__":
    import json
    scraper = DisneyPlusPromotionScraper()

    print("=== DisneyPlusPromotionScraper 테스트 ===\n")
    results = scraper.scrap()
    print(f"수집된 프로모션 수: {len(results)}건")

    for i, item in enumerate(results, 1):
        print(f"\n[{i}] {item.get('date', '')} | {item.get('title', '')}")
        content = item.get("content", "")
        if content:
            print(f"  본문 앞부분: {content[:120]}{'...' if len(content) > 120 else ''}")

    if results:
        print("\n[전체 JSON 출력 — 첫 번째 건]")
        print(json.dumps(results[0], ensure_ascii=False, indent=2))
    else:
        print("감지된 프로모션 없음 (PROMO_KEYWORDS 미감지)")