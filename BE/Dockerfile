# ==========================================
# 1단계: Build (Gradle로 빌드해서 jar 파일 생성)
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Gradle Wrapper 복사 + CRLF 처리 (Windows 환경 대응)
COPY gradlew gradlew
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 빌드 설정 복사 → 의존성 먼저 다운로드 (레이어 캐싱)
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 → 빌드 (소스만 바뀌면 여기부터만 재실행)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# plain jar 제외하고 실행 가능한 jar 선택
RUN JAR_FILE=$(ls build/libs/*.jar | grep -v 'plain' | head -n 1) && cp "$JAR_FILE" app.jar

# ==========================================
# 2단계: Run (jar만 가져와서 실행)
# ==========================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
