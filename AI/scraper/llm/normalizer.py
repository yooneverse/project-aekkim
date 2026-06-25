"""
LLM 정규화 모듈

스크래퍼 raw 출력을 SERVICE_CATALOG_CRAWL / PROMOTION_CRAWL JSON으로 변환한다.
모델: claude-3-5-haiku-latest (SSAFY GMS API)

호출 단위 규칙:
  - normalize_plans():      플랫폼 1개 단위로 호출 (전체 OTT 합산 금지)
  - normalize_promotions(): 공지/프로모션 1건 단위로 호출 (여러 건 묶기 금지)

번들 처리:
  - normalize_plans() 결과에서 services 2개 이상 → PROMOTION_CRAWL(BUNDLE)
  - services 1개 → SERVICE_CATALOG_CRAWL
  - 반환값이 list[dict]인 이유: 한 플랫폼에 단일 요금제 + 번들이 섞일 수 있음
"""
import json
import logging
import os
from datetime import datetime, timezone, timedelta
from pathlib import Path

# import anthropic
import google.generativeai as genai
from google.generativeai.types import RequestOptions
from pydantic import ValidationError

from llm.schemas import ServiceCatalogCrawlSchema, PromotionCrawlSchema, PromotionSchema
from base import SERVICE_LOGO_URLS, SERVICE_CANCEL_URLS
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

PROMPTS_DIR = Path(__file__).parent / "prompts"
KST         = timezone(timedelta(hours=9))

# 실패한 raw 데이터를 저장하는 Dead Letter Queue 디렉토리
DLQ_DIR = Path(__file__).parent.parent / "dlq"


def _load_prompt(filename: str) -> str:
    """llm/prompts/ 폴더에서 시스템 프롬프트 텍스트 로드"""
    path = PROMPTS_DIR / filename
    if path.exists():
        return path.read_text(encoding="utf-8")

    lower_name = filename.lower()
    for candidate in PROMPTS_DIR.iterdir():
        if candidate.is_file() and candidate.name.lower() == lower_name:
            return candidate.read_text(encoding="utf-8")

    return path.read_text(encoding="utf-8")


class LLMNormalizer:
    """
    스크래퍼 raw 출력을 Gemini 2.0 Flash로 정규화
    """

    MODEL    = "gemini-2.0-flash"
    BASE_URL = os.getenv(
        "GMS_API_ENDPOINT",
        "api-proxy.example.com/gmsapi/generativelanguage.googleapis.com",
    )

    # __init__ 변경
    def __init__(self):
        genai.configure(
            api_key=os.environ["GMS_KEY"],
            client_options={"api_endpoint": self.BASE_URL},
            transport="rest",
        )
        self._plans_single_prompt      = _load_prompt("plans_single.txt")
        self._plans_bundle_prompt      = _load_prompt("plans_bundle.txt")
        self._promotions_prompt        = _load_prompt("promotions.txt")
        self._tving_promotion_prompt   = _load_prompt("tving_promotion.txt")
        logger.info("[LLMNormalizer] Gemini 2.0 Flash 초기화 완료 (GMS Proxy)")

    # _call_llm 변경
    def _call_llm(self, system_prompt: str, user_content: str) -> str:
        model = genai.GenerativeModel(
            model_name=self.MODEL,
            system_instruction=system_prompt,
        )
        response = model.generate_content(
            user_content,
            generation_config=genai.GenerationConfig(
                temperature=0,
                response_mime_type="application/json",
            ),
        )
        return response.text.strip()

    # ── 공개 메서드 ────────────────────────────────────────────────────────

    def normalize_plans(self, raw_plans: list[dict], service_info: dict | None = None) -> list[dict]:
        """
        스크래퍼 raw_plans → SERVICE_CATALOG_CRAWL / PROMOTION_CRAWL(BUNDLE) JSON

        번들/단일 분리는 Python에서 먼저 수행한 뒤 LLM에 각각 전달한다.
        LLM 모델이 작아 복잡한 분기 지시를 프롬프트만으로 믿기 어렵기 때문.

            services 길이 >= 2 → 번들 → _normalize_bundle() → PROMOTION_CRAWL
            services 길이 == 1 → 단일 → _normalize_single() → SERVICE_CATALOG_CRAWL

        services[] (회사 정보)는 LLM이 채우지 않는다.
        호출자(scheduler)가 scrap_company_info()로 수집한 정보를 service_info로 전달하면
        Python이 직접 조립해 LLM 결과에 주입한다.

        YEARLY 월 환산도 Python에서 처리한다:
            monthlyPrice = rawPrice // 12

        Args:
            raw_plans:    플랫폼 1개 분량의 scrap_plans() 반환값
            service_info: 선택. scrap_company_info()로 수집한 회사 메타 정보.
                {
                    "service_code": str,          # 예: "TVING"
                    "service_name": str,          # 예: "티빙"
                    "phone":        str | None,
                }
                전달되면 SERVICE_CATALOG_CRAWL의 services[]를 Python이 직접 조립.
                None이면 LLM 출력의 services[]를 그대로 사용.

        Returns:
            list[dict]: jobType별로 분리된 결과 목록
                - 단일 요금제만 있으면: [SERVICE_CATALOG_CRAWL]
                - 번들만 있으면: [PROMOTION_CRAWL]
                - 둘 다 있으면: [SERVICE_CATALOG_CRAWL, PROMOTION_CRAWL]
        """
        # ── Python이 번들 / 단일 분리 ──────────────────────────────────────
        single_plans = [r for r in raw_plans if len(r.get("services", [])) < 2]
        bundle_plans = [r for r in raw_plans if len(r.get("services", [])) >= 2]

        logger.info(
            "[LLMNormalizer] 분리 결과 — 단일 %d건 / 번들 %d건",
            len(single_plans), len(bundle_plans),
        )

        results: list[dict] = []

        if single_plans:
            result = self._normalize_single(single_plans, service_info)
            if result:
                results.append(result)

        if bundle_plans:
            result = self._normalize_bundle(bundle_plans)
            if result:
                results.append(result)

        return results if results else [self._empty_service_catalog()]

    def _normalize_single(self, single_plans: list[dict], service_info: dict | None = None) -> dict | None:
        """
        단일 서비스 요금제 목록을 LLM으로 정규화 → SERVICE_CATALOG_CRAWL 반환

        LLM은 servicePlans[]만 채운다.
        services[]는 service_info가 전달된 경우 Python이 직접 조립해 덮어쓴다.

        Args:
            single_plans: services 길이가 1인 raw_plans 항목 목록
            service_info: normalize_plans()에서 전달받은 회사 메타 정보 (optional)

        Returns:
            SERVICE_CATALOG_CRAWL dict, 실패 시 None
        """
        from llm.schemas import ServicePlanSchema

        user_content = json.dumps(single_plans, ensure_ascii=False)
        raw_output   = self._call_llm(self._plans_single_prompt, user_content)

        try:
            data = json.loads(self._strip_markdown(raw_output))
        except json.JSONDecodeError as e:
            logger.error("[LLMNormalizer] _normalize_single JSON 파싱 실패: %s\n출력: %s", e, raw_output)
            self._save_dlq("normalize_single", single_plans, raw_output)
            return None

        # LLM이 배열로 감싸 반환하는 경우 첫 번째 SERVICE_CATALOG_CRAWL 항목 사용
        if isinstance(data, list):
            candidates = [d for d in data if d.get("jobType") == "SERVICE_CATALOG_CRAWL"]
            data = candidates[0] if candidates else (data[0] if data else {})

        # ── services[] 조립: service_info 있으면 Python이 직접, 없으면 LLM 결과 사용 ──
        if service_info:
            service_code = service_info.get("service_code", "")
            data["services"] = [{
                "code":                  service_code,
                "name":                  service_info.get("service_name", service_code),
                "category":              "OTT",
                "logoUrl":               SERVICE_LOGO_URLS.get(service_code),
                "cancelGuideUrl":        SERVICE_CANCEL_URLS.get(service_code),
                "customerServicePhone":  service_info.get("phone"),
                "contactEmail":          service_info.get("email"),
            }]
            logger.info("[LLMNormalizer] services[] Python 직접 조립: %s", service_code)

        # collectedAt은 항상 Python 현재 시각으로 덮어씀 (LLM이 임의 과거 시간 넣는 문제 방지)
        data["collectedAt"] = self._now_kst()

        # YEARLY 월 환산 (Python 처리) — rawPrice 수식/문자열도 보정
        for item in data.get("servicePlans", []):
            raw = item.get("rawPrice")
            if raw is not None:
                item["rawPrice"] = self._sanitize_price(raw)
            self._calc_monthly_price(item)

        # 항목별 Pydantic 검증
        valid_plans = []
        for item in data.get("servicePlans", []):
            try:
                ServicePlanSchema(**item)
                valid_plans.append(item)
            except ValidationError as e:
                logger.warning("[LLMNormalizer] servicePlan 검증 실패, 스킵: %s | 항목: %s", e, item)
                self._save_dlq("servicePlan_validation", item, str(e))
        data["servicePlans"] = valid_plans

        try:
            ServiceCatalogCrawlSchema(**data)
        except ValidationError as e:
            logger.error("[LLMNormalizer] SERVICE_CATALOG_CRAWL 구조 검증 실패: %s", e)

        return data

    def _normalize_bundle(self, bundle_plans: list[dict]) -> dict | None:
        """
        번들 요금제 목록을 LLM으로 정규화 → PROMOTION_CRAWL(BUNDLE) 반환

        Args:
            bundle_plans: services 길이가 2 이상인 raw_plans 항목 목록

        Returns:
            PROMOTION_CRAWL dict, 실패 시 None
        """
        user_content = json.dumps(bundle_plans, ensure_ascii=False)
        raw_output   = self._call_llm(self._plans_bundle_prompt, user_content)

        try:
            data = json.loads(self._strip_markdown(raw_output))
        except json.JSONDecodeError as e:
            logger.error("[LLMNormalizer] _normalize_bundle JSON 파싱 실패: %s\n출력: %s", e, raw_output)
            self._save_dlq("normalize_bundle", bundle_plans, raw_output)
            return None

        # LLM이 배열로 감싸 반환하는 경우 첫 번째 PROMOTION_CRAWL 항목 사용
        if isinstance(data, list):
            candidates = [d for d in data if d.get("jobType") == "PROMOTION_CRAWL"]
            data = candidates[0] if candidates else (data[0] if data else {})

        # 가격 문자열·서비스코드 한글명 후처리 보정 (Pydantic 검증 전)
        data["promotions"] = self._sanitize_bundle_promotions(data.get("promotions", []))

        # collectedAt, startsAt, endsAt Python 강제 세팅
        # 상시 번들은 기간 개념이 없으므로: startsAt=수집시각, endsAt=2099-12-31
        now_kst    = self._now_kst()
        far_future = "2099-12-31T23:59:59+09:00"
        data["collectedAt"] = now_kst
        for item in data.get("promotions", []):
            item["startsAt"] = now_kst
            item["endsAt"]   = far_future

        # 항목별 Pydantic 검증
        valid_promotions = []
        for item in data.get("promotions", []):
            try:
                PromotionSchema(**item)
                valid_promotions.append(item)
            except ValidationError as e:
                logger.warning("[LLMNormalizer] bundle promotion 검증 실패, 스킵: %s | 항목: %s", e, item)
                self._save_dlq("bundle_promotion_validation", item, str(e))
        data["promotions"] = valid_promotions

        try:
            PromotionCrawlSchema(**data)
        except ValidationError as e:
            logger.error("[LLMNormalizer] PROMOTION_CRAWL 구조 검증 실패: %s", e)

        return data

    def normalize_promotions(self, raw_notice: dict) -> dict:
        """
        스크래퍼 raw_notice 1건 → PROMOTION_CRAWL JSON

        Args:
            raw_notice: scrap_promotions() 반환 리스트의 항목 1건

        Returns:
            PROMOTION_CRAWL 형식 dict
            검증 실패한 promotion 항목은 스킵되고 로그 출력
        """
        user_content = json.dumps(raw_notice, ensure_ascii=False)
        raw_output   = self._call_llm(self._promotions_prompt, user_content)

        try:
            data = json.loads(self._strip_markdown(raw_output))
        except json.JSONDecodeError as e:
            logger.error("[LLMNormalizer] normalize_promotions JSON 파싱 실패: %s\n출력: %s", e, raw_output)
            self._save_dlq("normalize_promotions", raw_notice, raw_output)
            return self._empty_promotion_crawl()

        valid_promotions = []
        for item in data.get("promotions", []):
            try:
                PromotionSchema(**item)
                valid_promotions.append(item)
            except ValidationError as e:
                logger.warning("[LLMNormalizer] promotion 검증 실패, 스킵: %s | 항목: %s", e, item)
                self._save_dlq("promotion_validation", item, str(e))

        data["promotions"] = valid_promotions

        try:
            PromotionCrawlSchema(**data)
        except ValidationError as e:
            logger.error("[LLMNormalizer] PROMOTION_CRAWL 구조 검증 실패: %s", e)

        return data

    def normalize_tving_promotion(self, raw_notice: dict) -> dict:
        """
        티빙 공지사항 1건 → PROMOTION_CRAWL JSON

        LLM이 공지 내용에 할인/프로모션이 있으면 PROMO로 정형화.
        관련 내용이 없으면 promotions: [] 로 반환 (저장 불필요).

        기존 promotions.txt 프롬프트에 필터링 지시 추가.

        Args:
            raw_notice: TvingPromotionScraper.scrap() 반환 항목 1건
                {"platform": "tving", "title": str, "date": str, "content": str}

        Returns:
            PROMOTION_CRAWL dict (promotions 빈 리스트 가능)
        """
        user_content = json.dumps(raw_notice, ensure_ascii=False)
        raw_output   = self._call_llm(self._tving_promotion_prompt, user_content)

        try:
            data = json.loads(self._strip_markdown(raw_output))
        except json.JSONDecodeError as e:
            logger.error("[LLMNormalizer] normalize_tving_promotion JSON 파싱 실패: %s\n출력: %s", e, raw_output)
            self._save_dlq("normalize_tving_promotion", raw_notice, raw_output)
            return self._empty_promotion_crawl()

        if isinstance(data, list):
            candidates = [d for d in data if d.get("jobType") == "PROMOTION_CRAWL"]
            data = candidates[0] if candidates else (data[0] if data else {})

        # promotions가 비어있으면 그대로 반환 (저장 불필요 신호)
        if not data.get("promotions"):
            data["jobType"]     = "PROMOTION_CRAWL"
            data["collectedAt"] = self._now_kst()
            data["promotions"]  = []
            return data

        data["collectedAt"] = self._now_kst()

        valid_promotions = []
        for item in data.get("promotions", []):
            try:
                PromotionSchema(**item)
                valid_promotions.append(item)
            except ValidationError as e:
                logger.warning("[LLMNormalizer] tving promotion 검증 실패, 스킵: %s | 항목: %s", e, item)
                self._save_dlq("tving_promotion_validation", item, str(e))
        data["promotions"] = valid_promotions

        return data

    def normalize_wavve_promotion(self, raw_notice: dict) -> dict:
        """
        웨이브 이벤트 1건 → PROMOTION_CRAWL JSON (LLM 없이 Python 직접 조립)

        이벤트 content가 "image_url:..." 형태라 LLM이 의미 있는 내용 추출 불가.
        title/date 기반으로 Python이 직접 조립.

        날짜 파싱:
            date 필드: "2026-03-12 ~ 2026-04-30" 형태 → startsAt / endsAt 변환
            파싱 실패 시 startsAt=수집시각, endsAt=2099-12-31(상시)

        Args:
            raw_notice: WavvePromotionScraper.scrap() 반환 항목 1건
                {"platform": "wavve", "title": str, "date": str, "content": str}

        Returns:
            PROMOTION_CRAWL dict
        """
        import re as _re
        from datetime import datetime as _dt

        now_kst    = self._now_kst()
        far_future = "2099-12-31T23:59:59+09:00"

        title   = raw_notice.get("title", "웨이브 이벤트")
        date    = raw_notice.get("date", "")
        content = raw_notice.get("content", "")

        # ── 날짜 파싱: "2026-03-12 ~ 2026-04-30" ─────────────────────────
        starts_at = now_kst
        ends_at   = far_future

        date_match = _re.search(
            r"(\d{4}-\d{2}-\d{2})\s*~\s*(\d{4}-\d{2}-\d{2})", date
        )
        if date_match:
            try:
                starts_at = date_match.group(1) + "T00:00:00+09:00"
                ends_at   = date_match.group(2) + "T23:59:59+09:00"
            except Exception:
                pass

        # ── sourceUrl: content에서 이미지 URL 추출 ────────────────────────
        source_url = None
        img_match = _re.search(r"image_url:(.+)", content)
        if img_match:
            first_url = img_match.group(1).split(",")[0].strip()
            source_url = first_url if first_url else None

        promotion = {
            "promotionType": "PROMO",
            "title":         title,
            "summary":       None,
            "originalPrice": None,
            "discountPrice": None,
            "startsAt":      starts_at,
            "endsAt":        ends_at,
            "sourceUrl":     source_url,
            "services":      [{"serviceCode": "WAVVE"}],
        }

        try:
            PromotionSchema(**promotion)
        except ValidationError as e:
            logger.error("[LLMNormalizer] wavve promotion 검증 실패: %s", e)

        result = {
            "jobType":     "PROMOTION_CRAWL",
            "collectedAt": now_kst,
            "promotions":  [promotion],
        }

        logger.info("[LLMNormalizer] wavve promotion 조립 완료 — %s", title)
        return result

    def normalize_naver_membership(self, card_detail: dict) -> dict:
        """
        네이버 멤버십 card_detail → PROMOTION_CRAWL(CARD_BENEFIT) JSON

        normalize_card_benefit() 결과에 웰컴 쿠폰 summary를 Python으로 추가.

        웰컴 쿠폰 감지:
            benefits 중 category="웰컴 쿠폰" 항목이 있으면 summary에 쿠폰 내용 추가.
            없으면 summary = meta_info 텍스트.

        Args:
            card_detail: NaverMembershipScraper.scrap() 반환 항목 1건

        Returns:
            PROMOTION_CRAWL(CARD_BENEFIT) dict
        """
        # 기본 정형화는 normalize_card_benefit() 활용
        result = self.normalize_card_benefit(card_detail)

        if not result.get("promotions"):
            return result

        promotion = result["promotions"][0]

        # ── 웰컴 쿠폰 감지 → summary 보강 ─────────────────────────────────
        benefits   = card_detail.get("benefits", [])
        coupon_benefit = next(
            (b for b in benefits if b.get("category") == "웰컴 쿠폰"),
            None
        )
        meta_info = card_detail.get("meta_info", "")

        if coupon_benefit:
            # 쿠폰 이미지 URL에서 쿠폰 금액 추출 (description에 포함된 경우)
            coupon_desc = coupon_benefit.get("description", "")
            import re as _re2
            amount_match = _re2.search(r"([\d,]+)원", coupon_desc)
            coupon_text = f"{amount_match.group(1)}원 웰컴 쿠폰 제공." if amount_match else "웰컴 쿠폰 제공."

            base_summary = (
                f"네이버 멤버십 {meta_info}."
                if meta_info else "네이버 멤버십."
            )
            promotion["summary"] = f"{base_summary} 신규 가입 시 {coupon_text}"
        else:
            promotion["summary"] = (
                f"네이버 멤버십 {meta_info}." if meta_info else None
            )

        logger.info("[LLMNormalizer] naver membership 조립 완료 — summary: %s", promotion.get("summary", "")[:80])
        return result

    def normalize_card_benefit(self, card_detail: dict) -> dict:
        """
        scrap_card_detail() 결과 1건 → PROMOTION_CRAWL(CARD_BENEFIT) JSON

        LLM 불필요 — 모든 필드가 규칙 기반으로 결정되므로 Python만으로 조립.

        Args:
            card_detail: scrap_card_detail() 반환 dict
                {
                    "card_name":  str,        # 카드명
                    "brand":      str,        # 카드사명
                    "card_type":  str,        # credit / check
                    "img_url":    str,        # 카드 이미지 URL
                    "meta_info":  str,        # 연회비/전월실적 텍스트 → summary
                    "benefits":   list[dict], # 혜택 목록 → OTT 서비스 추출
                    "source_url": str,        # 카드사 바로가기 URL (없으면 "")
                    "detail_url": str,        # 카드고릴라 상세 URL (fallback)
                }

        Returns:
            PROMOTION_CRAWL 형식 dict
        """
        now_kst    = self._now_kst()
        far_future = "2099-12-31T23:59:59+09:00"

        title      = f"{card_detail.get('brand', '')} {card_detail.get('card_name', '')}".strip()
        source_url = card_detail.get("source_url") or card_detail.get("detail_url", "")
        services   = self._extract_ott_services(card_detail.get("benefits", []))

        promotion = {
            "promotionType": "CARD_BENEFIT",
            "title":         title,
            "summary":       card_detail.get("meta_info", ""),
            "originalPrice": None,
            "discountPrice": None,
            "startsAt":      now_kst,
            "endsAt":        far_future,
            "sourceUrl":     source_url,
            "imgUrl":        card_detail.get("img_url", ""),
            "services":      services,
        }

        result = {
            "jobType":     "PROMOTION_CRAWL",
            "collectedAt": now_kst,
            "promotions":  [promotion],
        }

        try:
            PromotionCrawlSchema(**result)
        except ValidationError as e:
            logger.error("[LLMNormalizer] CARD_BENEFIT 구조 검증 실패: %s", e)

        logger.info(
            "[LLMNormalizer] CARD_BENEFIT 조립 완료 — %s | OTT %d개",
            title, len(services),
        )
        return result

    # OTT 키워드 → 서비스 코드 매핑 (텍스트에서 키워드 감지용)
    _OTT_KEYWORD_MAP: list[tuple[list[str], str]] = [
        (["넷플릭스", "netflix"],                                              "NETFLIX"),
        (["웨이브", "wavve"],                                                  "WAVVE"),
        (["왓챠", "watcha"],                                                   "WATCHA"),
        (["디즈니플러스", "디즈니+", "disney+", "disneyplus", "disney plus"], "DISNEY_PLUS"),
        (["티빙", "tving"],                                                    "TVING"),
        (["쿠팡플레이", "쿠팡 플레이", "coupang play", "coupangplay"],         "COUPANG_PLAY"),
    ]

    @classmethod
    def _extract_ott_services(cls, benefits: list[dict]) -> list[dict]:
        """
        benefits 텍스트에서 OTT 서비스 키워드를 감지해 services 목록 반환.

        benefits 각 항목의 category / description / detail 필드를 모두 합쳐
        소문자로 변환 후 키워드 매칭. 중복 serviceCode는 한 번만 포함.

        Args:
            benefits: _collect_benefits() 반환값
                [{"category": str, "description": str, "detail": str}, ...]

        Returns:
            [{"serviceCode": "NETFLIX"}, ...] 형태, 감지된 OTT 없으면 []
        """
        combined = " ".join(
            " ".join([
                b.get("category", ""),
                b.get("description", ""),
                b.get("detail", ""),
            ])
            for b in benefits
        ).lower()

        seen: set[str] = set()
        services: list[dict] = []

        for keywords, code in cls._OTT_KEYWORD_MAP:
            if code in seen:
                continue
            if any(kw.lower() in combined for kw in keywords):
                seen.add(code)
                services.append({"serviceCode": code})

        return services

    # ── Private 헬퍼 ──────────────────────────────────────────────────────

    @staticmethod
    def _calc_monthly_price(item: dict) -> None:
        """YEARLY 요금제의 월 환산을 Python에서 직접 계산해 monthlyPrice에 저장.

        LLM이 넘긴 rawPrice 필드를 읽어:
            MONTHLY → monthlyPrice = rawPrice (그대로)
            YEARLY  → monthlyPrice = rawPrice // 12
        rawPrice 키는 제거하고 monthlyPrice로 교체한다.
        """
        raw_price     = item.pop("rawPrice", None)
        billing_cycle = item.get("billingCycle", "MONTHLY")

        if raw_price is None:
            # LLM이 rawPrice 대신 monthlyPrice를 이미 채운 경우 그대로 사용
            return

        if billing_cycle == "YEARLY":
            item["monthlyPrice"] = int(raw_price) // 12
        else:
            item["monthlyPrice"] = int(raw_price)

    @staticmethod
    def _sanitize_price(value) -> int | None:
        """가격 필드를 숫자로 강제 변환.

        LLM이 "11000원", "49900 / 12" 같은 문자열을 넣는 경우를 처리한다.
        - 문자열이면 숫자만 추출 (첫 번째 연속 숫자 시퀀스)
        - 변환 불가면 None 반환
        """
        if value is None:
            return None
        if isinstance(value, int):
            return value
        if isinstance(value, float):
            return int(value)
        # 문자열: 앞쪽 숫자 시퀀스만 추출 ("11000원" → 11000, "49900 / 12" → 49900)
        import re
        m = re.search(r"\d+", str(value))
        return int(m.group()) if m else None

    # 서비스명 → 서비스 코드 매핑 (LLM이 한글 이름으로 내보낼 때 보정)
    _SERVICE_CODE_MAP: dict[str, str] = {
        "티빙": "TVING", "tving": "TVING",
        "웨이브": "WAVVE", "wavve": "WAVVE",
        "넷플릭스": "NETFLIX", "netflix": "NETFLIX",
        "디즈니+": "DISNEY_PLUS", "디즈니플러스": "DISNEY_PLUS",
        "disney+": "DISNEY_PLUS", "disneyplus": "DISNEY_PLUS", "disney_plus": "DISNEY_PLUS",
        "왓챠": "WATCHA", "watcha": "WATCHA",
        "쿠팡플레이": "COUPANG_PLAY", "coupang_play": "COUPANG_PLAY",
    }
    _VALID_CODES = {"NETFLIX", "DISNEY_PLUS", "TVING", "WAVVE", "WATCHA", "COUPANG_PLAY"}

    @classmethod
    def _sanitize_service_code(cls, code: str) -> str | None:
        """serviceCode를 유효한 코드로 강제 변환.

        LLM이 "티빙", " 티빙", "디즈니+" 같은 값을 넣을 때 보정한다.
        - 공백 제거 후 대소문자 무시 매핑 테이블 적용
        - 이미 유효한 코드면 그대로 반환
        - 매핑 불가면 None 반환
        """
        if not code:
            return None
        code = code.strip()
        if code in cls._VALID_CODES:
            return code
        # 대소문자 무시 매핑
        lower = code.lower().replace(" ", "").replace("-", "")
        return cls._SERVICE_CODE_MAP.get(lower) or cls._SERVICE_CODE_MAP.get(code)

    @classmethod
    def _sanitize_bundle_promotions(cls, promotions: list[dict]) -> list[dict]:
        """번들 promotion 항목의 가격·서비스코드를 후처리로 보정.

        LLM이 자주 저지르는 오류:
          - originalPrice/discountPrice 에 "11000원" 같은 문자열
          - services[].serviceCode 에 "티빙", " 웨이브" 같은 한글/공백 포함 값
        """
        sanitized = []
        for item in promotions:
            item["originalPrice"] = cls._sanitize_price(item.get("originalPrice"))
            item["discountPrice"] = cls._sanitize_price(item.get("discountPrice"))

            fixed_services = []
            for svc in item.get("services", []):
                code = cls._sanitize_service_code(svc.get("serviceCode", ""))
                if code:
                    fixed_services.append({"serviceCode": code})
                else:
                    logger.warning(
                        "[LLMNormalizer] 번들 serviceCode 변환 불가, 스킵: %s",
                        svc.get("serviceCode"),
                    )
            item["services"] = fixed_services
            sanitized.append(item)
        return sanitized

    @staticmethod
    def _save_dlq(context: str, raw_data: object, error_info: str) -> None:
        """실패한 raw 데이터를 DLQ 디렉토리에 JSON 파일로 저장.

        파일명: dlq/<context>_<YYYYMMDD_HHMMSS_mmm>.json
        저장 실패 시 로그만 출력하고 파이프라인은 계속 진행한다.

        Args:
            context:    실패 발생 위치 (예: 'normalize_plans', 'servicePlan_validation')
            raw_data:   저장할 raw 데이터 (dict, list 등 JSON 직렬화 가능한 객체)
            error_info: 오류 메시지 또는 LLM 원본 출력
        """
        try:
            DLQ_DIR.mkdir(parents=True, exist_ok=True)
            ts       = datetime.now(KST).strftime("%Y%m%d_%H%M%S_%f")[:-3]
            filename = DLQ_DIR / f"{context}_{ts}.json"
            payload  = {
                "context":    context,
                "savedAt":    datetime.now(KST).isoformat(),
                "errorInfo":  error_info,
                "rawData":    raw_data,
            }
            filename.write_text(
                json.dumps(payload, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            logger.warning("[LLMNormalizer] DLQ 저장: %s", filename)
        except Exception as e:
            logger.error("[LLMNormalizer] DLQ 저장 실패 (파이프라인 계속): %s", e)

    @staticmethod
    def _strip_markdown(text: str) -> str:
        """LLM 출력에서 ```json ... ``` 마크다운 펜스 제거"""
        text = text.strip()
        if text.startswith("```"):
            lines = text.splitlines()
            if lines[0].startswith("```"):
                lines = lines[1:]
            if lines and lines[-1].strip() == "```":
                lines = lines[:-1]
            text = "\n".join(lines).strip()
        return text

    @staticmethod
    def _now_kst() -> str:
        return datetime.now(KST).isoformat()

    @classmethod
    def _empty_service_catalog(cls) -> dict:
        return {
            "jobType":      "SERVICE_CATALOG_CRAWL",
            "collectedAt":  cls._now_kst(),
            "services":     [],
            "servicePlans": [],
        }

    @classmethod
    def _empty_promotion_crawl(cls) -> dict:
        return {
            "jobType":     "PROMOTION_CRAWL",
            "collectedAt": cls._now_kst(),
            "promotions":  [],
        }
