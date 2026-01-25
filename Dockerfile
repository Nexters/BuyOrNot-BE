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

# ---------- run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# timezone
ENV TZ=Asia/Seoul
RUN addgroup --system app && adduser --system --ingroup app app

# jar 복사 (bootJar 결과물)
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

# 기본값은 dev로 두되, K8s에서 env로 덮어씀
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","/app/app.jar"]
