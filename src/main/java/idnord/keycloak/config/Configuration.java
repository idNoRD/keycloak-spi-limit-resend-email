package idnord.keycloak.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@RequiredArgsConstructor
public class Configuration {

    private final int limitResendEmailMaxRetries; // The maximum number of retries of email before blocking
    private final int limitResendEmailRetryBlockDurationInSec; // The base back-off time in seconds.

    /**
     * Loads the configuration using the systems environment variables
     *
     * @return The loaded configuration
     */
    public static Configuration loadFromEnv() {
        final int limitResendEmailMaxRetries = Integer.parseInt(Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_MAX_RETRIES")).orElse("3"));
        final int limitResendEmailRetryBlockDurationInSec = Integer.parseInt(Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC")).orElse("3600"));

        return new Configuration(
                limitResendEmailMaxRetries,
                limitResendEmailRetryBlockDurationInSec
        );
    }

}