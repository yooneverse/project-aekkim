# SSAFY 금융망

### 카트 결제

- 설명: 조회한 가맹점에서 카드 결제합니다.

- HTTP URL (HTTP Method: POST)
```
https://finance-api.example.com/v1/credit-card/transactions
```

- 요청 메시지 명세

|변수명|설명|TYPE|필수|비고|
|:---|:---|:---|:---|:---|
|Header|공통|||Y|
|cardNo|카드번호|String|16|Y|
|cvc|카드보안코드|String|3|Y|
|merchantid|가맹점ID|Long||Y|
|paymentBalance|거래금액|Long||Y|

- 요청 메시지 형태
```
{
    "Header": {
        "apiName": "createCreditCardTransaction",
        "transmissionDate": "20240408",
        "transmissionTime": "135600",
        "institutionCode": "00100",
        "fintechAppNo": "001",
        "apiServiceCode": "createCreditCardTransaction",
        "institutionTransactionUniqueNo": "20240215121212123571",
        "apiKey": "MASKED_API_KEY",
        "userKey": "MASKED_USER_KEY"
    },
    "cardNo": "MASKED_CARD_NO",
    "cvc": "MASKED_CVC",
    "merchantId": "1",
    "paymentBalance": "500000"
}
```

---
