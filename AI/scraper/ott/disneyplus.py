"""
디즈니플러스 공식 헬프 아티클에서 요금제 정보를 수집하는 크롤러
URL: https://help.disneyplus.com/ko/article/disneyplus-price

페이지 구조:
  - 개별 멤버십 (table.ql-table-blob): 스탠다드, 프리미엄, 추가 회원
  - 번들 멤버십 (header에 '번들' 포함 table): 디즈니+티빙, 디즈니+티빙+웨이브
  - cautions: 테이블 바깥 <p> 태그
  - company_info: div.supportFooter-regional ("|" 구분)


※ sync_playwright는 greenlet 기반이라 컨텍스트 공유 불가 → 메서드마다 독립 인스턴스
"""
from playwright.sync_api import sync_playwright, Page

from base import OTTScraper


PLAN_URL = "https://help.disneyplus.com/ko/article/disneyplus-price"
HOME_URL = "https://www.disneyplus.com/ko-kr"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)



class DisneyPlusScraper(OTTScraper):

    # 서비스명 키워드 → 플랫폼명 매핑
    SERVICE_MAP = {
        "디즈니": "디즈니+",
        "티빙": "티빙",
        "웨이브": "웨이브",
    }

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
            "[disneyplus] scrap_logo() 미구현 — 헤더 로고 선택자 확인 후 구현 필요"
        )

    def scrap_plans(self) -> list[dict]:
        """
        구독 요금제 전체 목록 수집

        Returns:
            [
                {
                    "platform":       str,        # "disneyplus"
                    "tab":            None,        # 디즈니+는 탭 구분 없음
                    "services":       list[str],   # ["디즈니+"] 또는 번들 서비스 목록
                    "plan_name":      str,
                    "billing_cycle":  str,         # "monthly" | "yearly"
                    "price":          int,
                    "original_price": None,
                    "description":    list[str],
                },
                ...
            ]
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT, locale="ko-KR").new_page()
            try:
                page.goto(PLAN_URL, wait_until="load", timeout=60000)
                page.wait_for_selector("table.ql-table-blob", timeout=30000)
                plans = self._parse_individual_plans(page)
                plans += self._parse_bundle_plans(page)
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
            page = browser.new_context(user_agent=USER_AGENT, locale="ko-KR").new_page()
            try:
                page.goto(PLAN_URL, wait_until="load", timeout=60000)
                page.wait_for_selector("table.ql-table-blob", timeout=30000)
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
            page = browser.new_context(user_agent=USER_AGENT, locale="ko-KR").new_page()
            try:
                page.goto(PLAN_URL, wait_until="load", timeout=60000)
                # supportFooter-regional은 테이블 없이도 렌더되므로 해당 셀렉터 직접 대기
                page.wait_for_selector("div.supportFooter-regional", timeout=30000)
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
            page = browser.new_context(user_agent=USER_AGENT, locale="ko-KR").new_page()
            try:
                page.goto(PLAN_URL, wait_until="load", timeout=60000)
                page.wait_for_selector("table.ql-table-blob", timeout=30000)

                plans = self._parse_individual_plans(page)
                plans += self._parse_bundle_plans(page)

                return {
                    "logo":         "",   # scrap_logo() NotImplementedError — 선택자 확인 필요
                    "plans":        plans,
                    "cautions":     self._parse_cautions(page),
                    "company_info": self._parse_company_info(page),
                }
            finally:
                browser.close()

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 프로모션 감지
    # ═══════════════════════════════════════════════════════════════════════

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 요금제 파싱
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_individual_plans(self, page: Page) -> list[dict]:
        """개별 멤버십 파싱 (table.ql-table-blob)

        3열 구조:
            td[0]: 플랜명
            td[1]: ul li → description
            td[2]: <p> 텍스트 → '월'/'연' 포함 행이 가격
        헤더 행은 가격 열에 '월'/'연'이 없어 자동 스킵.
        """
        results = []
        try:
            table = page.locator("table.ql-table-blob").first
            if not table.count():
                print("[WARN] 개별 요금제 테이블을 찾을 수 없음")
                return results

            for row in table.locator("tbody tr").all():
                tds = row.locator("td").all()
                if len(tds) < 3:
                    continue

                plan_name = tds[0].inner_text().strip()
                if not plan_name:
                    continue

                description = [
                    li.inner_text().strip()
                    for li in tds[1].locator("li").all()
                    if li.inner_text().strip()
                ]

                prices = self._extract_prices_from_td(tds[2])
                for billing_cycle, price in prices:
                    results.append({
                        "platform":       "disneyplus",
                        "tab":            None,
                        "services":       ["디즈니+"],
                        "plan_name":      plan_name,
                        "billing_cycle":  billing_cycle,
                        "price":          price,
                        "original_price": None,
                        "description":    description,
                    })

        except Exception as e:
            print(f"[ERROR] 개별 요금제 파싱 실패: {e}")

        return results

    def _parse_bundle_plans(self, page: Page) -> list[dict]:
        """번들 멤버십 파싱 (첫 번째 행에 '번들' 텍스트 있는 table)

        3열 구조:
            td[0]: 번들명
            td[1]: ul li → 구성 서비스 목록 (services 추출에도 활용)
            td[2]: <p> 텍스트 → 가격
        헤더 행은 가격 열에 '월'/'연'이 없어 자동 스킵.
        """
        results = []
        try:
            bundle_table = None
            for table in page.locator("table").all():
                first_row = table.locator("tbody tr").first
                if first_row.count() and "번들" in first_row.inner_text():
                    bundle_table = table
                    break

            if not bundle_table:
                print("[WARN] 번들 요금제 테이블을 찾을 수 없음")
                return results

            for row in bundle_table.locator("tbody tr").all():
                tds = row.locator("td").all()
                if len(tds) < 3:
                    continue

                plan_name = tds[0].inner_text().strip()
                if not plan_name:
                    continue

                service_items = [
                    li.inner_text().strip()
                    for li in tds[1].locator("li").all()
                    if li.inner_text().strip()
                ]
                services = self._extract_services(service_items)
                description = service_items

                prices = self._extract_prices_from_td(tds[2])
                for billing_cycle, price in prices:
                    results.append({
                        "platform":       "disneyplus",
                        "tab":            None,
                        "services":       services,
                        "plan_name":      plan_name,
                        "billing_cycle":  billing_cycle,
                        "price":          price,
                        "original_price": None,
                        "description":    description,
                    })

        except Exception as e:
            print(f"[ERROR] 번들 요금제 파싱 실패: {e}")

        return results

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 주의사항 / 사업자 정보 파싱
    # ═══════════════════════════════════════════════════════════════════════

    def _parse_cautions(self, page: Page) -> dict[str, list[str]]:
        """아티클 본문의 <p> 태그에서 주의사항 수집

        '*' 로 시작하는 주의사항 텍스트만 필터링.

        Returns:
            {"이용 안내": [...]}
        """
        cautions: dict[str, list[str]] = {}
        try:
            texts: list[str] = page.locator("div.article-content p").evaluate_all("""
                (els) => els
                    .filter(p => !p.closest('table'))
                    .map(p => p.innerText.trim().replace(/\u00a0/g, '').trim())
                    .filter(t => t.includes('*'))
            """)
            if texts:
                cautions["이용 안내"] = texts

        except Exception as e:
            print(f"[ERROR] 주의사항 파싱 실패: {e}")

        return cautions

    def _parse_company_info(self, page: Page) -> list[str]:
        """div.supportFooter-regional 에서 사업자 정보 수집

        텍스트를 "|" 구분자로 분리하여 반환.

        Returns:
            ["월트디즈니컴퍼니코리아 유한책임회사", "대표자: 김소연", ...]
        """
        info: list[str] = []
        try:
            regional_el = page.query_selector("div.supportFooter-regional")
            if not regional_el:
                return info

            text = (regional_el.inner_text() or "").strip()
            if text:
                info = [part.strip() for part in text.split("|") if part.strip()]

        except Exception as e:
            print(f"[ERROR] 사업자 정보 파싱 실패: {e}")

        return info

    # ═══════════════════════════════════════════════════════════════════════
    # Private 헬퍼 — 유틸
    # ═══════════════════════════════════════════════════════════════════════

    def _extract_prices_from_td(self, td) -> list[tuple[str, int | None]]:
        """가격 td 에서 (billing_cycle, price) 목록 추출

        td 내 <p> 텍스트 중 '월' 포함 → monthly, '연' 포함 → yearly.
        """
        prices = []
        for p_el in td.locator("p").all():
            text = (p_el.inner_text() or "").strip().replace("\xa0", "")
            if not text:
                continue
            if "월" in text:
                prices.append(("monthly", self._extract_price(text)))
            elif "연" in text:
                prices.append(("yearly", self._extract_price(text)))
        return prices

    def _extract_services(self, service_items: list[str]) -> list[str]:
        """서비스 텍스트 목록에서 플랫폼 이름 추출

        SERVICE_MAP 키워드 순서대로 탐색하여 중복 없이 반환.
        """
        services = []
        for item in service_items:
            for keyword, name in self.SERVICE_MAP.items():
                if keyword in item and name not in services:
                    services.append(name)
        return services or ["디즈니+"]

    def _extract_price(self, text: str) -> int | None:
        """텍스트에서 숫자만 추출 (예: '월 9,900원' → 9900)"""
        digits = "".join(filter(str.isdigit, text))
        return int(digits) if digits else None


if __name__ == "__main__":
    scraper = DisneyPlusScraper()

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