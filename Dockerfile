# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 캐시 효율을 위해 먼저 메타 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle* settings.gradle* ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies || true

# 소스 복사 후 빌드
COPY . .
RUN ./gradlew --no-daemon clean bootJar

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

# 10001 사용자/그룹 생성 (non-root)
RUN addgroup --system --gid 10001 app \
 && adduser  --system --uid 10001 --ingroup app app

# jar 복사
COPY --from=build /app/build/libs/*.jar /app/app.jar

# 소유권 정리
RUN chown -R 10001:10001 /app

# 숫자 UID/GID로 실행 (k8s runAsNonRoot 검증 OK)
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","/app/app.jar"]
