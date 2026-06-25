"""
티빙 공지사항 스크래퍼

고객센터 공지사항에서 '공지' 태그 항목만 수집 후 상세 본문까지 수집.
요금 변경·서비스 정책 변경 등 중요 공지 감지 목적.

수집 주기: 주 1회
"""
from playwright.sync_api import sync_playwright, BrowserContext

from base import PromotionScraper


NOTICE_URL = "https://www.tving.com/help/notice"
BASE_URL   = "https://www.tving.com"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


class TvingPromotionScraper(PromotionScraper):

    def scrap(self) -> list[dict]:
        """
        고객센터 공지사항에서 '공지' 태그 항목만 수집 후 상세 본문까지 수집

        Returns:
            [
                {
                    "platform": "tving",
                    "title":    str,   # 공지 제목
                    "date":     str,   # 작성일 (예: "2026.03.04")
                    "content":  str,   # 상세 페이지 본문 전체 텍스트
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            context = browser.new_context(user_agent=USER_AGENT)
            try:
                notices = self._parse_notice_list(context)
                for item in notices:
                    item["content"] = self._parse_notice_detail(context, item["url"])
                    item.pop("url", None)
                return notices
            finally:
                browser.close()

    def _parse_notice_list(self, context: BrowserContext) -> list[dict]:
        """
        공지사항 목록에서 '공지' 항목의 제목·날짜·링크 수집

        DOM 구조:
            <div class="board__table">
              <tbody>
                <tr href="/help/notice/{id}?page=1">
                  <td>공지</td>
                  <td>제목</td>
                  <td class="align-center">2026.03.04</td>
        """
        notices: list[dict] = []
        page = context.new_page()
        try:
            page.goto(NOTICE_URL, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(2000)

            for row in page.query_selector_all("div.board__table tbody tr"):
                tds = row.query_selector_all("td")
                if len(tds) < 3:
                    continue
                if (tds[0].inner_text() or "").strip() != "공지":
                    continue

                title = (tds[1].inner_text() or "").strip()
                date  = (tds[2].inner_text() or "").strip()
                href  = (row.get_attribute("href") or "").strip()

                if title and href:
                    notices.append({
                        "platform": "tving",
                        "title":    title,
                        "date":     date,
                        "url":      BASE_URL + href,
                    })
        finally:
            page.close()

        return notices

    def _parse_notice_detail(self, context: BrowserContext, url: str) -> str:
        """
        공지사항 상세 페이지 본문 수집

        DOM 구조:
            <div class="topic topic__open-main">
                <h4>제목</h4>
                <div><p>본문...</p></div>
        """
        page = context.new_page()
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(1500)

            content_div = page.query_selector("div.topic.topic__open-main")
            if not content_div:
                return ""

            return (content_div.inner_text() or "").strip()
        finally:
            page.close()


if __name__ == "__main__":
    import json
    scraper = TvingPromotionScraper()

    print("=== TvingPromotionScraper 테스트 ===\n")
    results = scraper.scrap()
    print(f"수집된 공지 수: {len(results)}건")

    for i, item in enumerate(results, 1):
        print(f"\n[{i}] {item.get('date', '')} | {item.get('title', '')}")
        content = item.get("content", "")
        if content:
            print(f"  본문 앞부분: {content[:120]}{'...' if len(content) > 120 else ''}")

    if results:
        print("\n[전체 JSON 출력 — 첫 번째 건]")
        print(json.dumps(results[0], ensure_ascii=False, indent=2))