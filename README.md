# keycloak-spi-limit-resend-email-verification
Keycloak spi that limits resend email verification https://github.com/keycloak/keycloak/issues/19234

## Configure
KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES = 3
KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_RETRY_BLOCK_DURATION_IN_SEC = 3600

## Dev

```text
mvn clean package shade:shade
```