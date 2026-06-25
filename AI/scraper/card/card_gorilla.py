"""
카드고릴라 스크래퍼 (card_gorilla.py)

[핵심 구조 파악]
- 카드고릴라는 Vue.js SPA 구조임
- 검색 결과 리스트의 a.b_view 버튼 href = 'javascript:;' (실제 URL 없음)
- b_view 클릭 시 SPA 라우터가 /card/detail/{id} 로 URL 변경함
- 이미지 URL 패턴: /card/{id}/card_img/... → 카드 ID 추출 가능 (백업 방법)
- '카드사 바로가기' 버튼도 href='javascript:' → 클릭 후 새 탭 URL 감지 필요
"""
import re
import time
import json
from playwright.sync_api import sync_playwright, Page, BrowserContext

from base import CardScraper

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

BASE_URL = "https://www.card-gorilla.com"

SEARCH_URLS = {
    "credit": f"{BASE_URL}/search/card?cate=CRD&search_benefit=19,185&annual=0,5&pre=0,3&sort=ranking&is_discon=1",
    "check":  f"{BASE_URL}/search/card?cate=CHK&search_benefit=19,185&annual=0,5&pre=0,3&sort=ranking&is_discon=1",
}


class CardGorillaScraper(CardScraper):

    def __init__(self):
        self.base_url = BASE_URL

    # ───────────────────────────────────────────────────────────────
    # 1. 검색 결과에서 카드 상세 URL 수집
    # ───────────────────────────────────────────────────────────────

    def get_card_links(self, search_url: str) -> list[str]:
        """
        검색 결과 페이지에서 모든 카드 상세 URL 수집.

        [문제] a.b_view 버튼의 href = 'javascript:;' 이므로
               href를 직접 읽는 방식으로는 링크 수집 불가.

        [해결] 두 가지 방법을 함께 사용:
          - 방법 A: 리스트 아이템의 카드 이미지 src에서 카드 ID 파싱
                    패턴: /card/{id}/card_img/...
          - 방법 B(백업): 첫 번째 b_view 클릭 후 SPA URL 변화로 패턴 확인

        방법 A가 가장 안정적이며 클릭 없이 모든 ID를 한 번에 수집 가능.
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(search_url, wait_until="networkidle", timeout=60000)

                # ── 더보기 버튼 끝까지 클릭 ─────────────────────────────
                self._click_until_all_loaded(page)

                # ── [방법 A] 이미지 URL에서 카드 ID 추출 ─────────────────
                links = self._extract_links_from_images(page)

                if not links:
                    # ── [방법 B 백업] b_view 클릭 후 URL 감지 ─────────────
                    print("[경고] 이미지 방식 실패 → b_view 클릭 방식으로 재시도")
                    links = self._extract_links_by_clicking(page)

                return links
            finally:
                browser.close()

    def _click_until_all_loaded(self, page: Page) -> None:
        """
        <a class="lst_more"> 버튼이 display:none 될 때까지 반복 클릭.
        검색 결과 전체를 로드하기 위함.
        """
        max_clicks = 50  # 무한루프 방지
        click_count = 0
        while click_count < max_clicks:
            more_btn = page.locator("a.lst_more")
            try:
                # 존재하고 visible 이어야 클릭
                more_btn.wait_for(state="visible", timeout=3000)
                style = more_btn.get_attribute("style") or ""
                if "display: none" in style or "display:none" in style:
                    break
                more_btn.click()
                page.wait_for_timeout(1500)
                click_count += 1
            except Exception:
                break  # 버튼이 없거나 timeout → 전부 로드된 것으로 간주

    def _extract_links_from_images(self, page: Page) -> list[str]:
        """
        카드 목록의 이미지 src에서 카드 ID를 추출해 상세 URL 구성.

        이미지 URL 패턴:
          https://d1c5n4ri2guedi.cloudfront.net/card/{id}/card_img/...
        상세 URL 패턴:
          https://www.card-gorilla.com/card/detail/{id}
        """
        img_locator = page.locator("ul.lst li div.card_img img")
        count = img_locator.count()
        if count == 0:
            return []

        seen_ids: set[str] = set()
        links: list[str] = []
        for i in range(count):
            src = img_locator.nth(i).get_attribute("src") or ""
            # /card/{숫자}/ 패턴 추출
            m = re.search(r"/card/(\d+)/", src)
            if m:
                card_id = m.group(1)
                if card_id not in seen_ids:
                    seen_ids.add(card_id)
                    links.append(f"{self.base_url}/card/detail/{card_id}")

        return links

    def _extract_links_by_clicking(self, page: Page) -> list[str]:
        """
        [백업] b_view 버튼을 하나씩 클릭 후 SPA URL 변화를 감지해 ID 수집.
        이미지 방식이 실패한 경우에만 사용. 속도 느림.
        """
        seen_ids: set[str] = set()
        links: list[str] = []

        items = page.locator("ul.lst li div.card_data")
        count = items.count()
        for i in range(count):
            try:
                btn = items.nth(i).locator("a.b_view")
                prev_url = page.url
                btn.click()
                # SPA 라우터가 URL을 /card/detail/{id} 로 바꿀 때까지 대기
                page.wait_for_function(
                    f"window.location.href !== '{prev_url}'",
                    timeout=5000
                )
                current_url = page.url
                m = re.search(r"/card/detail/(\d+)", current_url)
                if m:
                    card_id = m.group(1)
                    if card_id not in seen_ids:
                        seen_ids.add(card_id)
                        links.append(f"{self.base_url}/card/detail/{card_id}")
                # 목록 페이지로 돌아가기
                page.go_back()
                page.wait_for_timeout(1500)
            except Exception as e:
                print(f"  [경고] {i}번째 카드 클릭 방식 실패: {e}")
                continue

        return links

    # ───────────────────────────────────────────────────────────────
    # 2. 개별 카드 상세 정보 수집
    # ───────────────────────────────────────────────────────────────

    def scrap_card_detail(self, detail_url: str, card_type: str) -> dict:
        """
        카드 상세 페이지에서 필요한 정보 수집.

        수집 항목:
          - card_name     : <strong class="card"> 텍스트
          - brand         : <p class="brand"> 텍스트
          - card_type     : 파라미터로 전달 (credit / check)
          - img_url       : <div class="card_img"> 첫 번째 <img> src
          - meta_info     : <div class="bnf2"> 텍스트 (연회비/전월실적 등)
          - benefits      : <div class="lst bene_area"> 내 모든 dl 클릭 후 dd 텍스트
          - source_url    : '카드사 바로가기' 버튼 클릭 후 최종 이동 URL
          - detail_url    : 원본 상세 URL
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            context = browser.new_context(user_agent=USER_AGENT)
            page = context.new_page()
            try:
                page.goto(detail_url, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)

                # 1. 기본 정보
                card_name = self._safe_inner_text(page, "strong.card")
                brand_name = self._safe_inner_text(page, "p.brand")
                img_src = page.locator("div.card_img img").first.get_attribute("src") or ""

                # 2. 연회비/전월실적 메타 정보
                meta_info = self._safe_inner_text(page, "div.bnf2")

                # 3. 혜택 상세 (아코디언 전부 펼치기)
                benefits = self._collect_benefits(page)

                # 4. 카드사 바로가기 URL
                source_url = self._extract_redirect_url(page)

                return {
                    "card_name":  card_name,
                    "brand":      brand_name,
                    "card_type":  card_type,
                    "img_url":    img_src,
                    "meta_info":  meta_info,
                    "benefits":   benefits,
                    "source_url": source_url,
                    "detail_url": detail_url,
                }
            finally:
                browser.close()

    def _collect_benefits(self, page: Page) -> list[dict]:
        """
        <div class="lst bene_area"> 내 모든 <dl> 태그를 클릭해
        아코디언을 열고 dt/dd 텍스트를 수집.

        반환 형식:
          [{"category": "...", "description": "...", "detail": "..."}, ...]
        """
        benefits = []
        dl_locator = page.locator("div.lst.bene_area dl")
        count = dl_locator.count()

        for i in range(count):
            dl = dl_locator.nth(i)
            try:
                # dt 텍스트 (카테고리명 + 요약)
                dt = dl.locator("dt")
                category = self._safe_inner_text_locator(dt.locator("p.txt1"))
                description = self._safe_inner_text_locator(dt.locator("i"))

                # 아코디언 클릭 (dd가 없으면 클릭 후 생성됨)
                dt.click()
                page.wait_for_timeout(400)

                # dd 텍스트 수집
                detail = self._safe_inner_text_locator(dl.locator("dd"))

                benefits.append({
                    "category":    category,
                    "description": description,
                    "detail":      detail,
                })
            except Exception as e:
                benefits.append({
                    "category":    f"[수집실패 #{i}]",
                    "description": str(e),
                    "detail":      "",
                })

        return benefits

    def _extract_redirect_url(self, page: Page) -> str:
        """
        '카드사 바로가기' 버튼 클릭 후 최종 이동 URL 포착.

        버튼 구조:
          <div class="app_btn">
            <a href="javascript:" onclick="gtag(...)">카드사 바로가기</a>
          </div>

        href = 'javascript:' 이므로 정적 파싱 불가.
        Vue 컴포넌트가 런타임에 window.open() 또는 window.location으로 이동함.

        [전략 1 - 우선] window.open을 JS로 가로채서 URL 추출 (새 탭 차단 후 URL만 수집)
        [전략 2 - 폴백] 새 탭이 실제로 열리면 해당 탭 URL 감지
        [전략 3 - 폴백] 현재 탭 URL 변화 감지
        """
        btn = page.locator("div.app_btn a").first
        try:
            btn.wait_for(state="visible", timeout=3000)
        except Exception:
            return ""

        # ── 전략 1: window.open 가로채기 ──────────────────────────────
        # window.open을 덮어써서 실제 호출될 URL을 __interceptedUrl에 저장,
        # 새 탭은 열지 않음 → 현재 페이지 상태 유지
        page.evaluate("""
            window.__interceptedUrl = null;
            window.open = function(url) {
                window.__interceptedUrl = url;
                return null;
            };
            const _origAssign = window.location.assign.bind(window.location);
            window.location.assign = function(url) {
                window.__interceptedUrl = url;
            };
        """)

        btn.click()
        page.wait_for_timeout(1500)  # Vue 핸들러 실행 대기

        intercepted = page.evaluate("window.__interceptedUrl")
        if intercepted:
            return intercepted

        # ── 전략 2: 실제 새 탭이 열린 경우 ──────────────────────────
        try:
            with page.context.expect_page(timeout=4000) as new_page_info:
                btn.click()
            new_page = new_page_info.value
            new_page.wait_for_load_state("domcontentloaded", timeout=10000)
            final_url = new_page.url
            new_page.close()
            return final_url
        except Exception:
            pass

        # ── 전략 3: 현재 탭 URL 변화 감지 ───────────────────────────
        try:
            prev_url = page.url
            btn.click()
            page.wait_for_function(
                f"window.location.href !== '{prev_url}'",
                timeout=4000
            )
            return page.url
        except Exception:
            return ""

    # ───────────────────────────────────────────────────────────────
    # 3. 헬퍼
    # ───────────────────────────────────────────────────────────────

    @staticmethod
    def _safe_inner_text(page: Page, selector: str) -> str:
        try:
            el = page.locator(selector).first
            el.wait_for(state="visible", timeout=5000)
            return el.inner_text().strip()
        except Exception:
            return ""

    @staticmethod
    def _safe_inner_text_locator(locator) -> str:
        try:
            return locator.first.inner_text().strip()
        except Exception:
            return ""

    # ───────────────────────────────────────────────────────────────
    # 4. 전체 수집 (신용 + 체크)
    # ───────────────────────────────────────────────────────────────

    def scrap_all(self) -> list[dict]:
        """신용카드 + 체크카드 전체 수집"""
        results = []
        for card_type, url in SEARCH_URLS.items():
            print(f"\n[{card_type.upper()}] 링크 수집 중...")
            links = self.get_card_links(url)
            print(f"  → {len(links)}개 카드 링크 수집 완료")

            for i, link in enumerate(links, 1):
                print(f"  [{i}/{len(links)}] {link}")
                try:
                    detail = self.scrap_card_detail(link, card_type)
                    results.append(detail)
                except Exception as e:
                    print(f"    [오류] {e}")
                time.sleep(1)  # 요청 간격 조절

        return results


# ─── 테스트 실행부 ────────────────────────────────────────────────

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="카드고릴라 스크래퍼")
    parser.add_argument(
        "--mode",
        choices=["all", "credit", "check", "detail"],
        default="all",
        help="all: 신용+체크 전체 | credit: 신용카드만 | check: 체크카드만 | detail: 링크 수집만",
    )
    parser.add_argument("--sample", action="store_true", help="첫 번째 카드 1장만 상세 수집")
    args = parser.parse_args()

    scraper = CardGorillaScraper()

    # ── 수집 대상 URL 결정 ───────────────────────────────────────
    if args.mode == "credit":
        target_urls = {"credit": SEARCH_URLS["credit"]}
    elif args.mode == "check":
        target_urls = {"check": SEARCH_URLS["check"]}
    else:  # all / detail
        target_urls = SEARCH_URLS

    # ── 링크 수집 ────────────────────────────────────────────────
    all_links: dict[str, list[str]] = {}
    for card_type, url in target_urls.items():
        print("=" * 60)
        print(f"[링크 수집] {card_type.upper()} → {url}")
        print("=" * 60)
        links = scraper.get_card_links(url)
        all_links[card_type] = links
        print(f"수집된 카드 수: {len(links)}개")
        if links:
            print(f"  첫 번째: {links[0]}")
            print(f"  마지막:  {links[-1]}")

    if args.mode == "detail":
        # 링크 수집 결과만 출력하고 종료
        total = sum(len(v) for v in all_links.values())
        print(f"\n총 {total}개 링크 수집 완료 (상세 스크랩 생략)")
        exit(0)

    # ── 상세 스크랩 ──────────────────────────────────────────────
    results = []
    for card_type, links in all_links.items():
        if not links:
            print(f"[경고] {card_type}: 수집된 링크 없음, 건너뜀")
            continue

        targets = links[:1] if args.sample else links
        print()
        print("=" * 60)
        print(f"[상세 스크랩] {card_type.upper()} — {len(targets)}개")
        print("=" * 60)

        for i, link in enumerate(targets, 1):
            print(f"  [{i}/{len(targets)}] {link}")
            try:
                detail = scraper.scrap_card_detail(link, card_type=card_type)
                results.append(detail)
                print(f"    ✓ {detail['card_name']} ({detail['brand']})")
            except Exception as e:
                print(f"    [오류] {e}")
            time.sleep(1)

    # ── 결과 저장 ────────────────────────────────────────────────
    if results:
        out_path = "card_gorilla_result.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print(f"\n[완료] {len(results)}개 저장 → {out_path}")