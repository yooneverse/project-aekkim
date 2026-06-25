FROM mcr.microsoft.com/playwright/python:v1.58.0-jammy

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app/scraper \
    TZ=Asia/Seoul

WORKDIR /app

COPY requirment.txt ./requirment.txt

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirment.txt

COPY scraper ./scraper

WORKDIR /app/scraper

CMD ["python", "scheduler.py"]
