"""
웨이브 이벤트 스크래퍼

이벤트 목록 페이지에서 이벤트 제목·기간·이미지 URL 수집.

[구조 분석]
- 이벤트 목록: div.event-list > ul.event-row02 > li > a[href="#"]
- a 태그의 href는 전부 "#" — 직접 URL 추출 불가
- 클릭 시 SPA history.pushState로 /customer/event_view?eventId={id} 로 이동
- 따라서 각 카드를 클릭 → URL 변화 감지 → eventId 추출 → 상세 페이지 수집

수집 주기: 주 1회
"""
import logging
import re

from playwright.sync_api import sync_playwright, Page

from base import PromotionScraper


EVENT_LIST_URL = "https://www.wavve.com/customer/event_list"
EVENT_VIEW_URL = "https://www.wavve.com/customer/event_view"
BASE_URL       = "https://www.wavve.com"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

logger = logging.getLogger(__name__)


class WavvePromotionScraper(PromotionScraper):

    def scrap(self) -> list[dict]:
        """
        이벤트 목록 페이지에서 이벤트 수집

        [흐름]
        1. 목록 페이지 접속 → li 개수 파악
        2. 각 li 클릭 → wait_for_url로 event_view URL 변화 감지
        3. eventId 추출 → 상세 정보 수집
        4. 목록으로 back() → 다음 li 클릭 반복

        Returns:
            [
                {
                    "platform": "wavve",
                    "title":    str,
                    "date":     str,
                    "content":  str,  # "image_url:{src}" 형태
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                return self._collect_events(page)
            except Exception as e:
                logger.error("[WavvePromotionScraper] 이벤트 수집 실패: %s", e)
                return []
            finally:
                browser.close()

    def _collect_events(self, page: Page) -> list[dict]:
        """목록 페이지 접속 → 각 카드 클릭 → 상세 수집 반복"""
        results: list[dict] = []

        page.goto(EVENT_LIST_URL, wait_until="domcontentloaded", timeout=60000)
        try:
            page.wait_for_selector("div.event-list ul.event-row02 li", timeout=15000)
        except Exception:
            logger.warning("[WavvePromotionScraper] 이벤트 목록 없음")
            return results

        # 목록의 li 개수 파악 (클릭 후 DOM이 바뀌므로 미리 개수만 저장)
        li_count = page.locator("div.event-list ul.event-row02 li").count()
        logger.info("[WavvePromotionScraper] 이벤트 카드 %d개 발견", li_count)

        for i in range(li_count):
            try:
                # 목록 페이지인지 확인 후 i번째 카드 클릭
                page.wait_for_selector("div.event-list ul.event-row02 li", timeout=10000)
                card = page.locator("div.event-list ul.event-row02 li").nth(i)

                # 목록에서 제목·날짜 미리 수집 (클릭 전)
                title_pre = ""
                date_pre  = ""
                try:
                    title_pre = card.locator("strong.event-con-tit").inner_text().strip()
                    date_pre  = card.locator("span.event-td").first.inner_text().strip()
                except Exception:
                    pass

                # 카드 클릭 → event_view URL로 이동 감지
                card.locator("a").click()
                try:
                    page.wait_for_url("**/event_view**", timeout=8000)
                except Exception:
                    logger.warning("[WavvePromotionScraper] %d번째 카드 클릭 후 URL 변화 없음", i)
                    page.goto(EVENT_LIST_URL, wait_until="domcontentloaded", timeout=30000)
                    continue

                # 상세 페이지 수집
                detail = self._parse_event_detail(page, title_pre, date_pre)
                if detail:
                    results.append(detail)

                # 목록으로 복귀
                page.go_back()
                page.wait_for_selector("div.event-list ul.event-row02 li", timeout=10000)
                page.wait_for_timeout(500)

            except Exception as e:
                logger.error("[WavvePromotionScraper] %d번째 카드 처리 실패: %s", i, e)
                # 오류 시 목록 페이지로 강제 복귀
                try:
                    page.goto(EVENT_LIST_URL, wait_until="domcontentloaded", timeout=30000)
                    page.wait_for_selector("div.event-list ul.event-row02 li", timeout=10000)
                except Exception:
                    break

        return results

    def _parse_event_detail(self, page: Page, title_pre: str, date_pre: str) -> dict | None:
        """
        event_view 페이지에서 상세 정보 수집

        DOM 구조:
            h3.event-d-tit  → 이벤트 제목
            p.event-d-date  → 기간
            div.event-contents img → 이미지 URL
        """
        try:
            page.wait_for_timeout(800)

            # 제목
            title_el = page.query_selector("h3.event-d-tit")
            title    = (title_el.inner_text() or "").strip() if title_el else title_pre

            # 기간
            date_el = page.query_selector("p.event-d-date")
            date    = (date_el.inner_text() or "").strip() if date_el else date_pre

            # 이미지 URL
            img_urls: list[str] = page.evaluate("""() => {
                return Array.from(document.querySelectorAll("div.event-contents img"))
                    .map(img => img.getAttribute("src") || "")
                    .filter(src => src && !src.startsWith("data:"));
            }""")
            content = "image_url:" + ",".join(img_urls) if img_urls else ""

            # eventId (현재 URL에서 추출)
            current_url = page.url
            event_id_match = re.search(r"eventId=(\d+)", current_url)
            event_id = event_id_match.group(1) if event_id_match else ""

            logger.info("[WavvePromotionScraper] 수집 완료: [eventId=%s] %s", event_id, title)

            return {
                "platform": "wavve",
                "title":    title,
                "date":     date,
                "content":  content,
            }

        except Exception as e:
            logger.error("[WavvePromotionScraper] 상세 파싱 실패: %s", e)
            return None


if __name__ == "__main__":
    import json
    scraper = WavvePromotionScraper()

    print("=== WavvePromotionScraper 테스트 ===\n")
    results = scraper.scrap()
    print(f"수집된 이벤트 수: {len(results)}건")

    for i, item in enumerate(results, 1):
        print(f"\n[{i}] {item.get('date', '')} | {item.get('title', '')}")
        content = item.get("content", "")
        if content:
            print(f"  content: {content[:120]}{'...' if len(content) > 120 else ''}")

    if results:
        print("\n[전체 JSON 출력 — 첫 번째 건]")
        print(json.dumps(results[0], ensure_ascii=False, indent=2))