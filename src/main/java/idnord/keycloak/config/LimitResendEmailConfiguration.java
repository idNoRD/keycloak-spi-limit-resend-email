package idnord.keycloak.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LimitResendEmailConfiguration {

    public static final int LIMIT_RESEND_EMAIL_MAX_RETRIES;
    public static final int LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC;

    static {
        LIMIT_RESEND_EMAIL_MAX_RETRIES = Integer.parseInt(
                Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_MAX_RETRIES")).orElse("3")
        );

        LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC = Integer.parseInt(
                Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC")).orElse("3600")
        );
    }

    // Prevent instantiation
    private LimitResendEmailConfiguration() {}
}
