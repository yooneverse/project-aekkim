"""
웨이브 공식 홈페이지에서 요금제 정보를 수집하는 크롤러

요금제: https://www.wavve.com/voucher/
이벤트: https://www.wavve.com/customer/event_list

※ sync_playwright는 greenlet 기반이라 컨텍스트 공유 불가 → 메서드마다 독립 인스턴스
"""
import logging

from playwright.sync_api import sync_playwright, Page

from base import OTTScraper


VOUCHER_URL = "https://www.wavve.com/voucher/"
BASE_URL    = "https://www.wavve.com"

logger = logging.getLogger(__name__)

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


class WavveScraper(OTTScraper):

    # nav.ticket-tab 의 li 텍스트와 파싱 타입 매핑
    TABS = [
        {"name": "웨이브 이용권",            "type": "wavve"},
        {"name": "웨이브 x 티빙 더블 이용권", "type": "double"},
        {"name": "제휴 이용권",              "type": "affiliate"},
    ]

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 로고
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_logo(self, page: Page) -> str:
        """헤더 <a class="logo"> 내 <img src> 를 절대 경로로 반환

        구조: <a class="logo logo"><img src="/static/ci-wavve-...svg" alt="Wavve"></a>
        상대 경로이므로 BASE_URL을 붙여 절대 경로로 변환한다.
        """
        img = page.query_selector("a.logo img[alt='Wavve']")
        if not img:
            raise NotImplementedError(
                "[wavve] scrap_logo() 미구현 — a.logo img[alt='Wavve'] 선택자로 로고를 찾을 수 없음, 선택자 확인 필요"
            )
        src = (img.get_attribute("src") or "").strip()
        if not src:
            raise NotImplementedError(
                "[wavve] scrap_logo() 미구현 — 로고 img src 속성이 비어 있음"
            )
        return src if src.startswith("http") else BASE_URL + src

    # ─── 탭별 요금제 파싱 ─────────────────────────────────────────────────

    def _parse_wavve_tab(self, page: Page, tab_name: str) -> list[dict]:
        """웨이브 이용권 탭 파싱 - table 구조

        thead의 th 마다 button.btn 이 기간(1개월/12개월)별로 존재.
        tbody tr 은 feature(행) × plan(열) 의 description 테이블.
        """
        results = []
        try:
            table = page.locator("table.voucher-table").first
            plans_meta = []

            for th in table.locator("thead tr th").all():
                plan_name_el = th.locator("span.product-name")
                if not plan_name_el.count():
                    continue
                plan_name = plan_name_el.inner_text().strip()

                # 각 button.btn 이 하나의 기간 요금
                # span.month > span:first → "1개월" or "12개월"
                # span.price > span:first → "￦7,900" 등
                prices = []
                for btn in th.locator("button.btn").all():
                    month_text = btn.locator("span.month > span").first.inner_text().strip()
                    price_text = btn.locator("span.price > span").first.inner_text().strip()
                    billing_cycle = "yearly" if "12" in month_text else "monthly"
                    prices.append({
                        "billing_cycle": billing_cycle,
                        "price": self._extract_price(price_text),
                    })

                plans_meta.append({"plan_name": plan_name, "prices": prices})

            # tbody: feature 행 × plan 열 → description 수집
            descriptions = {meta["plan_name"]: [] for meta in plans_meta}
            for row in table.locator("tbody tr").all():
                th_el = row.locator("th")
                feature_name = th_el.inner_text().strip() if th_el.count() else ""
                tds = row.locator("td").all()

                for idx, meta in enumerate(plans_meta):
                    if idx >= len(tds):
                        break
                    span_el = tds[idx].locator("span")
                    td_text = (
                        span_el.first.inner_text().strip()
                        if span_el.count()
                        else tds[idx].inner_text().strip()
                    )
                    if feature_name and td_text:
                        descriptions[meta["plan_name"]].append(f"{feature_name}: {td_text}")

            for meta in plans_meta:
                for price_info in meta["prices"]:
                    results.append({
                        "platform": "wavve",
                        "tab": tab_name,
                        "services": ["웨이브"],
                        "plan_name": meta["plan_name"],
                        "billing_cycle": price_info["billing_cycle"],
                        "price": price_info["price"],
                        "original_price": None,
                        "description": descriptions[meta["plan_name"]],
                    })

        except Exception as e:
            print(f"[ERROR] 웨이브 이용권 파싱 실패: {e}")

        return results

    def _parse_double_tab(self, page: Page, tab_name: str) -> list[dict]:
        """웨이브 x 티빙 더블 이용권 탭 파싱 - table 구조"""
        results = []
        try:
            table = page.locator("table.voucher-table").first
            plans_meta = []

            for th in table.locator("thead tr th").all():
                plan_name_el = th.locator("span.product-name")
                if not plan_name_el.count():
                    continue
                plan_name = plan_name_el.inner_text().strip()

                prices = []
                for btn in th.locator("button.btn").all():
                    # 더블 이용권 버튼에는 span.month 가 없으므로 count() 로 확인 후 처리
                    month_span = btn.locator("span.month > span")
                    month_text = month_span.first.inner_text().strip() if month_span.count() else ""
                    price_text = btn.locator("span.price > span").first.inner_text().strip()
                    billing_cycle = "yearly" if "12" in month_text else "monthly"
                    prices.append({
                        "billing_cycle": billing_cycle,
                        "price": self._extract_price(price_text),
                    })

                plans_meta.append({"plan_name": plan_name, "prices": prices})

            descriptions = {meta["plan_name"]: [] for meta in plans_meta}
            for row in table.locator("tbody tr").all():
                th_el = row.locator("th")
                feature_name = th_el.inner_text().strip() if th_el.count() else ""
                tds = row.locator("td").all()

                for idx, meta in enumerate(plans_meta):
                    if idx >= len(tds):
                        break
                    td = tds[idx]

                    # 더블 이용권은 em.b-box 구조가 있을 수 있음
                    b_box = td.locator("em.b-box")
                    if b_box.count():
                        parts = [
                            el.inner_text().strip()
                            for el in b_box.locator("i, span").all()
                            if el.inner_text().strip()
                        ]
                        td_text = " ".join(parts)
                    else:
                        span_el = td.locator("span")
                        td_text = (
                            span_el.first.inner_text().strip()
                            if span_el.count()
                            else td.inner_text().strip()
                        )

                    if feature_name and td_text:
                        descriptions[meta["plan_name"]].append(f"{feature_name}: {td_text}")

            for meta in plans_meta:
                for price_info in meta["prices"]:
                    results.append({
                        "platform": "wavve",
                        "tab": tab_name,
                        "services": ["웨이브", "티빙"],
                        "plan_name": meta["plan_name"],
                        "billing_cycle": price_info["billing_cycle"],
                        "price": price_info["price"],
                        "original_price": None,
                        "description": descriptions[meta["plan_name"]],
                    })

        except Exception as e:
            print(f"[ERROR] 더블 이용권 파싱 실패: {e}")

        return results

    def _parse_affiliate_tab(self, page: Page, tab_name: str) -> list[dict]:
        """제휴 이용권 탭 파싱 - ul > li 구조"""
        results = []
        try:
            for item in page.locator("ul.voucher-list li").all():
                try:
                    plan_name_el = item.locator("h3 span").first
                    plan_name = plan_name_el.inner_text().strip() if plan_name_el.count() else None

                    price_el = item.locator("div.price-area button").first
                    price = self._extract_price(price_el.inner_text()) if price_el.count() else None

                    # span.before-price 가 있으면 할인 전 원가
                    before_price_el = item.locator("div.price-area span.before-price")
                    original_price = (
                        self._extract_price(before_price_el.inner_text())
                        if before_price_el.count()
                        else None
                    )

                    description = []
                    for row in item.locator("tbody tr").all():
                        th_el = row.locator("th")
                        td_el = row.locator("td").first
                        feature = th_el.inner_text().strip() if th_el.count() else ""
                        value = td_el.inner_text().strip() if td_el.count() else ""
                        if feature and value:
                            description.append(f"{feature}: {value}")

                    if plan_name:
                        results.append({
                            "platform": "wavve",
                            "tab": tab_name,
                            "services": ["웨이브"],
                            "plan_name": plan_name,
                            "billing_cycle": "monthly",
                            "price": price,
                            "original_price": original_price,
                            "description": description,
                        })

                except Exception as e:
                    print(f"[ERROR] 제휴 이용권 항목 파싱 실패: {e}")
                    continue

        except Exception as e:
            print(f"[ERROR] 제휴 이용권 파싱 실패: {e}")

        return results

    # ─── DOM 파싱 ─────────────────────────────────────────────────────────

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """div.bt-noti-wrap 에서 섹션별 주의사항 수집

        구조:
            div.bt-noti-wrap
              div.bt-noti
                p.bt-noti-tit  → 섹션 제목
                p.dot-text (×N) → 항목

        Returns:
            {"서비스 이용 주의사항": [...], "이용권 구매 주의사항": [...], ...}
        """
        cautions: dict[str, list[str]] = {}
        try:
            noti_wrap = page.query_selector("div.bt-noti-wrap")
            if not noti_wrap:
                return cautions

            for section in noti_wrap.query_selector_all("div.bt-noti"):
                title_el = section.query_selector("p.bt-noti-tit")
                if not title_el:
                    continue
                title = title_el.inner_text().strip()
                items = [
                    p.inner_text().strip()
                    for p in section.query_selector_all("p.dot-text")
                    if p.inner_text().strip()
                ]
                if items:
                    cautions[title] = items

        except Exception as e:
            print(f"[ERROR] 유의사항 파싱 실패: {e}")

        return cautions

    def _parse_company_info(self, page: Page) -> list[str]:
        """footer div.footer-info-copyright 에서 사업자 정보 수집

        직계 자식 span / address 요소만 순회하여 중첩 span 중복 방지.

        Returns:
            ["콘텐츠웨이브 주식회사", "대표이사 서장호", ...]
        """
        info: list[str] = []
        try:
            info_div = page.query_selector("footer div.footer-info-copyright")
            if not info_div:
                return info

            for el in info_div.query_selector_all(":scope > span, :scope > address"):
                text = (el.inner_text() or "").strip()
                if text:
                    info.append(text)

        except Exception as e:
            print(f"[ERROR] 사업자 정보 파싱 실패: {e}")

        return info

    # ─── 유틸 ─────────────────────────────────────────────────────────────

    def _extract_price(self, text: str) -> int | None:
        """텍스트에서 숫자만 추출 (예: '￦7,900' → 7900)"""
        digits = "".join(filter(str.isdigit, text))
        return int(digits) if digits else None


if __name__ == "__main__":
    scraper = WavveScraper()

    print("=== 개별 메서드 테스트 ===\n")

    print("[scrap_logo]")
    logo = scraper.scrap_logo()
    print(logo, "\n")

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