# 백엔드 assets에 고정된 로고 경로 — 스크래핑 불필요
SERVICE_LOGO_URLS: dict[str, str] = {
    # OTT
    "NETFLIX":       "/assets/services/NETFLIX.png",
    "TVING":         "/assets/services/TVING.png",
    "WAVVE":         "/assets/services/WAVVE.png",
    "DISNEY_PLUS":   "/assets/services/DISNEY_PLUS.png",
    "WATCHA":        "/assets/services/WATCHA.png",
    "COUPANG_PLAY":  "/assets/services/COUPANG_PLAY.png",
    # MUSIC
    "MELON":         "/assets/services/MELON.png",
    "BUGS":          "/assets/services/BUGS.png",
    "YOUTUBE_MUSIC": "/assets/services/YOUTUBE_MUSIC.png",
    "SPOTIFY":       "/assets/services/SPOTIFY.png",
    # AI
    "CHATGPT":       "/assets/services/CHATGPT.png",
    "GEMINI":        "/assets/services/GEMINI.png",
    "CLAUDE":        "/assets/services/CLAUDE.png",
}

# 각 서비스 해지 안내 고정 URL
SERVICE_CANCEL_URLS: dict[str, str] = {
    # OTT
    "NETFLIX":       "https://help.netflix.com/ko/node/407",
    "TVING":         "https://www.tving.com/help/withdrawal-subscription",
    "WAVVE":         "https://www.wavve.com/customer/faq?faqId=3872",
    "DISNEY_PLUS":   "https://help.disneyplus.com/ko/article/disneyplus-cancel",
    "WATCHA":        "https://help.watcha.com/hc/ko/articles/31326576396825-TV%EC%97%90%EC%84%9C-%EA%B5%AC%EB%8F%85-%EC%A4%91%EC%9D%B8%EB%8D%B0-%ED%95%B4%EC%A7%80%ED%95%98%EA%B3%A0-%EC%8B%B6%EC%96%B4%EC%9A%94",
    "COUPANG_PLAY":  "https://www.coupangplay.com/faq",
    # MUSIC
    "MELON":         "https://faqs2.melon.com/customer/faq/informFaq.htm?faqId=2212",
    "BUGS":          "https://music.bugs.co.kr/pay/recommend",
    "YOUTUBE_MUSIC": "https://support.google.com/youtubemusic/answer/6308278?hl=ko",
    "SPOTIFY":       "https://support.spotify.com/kr-ko/article/cancel-premium/",
    # AI
    "CHATGPT":       "https://help.openai.com/ko-kr/articles/7232927-chatgpt-%EA%B5%AC%EB%8F%85%EC%9D%84-%EC%B7%A8%EC%86%8C%ED%95%98%EB%A0%A4%EB%A9%B4-%EC%96%B4%EB%96%BB%EA%B2%8C-%ED%95%B4%EC%95%BC-%ED%95%98%EB%82%98%EC%9A%94",
    "GEMINI":        "https://support.google.com/gemini/answer/14517446",
    "CLAUDE":        "https://support.anthropic.com/en/articles/8325617",
}

# 서비스 코드 → 카테고리 매핑
SERVICE_CATEGORY: dict[str, str] = {
    # OTT
    "NETFLIX":       "OTT",
    "TVING":         "OTT",
    "WAVVE":         "OTT",
    "DISNEY_PLUS":   "OTT",
    "WATCHA":        "OTT",
    "COUPANG_PLAY":  "OTT",
    # MUSIC
    "MELON":         "MUSIC",
    "BUGS":          "MUSIC",
    "YOUTUBE_MUSIC": "MUSIC",
    "SPOTIFY":       "MUSIC",
    # AI
    "CHATGPT":       "AI",
    "GEMINI":        "AI",
    "CLAUDE":        "AI",
}


# ════════════════════════════════════════════════════════════════════════════
# OTT 스크래퍼 베이스
# ════════════════════════════════════════════════════════════════════════════

class OTTScraper:

    # ── 공통 필수 구현 메서드 (모든 하위 클래스에서 override) ──

    def scrap_logo(self) -> str:
        """PageHeader 영역 내 <svg> 태그 전체를 문자열로 반환

        Returns:
            str: SVG 태그 전체 문자열
                 예) '<svg xmlns="..." viewBox="...">...</svg>'

        권장 수집 주기: 월 1회
        """
        raise NotImplementedError

    def scrap_plans(self) -> list[dict]:
        """요금제 목록을 raw dict 리스트로 반환

        Returns:
            list[dict]: 요금제 1건당 아래 형식
            {
                "platform":       str,        # 플랫폼 코드 소문자 (예: 'tving')
                "tab":            str | None,  # 카테고리 탭 표시명 (없으면 None)
                "services":       list[str],   # 서비스명 리스트 (예: ['티빙', 'Apple TV+'])
                "plan_name":      str,         # 요금제명
                "billing_cycle":  str,         # 'monthly' | 'yearly'
                "price":          int,         # 실제 결제 금액 (원)
                "original_price": int | None,  # 할인 전 정가 (없으면 None)
                "description":    list[str],   # 스펙 요약 문자열 리스트
            }

        권장 수집 주기: 일 1회
        """
        raise NotImplementedError

    def scrap_cautions(self) -> dict[str, list]:
        """구독 주의사항을 섹션명 → 항목 목록 형태로 반환

        Returns:
            dict[str, list[str]]: 섹션명 → 주의사항 항목 목록
                예) {"계정 및 결제": ["...", ...], "해지 및 환불": [...]}

        권장 수집 주기: 주 1회
        """
        raise NotImplementedError

    def scrap_company_info(self) -> list[str]:
        """사업자 정보 줄 목록을 반환

        Returns:
            list[str]: 사업자 정보 각 줄
                예) ["대표이사 강석원  |  ...", "사업자 등록번호 ..."]

        권장 수집 주기: 월 1회
        """
        raise NotImplementedError

    # ── 통합 수집 (스케줄러가 직접 호출하지 않음) ──

    def scrap(self) -> dict:
        """
        OTT 플랫폼 정보를 수집하여 반환

        Returns:
            {
                "plans": list[dict],  # 요금제 목록
                    각 dict:
                    {
                        "platform": str,
                        "tab": str,
                        "services": list[str],
                        "plan_name": str,
                        "billing_cycle": "monthly" | "yearly",
                        "price": int,
                        "original_price": int | None,
                        "description": list[str],
                    }
                "cautions": dict[str, list[str]],  # 주의사항 (제목 → 항목 목록)
                "company_info": list[str],          # 사업자 정보 줄 목록
            }
        """
        raise NotImplementedError


# ════════════════════════════════════════════════════════════════════════════
# 음악 스트리밍 스크래퍼 베이스
# ════════════════════════════════════════════════════════════════════════════

class MusicScraper:
    """음악 스트리밍 서비스 요금제 수집을 위한 공통 인터페이스

    OTT와 달리 음악 서비스는 아래 특성을 가진다:
    - 로그인 없이 요금제 페이지 접근 가능 (대부분)
    - 탭 구분: 개인 / 가족 / 학생 등 대상별 분리가 있는 경우 있음
    - 번들: 멜론×넷플릭스 등 타 서비스와 결합 상품 존재 가능
    - 음질 티어(일반/Hi-Fi/무손실)가 요금제 구분 기준이 되기도 함
    - 오프라인 재생 가능 여부가 핵심 spec 항목

    수집 주기 기준:
        scrap_plans()        일 1회  (가격 변동 모니터링)
        scrap_cautions()     주 1회
        scrap_company_info() 월 1회
    """

    def scrap_plans(self) -> list[dict]:
        """요금제 목록을 raw dict 리스트로 반환

        Returns:
            list[dict]: 요금제 1건당 아래 형식
            {
                "platform":       str,        # 플랫폼 코드 소문자 (예: 'melon')
                "tab":            str | None,  # 대상 탭 표시명 (예: '개인', '가족', None)
                "services":       list[str],   # 서비스명 리스트 (예: ['멜론'])
                                               # 번들인 경우 복수 (예: ['멜론', '넷플릭스'])
                "plan_name":      str,         # 요금제명 (예: '스트리밍클럽', 'Hi-Fi 스트리밍클럽')
                "billing_cycle":  str,         # 'monthly' | 'yearly'
                "price":          int,         # 실제 결제 금액 (원)
                "original_price": int | None,  # 할인 전 정가 (번들 할인 시 명시)
                "description":    list[str],   # 스펙 요약
                                               # 예: ['스트리밍: 무제한', '오프라인 재생: 불가',
                                               #      '음질: 일반', '동시 기기: 1대']
            }

        권장 수집 주기: 일 1회
        """
        raise NotImplementedError

    def scrap_cautions(self) -> dict[str, list[str]]:
        """이용 주의사항을 섹션명 → 항목 목록 형태로 반환

        Returns:
            dict[str, list[str]]: 섹션명 → 주의사항 항목 목록
                예) {
                    "이용권 안내":  ["이용권은 구매일로부터 ...", ...],
                    "해지 및 환불": ["해지 시 남은 기간은 ...", ...],
                }

        권장 수집 주기: 주 1회
        """
        raise NotImplementedError

    def scrap_company_info(self) -> list[str]:
        """사업자 정보 줄 목록을 반환

        Returns:
            list[str]: 사업자 정보 각 줄
                예) ["(주)카카오엔터테인먼트  대표이사 ...", "사업자 등록번호 ..."]

        권장 수집 주기: 월 1회
        """
        raise NotImplementedError

    def scrap(self) -> dict:
        """플랫폼 전체 정보를 한 번에 수집하여 반환

        Returns:
            {
                "plans":        list[dict],         # 요금제 목록
                "cautions":     dict[str, list[str]], # 주의사항
                "company_info": list[str],            # 사업자 정보
            }
        """
        raise NotImplementedError


# ════════════════════════════════════════════════════════════════════════════
# AI 서비스 스크래퍼 베이스
# ════════════════════════════════════════════════════════════════════════════

class AIScraper:
    """AI 구독 서비스 요금제 수집을 위한 공통 인터페이스

    AI 서비스(ChatGPT, Gemini, Claude 등)는 OTT·음악과 다른 특성을 가진다:
    - 요금제가 기능 제한(메시지 횟수, 모델 접근, 컨텍스트 길이) 기준으로 구분됨
    - 연간 결제 시 월 환산 할인가 표기 방식이 서비스마다 상이함
    - 팀/비즈니스 플랜은 per-seat(인당) 가격 구조를 가질 수 있음
    - 가격이 USD 기준으로 표기되고 원화로 환산되는 경우 있음
    - API 요금과 구독 요금이 혼재되어 있으므로 구독 플랜만 선별 필요
    - 무료(Free) 플랜은 수집하되 price=0 으로 표기

    수집 주기 기준:
        scrap_plans()        일 1회  (가격·플랜 변동이 잦음)
        scrap_cautions()     주 1회
        scrap_company_info() 월 1회
    """

    def scrap_plans(self) -> list[dict]:
        """요금제 목록을 raw dict 리스트로 반환

        Returns:
            list[dict]: 요금제 1건당 아래 형식
            {
                "platform":       str,        # 플랫폼 코드 소문자 (예: 'chatgpt')
                "tab":            str | None,  # 대상 탭 (예: '개인', '팀', '기업', None)
                "services":       list[str],   # 서비스명 리스트 (예: ['ChatGPT'])
                "plan_name":      str,         # 요금제명 (예: 'Plus', 'Pro', 'Business')
                "billing_cycle":  str,         # 'monthly' | 'yearly'
                "price":          int,         # 실제 결제 금액 (원, 월 환산)
                                               # 무료 플랜은 0
                "original_price": int | None,  # 연간 결제 시 월정가 (할인 전), 없으면 None
                "description":    list[str],   # 플랜 기능 요약
                                               # 예: ['GPT-4o 무제한', '고급 데이터 분석',
                                               #      'DALL·E 이미지 생성', '월 메시지: 무제한']
            }

        ※ 팀/비즈니스 플랜의 경우 price는 인당(per-seat) 월 금액으로 통일
        ※ 연간 결제 플랜은 billing_cycle='yearly', price=월 환산 금액으로 수집

        권장 수집 주기: 일 1회
        """
        raise NotImplementedError

    def scrap_cautions(self) -> dict[str, list[str]]:
        """이용 주의사항을 섹션명 → 항목 목록 형태로 반환

        Returns:
            dict[str, list[str]]: 섹션명 → 주의사항 항목 목록
                예) {
                    "결제 및 청구":  ["구독은 월/연 단위로 자동 갱신됩니다.", ...],
                    "해지 안내":     ["해지 시 즉시 무료 플랜으로 전환됩니다.", ...],
                    "환불 정책":     ["구독 기간 중 부분 환불은 제공되지 않습니다.", ...],
                }

        권장 수집 주기: 주 1회
        """
        raise NotImplementedError

    def scrap_company_info(self) -> list[str]:
        """사업자 정보 줄 목록을 반환

        AI 서비스는 해외 법인이 대부분이며, 한국 사업자 등록 정보가
        없을 수 있다. 이 경우 공식 서비스 약관 페이지의 운영사 정보를 반환한다.

        Returns:
            list[str]: 사업자 정보 각 줄
                예) ["OpenAI, L.L.C.", "주소: 3180 18th St, San Francisco, CA 94110"]
                정보 없으면 빈 리스트 반환

        권장 수집 주기: 월 1회
        """
        raise NotImplementedError

    def scrap(self) -> dict:
        """플랫폼 전체 정보를 한 번에 수집하여 반환

        Returns:
            {
                "plans":        list[dict],           # 요금제 목록
                "cautions":     dict[str, list[str]], # 주의사항
                "company_info": list[str],            # 사업자/운영사 정보
            }
        """
        raise NotImplementedError


# ════════════════════════════════════════════════════════════════════════════
# 카드 스크래퍼 베이스 (기존 유지)
# ════════════════════════════════════════════════════════════════════════════

class CardScraper:
    """카드 정보 수집을 위한 공통 인터페이스"""

    def get_card_links(self, search_url: str) -> list[str]:
        """검색 결과 페이지에서 모든 카드 상세 링크 수집"""
        raise NotImplementedError

    def scrap_card_detail(self, detail_url: str, card_type: str) -> dict:
        """개별 카드 상세 정보 수집"""
        raise NotImplementedError

    def _extract_redirect_url(self, page) -> str:
        """카드사 바로가기 버튼의 최종 목적지 URL 추출 (헬퍼 함수)"""
        raise NotImplementedError


# ════════════════════════════════════════════════════════════════════════════
# 프로모션 스크래퍼 베이스 (기존 유지)
# ════════════════════════════════════════════════════════════════════════════

class PromotionScraper:
    """프로모션/공지 수집을 위한 공통 인터페이스

    OTT 스크래퍼(OTTScraper)에서 분리된 프로모션 전용 스크래퍼.
    플랫폼별로 상속받아 scrap() 구현.

    수집 주기: 주 1회 (scheduler run_weekly_promotions)
    """

    def scrap(self) -> list[dict]:
        """프로모션/공지 목록 수집

        Returns:
            list[dict]: 1건당 아래 형식
            {
                "platform": str,   # 플랫폼 코드 소문자 (예: 'tving')
                "title":    str,   # 공지/프로모션 제목
                "date":     str,   # 작성일 또는 수집일 (예: '2026.03.04')
                "content":  str,   # 본문 전체 텍스트
            }

        수집 주기: 주 1회
        """
        raise NotImplementedError