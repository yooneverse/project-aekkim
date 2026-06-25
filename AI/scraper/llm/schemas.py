"""
LLM 출력값 Pydantic 검증 스키마

SERVICE_CATALOG_CRAWL / PROMOTION_CRAWL 두 가지 형식을 검증한다.
검증 실패 시 해당 항목만 스킵하고 오류 로그를 출력한다.
"""
from typing import Literal
from pydantic import BaseModel


# ── SERVICE_CATALOG_CRAWL ────────────────────────────────────────────────

class ServiceSchema(BaseModel):
    code:                  str
    name:                  str
    category:              str
    logoUrl:               str | None
    cancelGuideUrl:        str | None
    customerServicePhone:  str | None
    contactEmail:          str | None


class ServicePlanSchema(BaseModel):
    serviceCode:   str
    planName:      str
    billingCycle:  Literal['MONTHLY', 'YEARLY']
    monthlyPrice:  int


class ServiceCatalogCrawlSchema(BaseModel):
    jobType:      Literal['SERVICE_CATALOG_CRAWL']
    collectedAt:  str
    services:     list[ServiceSchema]
    servicePlans: list[ServicePlanSchema]


# ── PROMOTION_CRAWL ──────────────────────────────────────────────────────

class PromotionServiceSchema(BaseModel):
    serviceCode: str


class PromotionSchema(BaseModel):
    promotionType:  Literal['CARD_BENEFIT', 'BUNDLE', 'PROMO']
    title:          str
    summary:        str | None
    originalPrice:  int | None
    discountPrice:  int | None
    startsAt:       str
    endsAt:         str
    sourceUrl:      str | None
    imgUrl:         str | None = None  # CARD_BENEFIT 전용, BUNDLE/PROMO는 None
    services:       list[PromotionServiceSchema]


class PromotionCrawlSchema(BaseModel):
    jobType:     Literal['PROMOTION_CRAWL']
    collectedAt: str
    promotions:  list[PromotionSchema]