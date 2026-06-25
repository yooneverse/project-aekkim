"""
네이버 멤버십 스크래퍼

수집 URL: https://nid.naver.com/membership/join

수집 항목:
  1. div#benefit > ul.membership_list > li 중 "디지털 콘텐츠" 텍스트 포함 li
     → 넷플릭스 포함 여부 감지 (없으면 프로모션 없음으로 처리)
  2. div#1stCoupon > img[src]
     → 웰컴 쿠폰 이미지 URL (summary에 쿠폰 내용 반영)
  3. div.action_box > a#joinButton 클릭 후 나타나는 창에서 월/연 가격 수집

scrap() 반환값이 card_detail 형식 → normalize_card_benefit()으로 정규화

수집 주기: 주 1회
"""
import logging
import re

from playwright.sync_api import sync_playwright, Page

from base import PromotionScraper


MEMBERSHIP_URL = "https://nid.naver.com/membership/join"

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)

logger = logging.getLogger(__name__)


class NaverMembershipScraper(PromotionScraper):
    """
    네이버 멤버십 스크래퍼

    scrap() 반환값이 card_detail 형식이므로
    scheduler에서 normalizer.normalize_card_benefit()으로 정규화.
    넷플릭스가 디지털 콘텐츠 혜택에 없으면 빈 리스트 반환.
    """

    def scrap(self) -> list[dict]:
        """
        네이버 멤버십 페이지에서 혜택·쿠폰·가격 수집

        Returns:
            넷플릭스 포함 시:
            [
                {
                    "card_name":  "네이버 멤버십",
                    "brand":      "네이버",
                    "card_type":  "membership",
                    "img_url":    str,        # 웰컴 쿠폰 이미지 URL
                    "meta_info":  str,        # 월/연간 가격 텍스트
                    "benefits":   list[dict], # 디지털 콘텐츠 혜택 텍스트 (OTT 키워드 추출용)
                    "source_url": str,
                    "detail_url": str,
                }
            ]
            넷플릭스 미포함 시: []
        """
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_context(user_agent=USER_AGENT).new_page()
            try:
                page.goto(MEMBERSHIP_URL, wait_until="domcontentloaded", timeout=60000)
                page.wait_for_timeout(2000)

                # ── 1. 디지털 콘텐츠 혜택 li 수집 ──────────────────────────
                digital_benefit = self._parse_digital_benefit(page)
                if not digital_benefit:
                    logger.info("[NaverMembershipScraper] 디지털 콘텐츠 혜택 없음 — 스킵")
                    return []

                # 넷플릭스 포함 여부 확인
                if "넷플릭스" not in digital_benefit.lower() and "netflix" not in digital_benefit.lower():
                    logger.info("[NaverMembershipScraper] 넷플릭스 미포함 — 프로모션 없음으로 처리")
                    return []

                # ── 2. 웰컴 쿠폰 이미지 URL 수집 ────────────────────────────
                coupon_img_url = self._parse_coupon_image(page)

                # ── 3. joinButton 클릭 후 월/연 가격 수집 ───────────────────
                monthly_price, yearly_price = self._parse_prices(page)

                # ── meta_info 조립 ────────────────────────────────────────
                meta_parts = []
                if monthly_price:
                    meta_parts.append(f"월간 {monthly_price:,}원")
                if yearly_price:
                    meta_parts.append(f"연간 {yearly_price:,}원")
                meta_info = " / ".join(meta_parts)

                # ── benefits 조립 (OTT 키워드 추출 + 쿠폰 내용 LLM 전달용) ──
                benefits = [
                    {
                        "category":    "디지털 콘텐츠",
                        "description": digital_benefit,
                        "detail":      "넷플릭스 광고형 스탠다드 요금제 (월 7,000원) 이용 가능",
                    }
                ]
                if coupon_img_url:
                    benefits.append({
                        "category":    "웰컴 쿠폰",
                        "description": f"웰컴 쿠폰 이미지: {coupon_img_url}",
                        "detail":      "",
                    })

                return [{
                    "card_name":  "네이버 멤버십",
                    "brand":      "네이버",
                    "card_type":  "membership",
                    "img_url":    coupon_img_url,
                    "meta_info":  meta_info,
                    "benefits":   benefits,
                    "source_url": MEMBERSHIP_URL,
                    "detail_url": MEMBERSHIP_URL,
                }]

            except Exception as e:
                logger.error("[NaverMembershipScraper] 수집 실패: %s", e)
                return []
            finally:
                browser.close()

    # ── Private 헬퍼 ──────────────────────────────────────────────────────

    def _parse_digital_benefit(self, page: Page) -> str:
        """
        div#benefit > ul.membership_list > li 중
        "디지털 콘텐츠" 텍스트가 포함된 li의 전체 텍스트 반환.

        Returns:
            str: 해당 li 텍스트 (예: "디지털 콘텐츠 매월 선택 가능\n넷플릭스, 스포티파이...")
                 없으면 ""
        """
        try:
            benefit_div = page.locator("div#benefit ul.membership_list li")
            count = benefit_div.count()
            for i in range(count):
                li = benefit_div.nth(i)
                text = li.inner_text().strip()
                if "디지털 콘텐츠" in text:
                    logger.info("[NaverMembershipScraper] 디지털 콘텐츠 li 발견: %s", text[:80])
                    return text
        except Exception as e:
            logger.warning("[NaverMembershipScraper] 디지털 콘텐츠 파싱 실패: %s", e)
        return ""

    def _parse_coupon_image(self, page: Page) -> str:
        """
        div#1stCoupon > img[src] URL 반환.

        Returns:
            str: 이미지 절대 URL, 없으면 ""
        """
        try:
            img = page.locator('div[id="1stCoupon"] img').first
            if img.count():
                src = img.get_attribute("src") or ""
                if src:
                    return src if src.startswith("http") else "https:" + src
        except Exception as e:
            logger.warning("[NaverMembershipScraper] 웰컴 쿠폰 이미지 파싱 실패: %s", e)
        return ""

    def _parse_prices(self, page: Page) -> tuple[int | None, int | None]:
        """
        월간·연간 가격 반환.

        네이버 멤버십 가격 (하드코딩):
            월간: 4,900원
            연간: 46,800원
        """
        return 4900, 46800


if __name__ == "__main__":
    import json
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    )
    scraper = NaverMembershipScraper()

    print("=== NaverMembershipScraper 테스트 ===\n")
    results = scraper.scrap()

    if not results:
        print("수집 결과 없음 (넷플릭스 미포함 또는 수집 실패)")
    else:
        print(f"수집된 항목 수: {len(results)}건")
        for i, item in enumerate(results, 1):
            print(f"\n[{i}] {item.get('brand', '')} {item.get('card_name', '')}")
            print(f"  meta_info : {item.get('meta_info', '(없음)')}")
            print(f"  img_url   : {item.get('img_url', '(없음)')}")

            benefits = item.get("benefits", [])
            for b in benefits:
                print(f"  [{b['category']}] {b['description'][:120]}")

            # OTT 키워드 감지 미리보기
            combined = " ".join(
                b.get("description", "") for b in benefits
            ).lower()
            detected = []
            for keywords, code in [
                (["넷플릭스", "netflix"],  "NETFLIX"),
                (["웨이브", "wavve"],      "WAVVE"),
                (["티빙", "tving"],        "TVING"),
                (["디즈니", "disney"],     "DISNEY_PLUS"),
                (["왓챠", "watcha"],       "WATCHA"),
                (["쿠팡플레이"],            "COUPANG_PLAY"),
            ]:
                if any(k.lower() in combined for k in keywords):
                    detected.append(code)
            print(f"  감지된 OTT: {detected if detected else '없음'}")

        print("\n[전체 JSON 출력]")
        display = [{
            **r,
            "benefits": [{**b, "description": b["description"][:150]} for b in r["benefits"]]
        } for r in results]
        print(json.dumps(display, ensure_ascii=False, indent=2))