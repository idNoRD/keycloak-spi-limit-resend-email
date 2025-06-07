# keycloak-spi-limit-resend-email-verification
Keycloak spi that limits resend email verification https://github.com/keycloak/keycloak/issues/19234

## Notice:
This repository is under active development and not yet ready for production

# Keycloak Custom SPI Extensions

This repository contains custom [Keycloak](https://www.keycloak.org/) Service Provider Interfaces (SPI) for:
- Custom **Authenticator**
- Custom **EventListener**

These extensions are designed to enhance the login flow and event tracking features of Keycloak.

---

## ✨ Features

- 🔐 Block excessive email verification resends with time-based rate limiting
- 🛠 Track and reset resend counts upon successful verification
- 📢 Listen to Keycloak events (e.g., `SEND_VERIFY_EMAIL`, `VERIFY_EMAIL`) and persist user metadata
- ♻️ Integrated with Keycloak's flexible authentication flow system

---

## 🛠 Installation

### 1. Build the JAR
```text
mvn clean package
```

### 2. Ensure JAR includes necessary META-INF services
```bash
jar tf target/idnord.keycloak-keycloak-spi-limit-resend-email-verification.jar | grep META-INF/services/org.keycloak.
```
Expected services:
```text
META-INF/services/org.keycloak.authentication.AuthenticatorFactory  
META-INF/services/org.keycloak.events.EventListenerProviderFactory  
```
### 3. Copy jar to /opt/keycloak/providers/
### 4. Rebuild Keycloak
### 5. Restart Keycloak
### 6. Adjust settings
```text
KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES = 3
KEYCLOAK_LIMIT_RESEND_EMAIL_VERIFICATION_RETRY_BLOCK_DURATION_IN_SEC = 3600
```

## Development Notes
Destroy local keycloak
```bash
docker compose rm -f -s -v keycloak
```
Spin up local keycloak
```bash
 docker compose up
```
Upload spi into local keycloak, build and restart
```bash
./install.sh
```

# License
MIT