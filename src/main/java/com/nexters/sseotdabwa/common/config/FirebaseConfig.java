package com.nexters.sseotdabwa.common.config;

import java.io.FileInputStream;
import java.io.InputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    /**
     * enabled=true 인 경우에만 FirebaseApp을 초기화한다.
     * enabled=false면 FirebaseApp Bean이 없고, NoopFcmSender가 동작
     */
    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties props) throws Exception {
        if (props.serviceAccountKeyPath() == null || props.serviceAccountKeyPath().isBlank()) {
            throw new IllegalStateException("firebase.service-account-key-path is required");
        }
        if (props.projectId() == null || props.projectId().isBlank()) {
            throw new IllegalStateException("firebase.project-id is required");
        }

        try (InputStream is = new FileInputStream(props.serviceAccountKeyPath())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(props.projectId())
                    .build();

            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp already initialized. Reusing instance.");
                return FirebaseApp.getInstance();
            }

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp initialized. projectId={}", props.projectId());
            return app;
        }
    }
}
