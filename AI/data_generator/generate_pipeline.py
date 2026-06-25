"""
애낌 합성 결제 데이터 생성 파이프라인
--------------------------------------
흐름:
  1. Personal Finance Data CSV 로드
  2. SDV로 거래 구조 합성
  3. 후처리: 한국 결제명 교체 + 원화 변환 + 유저 케이스 분류
  4. SSAFY API 형식으로 변환
  5. 입금 → 출금 순서로 API 호출
"""

import random
import datetime
import os
import pandas as pd
from collections import defaultdict
try:
    from pydantic import BaseModel
    PYDANTIC_AVAILABLE = True
except ImportError:
    PYDANTIC_AVAILABLE = False
    class BaseModel:
        def __init__(self, **kwargs): self.__dict__.update(kwargs)
        def model_dump(self): return self.__dict__
        def model_dump_json(self, indent=None):
            import json
            return json.dumps(self.__dict__, default=str, indent=indent)

try:
    import requests
except ImportError:
    requests = None

try:
    from sdv.single_table import GaussianCopulaSynthesizer
    from sdv.metadata import SingleTableMetadata
    SDV_AVAILABLE = True
except ImportError:
    SDV_AVAILABLE = False


# ──────────────────────────────────────────
# 1. 결제명 매핑 테이블
# ──────────────────────────────────────────

# 구독 서비스별 실제 결제창 표기 패턴
# 설문조사 결과 추가되면 여기에 append
SUBSCRIPTION_MERCHANTS = {
    "넷플릭스": [
        "NETFLIX_INICIS",
        "NETFLIX_INICIS-넷플릭스서비시스코리",
        "Netflix.com",
        "NETFLIX.COM",
    ],
    "티빙": [
        "주식회사 티빙",
        "Apple",           # 앱스토어 경유 결제
    ],
    "티빙+디즈니번들": [
        "더블 (디즈니+)",
    ],
    "웨이브": [
        # 설문조사 수집 후 추가 예정
    ],
    "디즈니플러스": [
        # 설문조사 수집 후 추가 예정
    ],
    "왓챠": [
        # 설문조사 수집 후 추가 예정
    ],
    "쿠팡": [
        "쿠팡와우월회비",
        "쿠팡(와우멤버십)",
    ],
    "네이버멤버십": [
        "네이버플러스멤버십",
    ],
}

# 실제 요금제 금액 (service_plans DB 기준)
# SDV가 만든 USD 금액 대신 이 값으로 스냅
SUBSCRIPTION_PRICES = {
    "넷플릭스":       [13500, 15000, 17000],   # 광고형/스탠다드/프리미엄
    "티빙":           [7900, 10900, 13900],
    "티빙+디즈니번들": [10900],
    "웨이브":         [7900, 10900, 13900],
    "디즈니플러스":   [9900, 13900],
    "왓챠":           [7900, 12900],
    "쿠팡":           [7890],
    "네이버멤버십":   [4900],
}

# 비구독 결제명 풀 (KB 거래내역 실제 패턴)
NON_SUB_MERCHANTS = [
    "씨유(CU)부산글로벌", "세븐일레븐부산대연", "GS25부경삼광점",
    "지에스(GS)25좌동코", "이마트24R부경대가", "보너스마트",
    "스타벅스코리아", "하삼동커피부경대점", "텐퍼센트대연자이점",
    "컴포즈커피부산롯데", "투썸플레이스장유율", "매머드커피익스프레",
    "맥도날드부산송정DT", "마츠도", "주식회사빡벳부경",
    "부경대학교소비자생", "부경대학교", "탑플러스마트(부경대",
    "(주)이비카드택시법", "(주)이스턴웰스본사", "커피베이부산오시리",
]


# ──────────────────────────────────────────
# 2. 유저 케이스 정의
# ──────────────────────────────────────────

def get_user_subscriptions(case: int) -> list[str]:
    """
    케이스별 유저가 구독 중인 서비스 목록 반환

    케이스 1: 네이버멤버십 + OTT 직접 결제 (중복 사용)
    케이스 2: 네이버멤버십만 있음 (OTT 직접 결제 없음)
    케이스 3: 네이버멤버십 없고 OTT 직접 결제
    """
    ott_pool = [s for s in SUBSCRIPTION_MERCHANTS
                if s not in ("네이버멤버십",) and SUBSCRIPTION_MERCHANTS[s]]

    if case == 1:
        # 네이버멤버십 + OTT 1~2개 직접 결제
        otts = random.sample(ott_pool, k=random.randint(1, 2))
        return ["네이버멤버십"] + otts

    elif case == 2:
        # 네이버멤버십만
        return ["네이버멤버십"]

    elif case == 3:
        # OTT 1~3개 직접 결제 (네이버멤버십 없음)
        return random.sample(ott_pool, k=random.randint(1, 3))

    return []


# ──────────────────────────────────────────
# 3. SDV 합성 + 후처리
# ──────────────────────────────────────────

def synthesize_base(real_csv_path: str, n_users: int = 30) -> pd.DataFrame:
    """
    Personal Finance Data를 SDV로 학습해 거래 구조를 합성한다.
    네트워크 없이 테스트할 땐 real_csv_path=None으로 호출하면
    더미 DataFrame을 반환한다.
    """
    if real_csv_path is None or not SDV_AVAILABLE:
        if not SDV_AVAILABLE:
            print("    [INFO] SDV 미설치 → 더미 데이터로 대체 (pip install sdv 후 실제 합성 가능)")
        return _make_dummy_base(n_users)

    df = pd.read_csv(real_csv_path)
    meta = SingleTableMetadata()
    meta.detect_from_dataframe(df)

    synth = GaussianCopulaSynthesizer(meta)
    synth.fit(df)
    return synth.sample(num_rows=n_users * 20)  # 유저당 평균 20건


def _make_dummy_base(n_users: int) -> pd.DataFrame:
    """SDV 없이 돌릴 수 있는 더미 거래 구조 생성"""
    rows = []
    base_date = datetime.date(2024, 1, 1)
    for user_id in range(1, n_users + 1):
        n_txn = random.randint(15, 30)
        for _ in range(n_txn):
            delta = random.randint(0, 364)
            rows.append({
                "user_id": user_id,
                "date": base_date + datetime.timedelta(days=delta),
                "category": random.choices(
                    ["Subscription", "Food", "Transport", "Shopping"],
                    weights=[0.15, 0.40, 0.25, 0.20]
                )[0],
                "amount_usd": round(random.uniform(1, 80), 2),
            })
    return pd.DataFrame(rows)


def postprocess(base_df: pd.DataFrame, n_users: int = 30) -> list[dict]:
    """
    SDV 결과물에 한국 결제명 + 원화 금액 + 유저 케이스 적용

    반환: 출금 API 호출에 바로 쓸 수 있는 dict 리스트
    """
    KRW_RATE = 1350  # USD → KRW 환율

    # 유저별 케이스 배분 (1:1:3 비율)
    case_weights = [1, 1, 3]
    user_cases = {
        uid: random.choices([1, 2, 3], weights=case_weights)[0]
        for uid in range(1, n_users + 1)
    }
    user_subscriptions = {
        uid: get_user_subscriptions(user_cases[uid])
        for uid in range(1, n_users + 1)
    }

    records = []
    for _, row in base_df.iterrows():
        uid = int(row.get("user_id", random.randint(1, n_users)))
        uid = max(1, min(uid, n_users))
        date = row["date"] if isinstance(row["date"], datetime.date) \
               else datetime.date.today()
        category = row.get("category", "Food")

        if category == "Subscription":
            subs = user_subscriptions[uid]
            # 결제 가능한 서비스만 (결제명 있는 것)
            available = [s for s in subs if SUBSCRIPTION_MERCHANTS.get(s)]
            if not available:
                # 결제명 미수집 서비스만 있는 유저 → 비구독으로 대체
                merchant = random.choice(NON_SUB_MERCHANTS)
                amount = random.randint(1000, 15000)
            else:
                service = random.choice(available)
                merchant = random.choice(SUBSCRIPTION_MERCHANTS[service])
                amount = random.choice(SUBSCRIPTION_PRICES.get(service, [9900]))
        else:
            merchant = random.choice(NON_SUB_MERCHANTS)
            amount = int(row["amount_usd"] * KRW_RATE / 100) * 100
            amount = max(500, min(amount, 50000))

        records.append({
            "user_id": uid,
            "user_case": user_cases[uid],
            "date": date,
            "category": category,
            "merchant": merchant,
            "amount_krw": amount,
        })

    return records


# ──────────────────────────────────────────
# 4. SSAFY API 형식 변환
# ──────────────────────────────────────────

class SSAFYHeader(BaseModel):
    apiName: str
    transmissionDate: str   # YYYYMMDD
    transmissionTime: str   # HHMMSS
    institutionCode: str = "00100"
    fintechAppNo: str = "001"
    apiServiceCode: str
    institutionTransactionUniqueNo: str  # 20자리 고유번호
    apiKey: str
    userKey: str


class WithdrawalRequest(BaseModel):
    Header: SSAFYHeader
    accountNo: str
    transactionBalance: str
    transactionSummary: str  # ← 여기에 가맹점명 주입


class DepositRequest(BaseModel):
    Header: SSAFYHeader
    accountNo: str
    transactionBalance: str
    transactionSummary: str


_seq_counter = 0

def _unique_no() -> str:
    global _seq_counter
    _seq_counter += 1
    now = datetime.datetime.now()
    return now.strftime("%Y%m%d%H%M%S") + str(_seq_counter).zfill(6)


def to_withdrawal_request(
    record: dict,
    account_no: str,
    api_key: str,
    user_key: str,
) -> WithdrawalRequest:
    txn_date = record["date"]
    if isinstance(txn_date, datetime.date):
        date_str = txn_date.strftime("%Y%m%d")
        time_str = f"{random.randint(0,23):02d}{random.randint(0,59):02d}{random.randint(0,59):02d}"
    else:
        now = datetime.datetime.now()
        date_str = now.strftime("%Y%m%d")
        time_str = now.strftime("%H%M%S")

    header = SSAFYHeader(
        apiName="updateDemandDepositAccountWithdrawal",
        transmissionDate=date_str,
        transmissionTime=time_str,
        apiServiceCode="updateDemandDepositAccountWithdrawal",
        institutionTransactionUniqueNo=_unique_no(),
        apiKey=api_key,
        userKey=user_key,
    )
    return WithdrawalRequest(
        Header=header,
        accountNo=account_no,
        transactionBalance=str(record["amount_krw"]),
        transactionSummary=record["merchant"],
    )


def to_deposit_request(
    amount: int,
    account_no: str,
    api_key: str,
    user_key: str,
    summary: str = "초기잔액충전",
) -> DepositRequest:
    now = datetime.datetime.now()
    header = SSAFYHeader(
        apiName="updateDemandDepositAccountDeposit",
        transmissionDate=now.strftime("%Y%m%d"),
        transmissionTime=now.strftime("%H%M%S"),
        apiServiceCode="updateDemandDepositAccountDeposit",
        institutionTransactionUniqueNo=_unique_no(),
        apiKey=api_key,
        userKey=user_key,
    )
    return DepositRequest(
        Header=header,
        accountNo=account_no,
        transactionBalance=str(amount),
        transactionSummary=summary,
    )


# ──────────────────────────────────────────
# 5. API 호출
# ──────────────────────────────────────────

BASE_URL = os.getenv(
    "FINANCE_DEMAND_DEPOSIT_API_BASE_URL",
    "https://finance-api.example.com/v1/demand-deposit",
)

def call_deposit(req: DepositRequest) -> dict:
    url = f"{BASE_URL}/updateDemandDepositAccountDeposit"
    resp = requests.post(url, json=req.model_dump(), timeout=10)
    resp.raise_for_status()
    return resp.json()


def call_withdrawal(req: WithdrawalRequest) -> dict:
    url = f"{BASE_URL}/updateDemandDepositAccountWithdrawal"
    resp = requests.post(url, json=req.model_dump(), timeout=10)
    resp.raise_for_status()
    return resp.json()


def run_pipeline(
    account_no: str,
    api_key: str,
    user_key: str,
    real_csv_path: str = None,   # None이면 더미 데이터 사용
    n_users: int = 30,
    dry_run: bool = True,         # True면 API 호출 없이 출력만
):
    print(f"[1] 거래 구조 합성 (n_users={n_users}) ...")
    base_df = synthesize_base(real_csv_path, n_users)

    print(f"[2] 한국 결제명 후처리 ...")
    records = postprocess(base_df, n_users)

    # 유저별 케이스 분포 출력
    from collections import Counter
    case_dist = Counter(r["user_case"] for r in records)
    print(f"    케이스 분포: {dict(sorted(case_dist.items()))}")

    # 총 출금액 계산 (계좌별 충전 금액 산정용)
    total_amount = sum(r["amount_krw"] for r in records)
    charge_amount = total_amount + 100_000  # 여유 버퍼

    print(f"[3] 입금 API - 초기잔액 충전 ({charge_amount:,}원) ...")
    deposit_req = to_deposit_request(charge_amount, account_no, api_key, user_key)

    if dry_run:
        import json
        print(f"    [DRY RUN] 입금 요청:\n    {json.dumps(deposit_req.model_dump(), default=vars, indent=2)}")
    else:
        result = call_deposit(deposit_req)
        print(f"    입금 완료: {result}")

    print(f"[4] 출금 API - 거래내역 {len(records)}건 삽입 ...")
    ok, fail = 0, 0
    for i, record in enumerate(records):
        withdrawal_req = to_withdrawal_request(record, account_no, api_key, user_key)
        if dry_run:
            if i < 3:  # 처음 3건만 출력
                print(f"    [DRY RUN] {record['merchant']} / {record['amount_krw']:,}원")
            elif i == 3:
                print(f"    ... 이하 {len(records)-3}건 생략")
        else:
            try:
                call_withdrawal(withdrawal_req)
                ok += 1
            except Exception as e:
                fail += 1
                print(f"    [FAIL] {record['merchant']}: {e}")

    if not dry_run:
        print(f"[완료] 성공 {ok}건 / 실패 {fail}건")
    else:
        print(f"[DRY RUN 완료] 실제 API 호출 없이 {len(records)}건 검증 완료")

    return records


# ──────────────────────────────────────────
# 실행 예시
# ──────────────────────────────────────────

if __name__ == "__main__":
    records = run_pipeline(
        account_no="0016174648358792",   # 테스트 계좌번호
        api_key="YOUR_API_KEY",
        user_key="YOUR_USER_KEY",
        real_csv_path=None,              # None → 더미 데이터
        n_users=30,
        dry_run=True,                    # False로 바꾸면 실제 API 호출
    )

    # 결과 샘플 출력
    print("\n=== 생성된 데이터 샘플 (케이스별 5건) ===")
    df_result = pd.DataFrame(records)
    for case in [1, 2, 3]:
        subset = df_result[df_result["user_case"] == case].head(5)
        print(f"\n[케이스 {case}]")
        print(subset[["user_id", "date", "category", "merchant", "amount_krw"]].to_string(index=False))
