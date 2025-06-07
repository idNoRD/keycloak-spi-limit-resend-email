package idnord.keycloak.config;

import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
public class Configuration {

    private final int limitResendEmailVerificationMaxRetries; // The maximum number of retries of email verification before blocking
    private final int limitResendEmailVerificationRetryBlockDurationInSec; // The base back-off time in seconds.

    private Configuration(
            final int limitResendEmailVerificationMaxRetries,
            final int limitResendEmailVerificationRetryBlockDurationInSec
    ) {
        this.limitResendEmailVerificationMaxRetries = limitResendEmailVerificationMaxRetries;
        this.limitResendEmailVerificationRetryBlockDurationInSec = limitResendEmailVerificationRetryBlockDurationInSec;
    }

    /**
     * Loads the configuration using the systems environment variables
     *
     * @return The loaded configuration
     */
    public static Configuration loadFromEnv() {
        final int limitResendEmailVerificationMaxRetries = Integer.parseInt(Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES")).orElse("3"));
        final int limitResendEmailVerificationRetryBlockDurationInSec = Integer.parseInt(Optional.ofNullable(System.getenv("KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_RETRY_BLOCK_DURATION_IN_SEC")).orElse("3600"));

        return new Configuration(
                limitResendEmailVerificationMaxRetries,
                limitResendEmailVerificationRetryBlockDurationInSec
        );
    }

}