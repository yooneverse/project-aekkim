"""
MySQL UPSERT 클라이언트

ERD 기준 테이블 구조:
    Service         : code(PK), name, category, logoUrl
    ServicePlan     : servicePlanId(PK), serviceId(FK), planName, billingCycle, monthlyPrice
    Promotion       : promotionId(PK), promotionType, title, summary,
                      startsAt, endsAt, sourceUrl
    PromotionService: promotionServiceId(PK), promotionId(FK), serviceId(FK),
                      savedAmount, generatedCycleYm, matchedCardId

환경변수로 DB 연결 설정:
    DB_HOST  (기본값: localhost)
    DB_PORT  (기본값: 3306)
    DB_NAME  (기본값: aekkeum)
    DB_USER  (기본값: root)
    DB_PASS  (기본값: "")

오류 발생 시 롤백 후 로그 출력, 파이프라인은 계속 진행.
"""
import logging
import os

import pymysql
import pymysql.cursors

logger = logging.getLogger(__name__)

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", 3306))
DB_NAME = os.getenv("DB_NAME", "aekkeum")
DB_USER = os.getenv("DB_USER", "root")
DB_PASS = os.getenv("DB_PASS", "")


def _get_connection() -> pymysql.Connection:
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        db=DB_NAME,
        user=DB_USER,
        password=DB_PASS,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


class DBClient:

    # ── 공개 메서드 ────────────────────────────────────────────────────────

    def upsert_service_plans(self, normalized: dict) -> None:
        """
        SERVICE_CATALOG_CRAWL JSON → Service / ServicePlan 테이블 UPSERT

        흐름:
            1. Service UPSERT (code PK 기준)
            2. serviceId 조회 (code → serviceId)
            3. ServicePlan UPSERT (serviceId FK 기준)

        Args:
            normalized: LLMNormalizer.normalize_plans() 반환값
        """
        if normalized.get("jobType") != "SERVICE_CATALOG_CRAWL":
            logger.warning("[DBClient] upsert_service_plans: jobType 불일치, 스킵")
            return

        conn = _get_connection()
        try:
            with conn.cursor() as cur:
                for svc in normalized.get("services", []):
                    # ① Service UPSERT
                    self._upsert_service(cur, svc)

                    # ② serviceId 조회
                    service_id = self._get_service_id(cur, svc["code"])
                    if service_id is None:
                        logger.error("[DBClient] serviceId 조회 실패: %s — ServicePlan 스킵", svc["code"])
                        continue

                    # ③ ServicePlan UPSERT (해당 service_code 요금제만)
                    for plan in normalized.get("servicePlans", []):
                        if plan.get("serviceCode") == svc["code"]:
                            self._upsert_service_plan(cur, plan, service_id)

            conn.commit()
            logger.info(
                "[DBClient] upsert_service_plans 완료 — services: %d, plans: %d",
                len(normalized.get("services", [])),
                len(normalized.get("servicePlans", [])),
            )
        except Exception as e:
            conn.rollback()
            logger.error("[DBClient] upsert_service_plans 오류, 롤백: %s", e)
        finally:
            conn.close()

    def upsert_promotions(self, normalized: dict) -> None:
        """
        PROMOTION_CRAWL JSON → Promotion / PromotionService 테이블 UPSERT

        흐름:
            1. Promotion UPSERT (title + startsAt UNIQUE 기준)
            2. promotionId 조회
            3. 각 serviceCode → serviceId 조회
            4. PromotionService UPSERT (promotionId + serviceId)

        Args:
            normalized: LLMNormalizer.normalize_promotions() 반환값
        """
        if normalized.get("jobType") != "PROMOTION_CRAWL":
            logger.warning("[DBClient] upsert_promotions: jobType 불일치, 스킵")
            return

        conn = _get_connection()
        try:
            with conn.cursor() as cur:
                for promo in normalized.get("promotions", []):
                    # ① Promotion UPSERT
                    self._upsert_promotion(cur, promo)

                    # ② promotionId 조회
                    promotion_id = self._get_promotion_id(cur, promo.get("title"), promo.get("startsAt"))
                    if promotion_id is None:
                        logger.error("[DBClient] promotionId 조회 실패: %s — PromotionService 스킵", promo.get("title"))
                        continue

                    # ③ PromotionService UPSERT (서비스별 행 분리)
                    for svc in promo.get("services", []):
                        service_id = self._get_service_id(cur, svc.get("serviceCode", ""))
                        if service_id is None:
                            logger.warning(
                                "[DBClient] serviceId 조회 실패: %s — PromotionService 행 스킵",
                                svc.get("serviceCode"),
                            )
                            continue
                        self._upsert_promotion_service(cur, promotion_id, service_id)

            conn.commit()
            logger.info(
                "[DBClient] upsert_promotions 완료 — promotions: %d",
                len(normalized.get("promotions", [])),
            )
        except Exception as e:
            conn.rollback()
            logger.error("[DBClient] upsert_promotions 오류, 롤백: %s", e)
        finally:
            conn.close()

    # ── Private 헬퍼 ──────────────────────────────────────────────────────

    def _upsert_service(self, cur, svc: dict) -> None:
        """Service 테이블 UPSERT

        ERD 컬럼: serviceId(PK AUTO), code, name, category, logoUrl
        UPSERT 기준: code UNIQUE
        """
        sql = """
            INSERT INTO Service
                (code, name, category, logoUrl)
            VALUES
                (%(code)s, %(name)s, %(category)s, %(logoUrl)s)
            ON DUPLICATE KEY UPDATE
                name     = VALUES(name),
                category = VALUES(category),
                logoUrl  = VALUES(logoUrl)
        """
        cur.execute(sql, {
            "code":     svc.get("code"),
            "name":     svc.get("name"),
            "category": svc.get("category"),
            "logoUrl":  svc.get("logoUrl"),
        })

    def _get_service_id(self, cur, code: str) -> int | None:
        """code로 serviceId 조회"""
        cur.execute("SELECT serviceId FROM Service WHERE code = %s", (code,))
        row = cur.fetchone()
        return row["serviceId"] if row else None

    def _upsert_service_plan(self, cur, plan: dict, service_id: int) -> None:
        """ServicePlan 테이블 UPSERT

        ERD 컬럼: servicePlanId(PK AUTO), serviceId(FK), planName, billingCycle, monthlyPrice
        UPSERT 기준: serviceId + planName UNIQUE
        """
        sql = """
            INSERT INTO ServicePlan
                (serviceId, planName, billingCycle, monthlyPrice)
            VALUES
                (%(serviceId)s, %(planName)s, %(billingCycle)s, %(monthlyPrice)s)
            ON DUPLICATE KEY UPDATE
                billingCycle = VALUES(billingCycle),
                monthlyPrice = VALUES(monthlyPrice)
        """
        cur.execute(sql, {
            "serviceId":    service_id,
            "planName":     plan.get("planName"),
            "billingCycle": plan.get("billingCycle"),
            "monthlyPrice": plan.get("monthlyPrice"),
        })

    def _upsert_promotion(self, cur, promo: dict) -> None:
        """Promotion 테이블 UPSERT

        ERD 컬럼: promotionId(PK AUTO), promotionType, title, summary,
                  startsAt, endsAt, sourceUrl
        UPSERT 기준: title + startsAt UNIQUE

        ※ originalPrice, discountPrice는 ERD에 없으므로 저장하지 않음
        ※ imgUrl은 ERD에 없으므로 저장하지 않음 (필요 시 ERD 컬럼 추가 후 반영)
        """
        sql = """
            INSERT INTO Promotion
                (promotionType, title, summary, startsAt, endsAt, sourceUrl)
            VALUES
                (%(promotionType)s, %(title)s, %(summary)s,
                 %(startsAt)s, %(endsAt)s, %(sourceUrl)s)
            ON DUPLICATE KEY UPDATE
                promotionType = VALUES(promotionType),
                summary       = VALUES(summary),
                endsAt        = VALUES(endsAt),
                sourceUrl     = VALUES(sourceUrl)
        """
        cur.execute(sql, {
            "promotionType": promo.get("promotionType"),
            "title":         promo.get("title"),
            "summary":       promo.get("summary"),
            "startsAt":      promo.get("startsAt"),
            "endsAt":        promo.get("endsAt"),
            "sourceUrl":     promo.get("sourceUrl"),
        })

    def _get_promotion_id(self, cur, title: str, starts_at: str) -> int | None:
        """title + startsAt으로 promotionId 조회"""
        cur.execute(
            "SELECT promotionId FROM Promotion WHERE title = %s AND startsAt = %s",
            (title, starts_at),
        )
        row = cur.fetchone()
        return row["promotionId"] if row else None

    def _upsert_promotion_service(self, cur, promotion_id: int, service_id: int) -> None:
        """PromotionService 테이블 UPSERT

        ERD 컬럼: promotionServiceId(PK AUTO), promotionId(FK), serviceId(FK),
                  savedAmount, generatedCycleYm, matchedCardId
        스크래퍼는 promotionId + serviceId만 채움. 나머지는 Spring이 관리.
        UPSERT 기준: promotionId + serviceId UNIQUE
        """
        sql = """
            INSERT INTO PromotionService
                (promotionId, serviceId)
            VALUES
                (%(promotionId)s, %(serviceId)s)
            ON DUPLICATE KEY UPDATE
                promotionId = VALUES(promotionId)
        """
        cur.execute(sql, {
            "promotionId": promotion_id,
            "serviceId":   service_id,
        })