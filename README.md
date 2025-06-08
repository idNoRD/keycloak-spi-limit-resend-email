# keycloak-spi-limit-resend-email
Keycloak spi that limits resend email verification and forgot password emails https://github.com/keycloak/keycloak/issues/19234

## Notice:
This repository is under active development and not yet ready for production

## ✨ Features

## Feature #1 (Forgot password protection):
- After user registration we show Email confirmation page with Resend link, User clicks Resend link more than 5 times, User didn't open any emails and didn't confirm his email
- **User opens Login and clicks "Forgot password"**
- (Problem that we solve): User can spam Forgot password emails by clicking Forgot password
- (Solution) We show error because we sent more than 5 (email-verification emails that weren't confirmed or password reset emails) during last hour
- (Configuration): 
  - "Reset credentials flow" copied and "LimitResendEmail Authenticator" with "Required" was inserted before "Send Resend Email"
  - Open copy of "Reset credentials flow" and bind with "Reset credentials flow"
  - Realm Settings -> Events -> Event listeners -> add limit-resend-email-event
## Feature #2 (Login protection):
- After user registration we show Email confirmation page with Resend link, User clicks Resend link more than 5 times, User didn't open any emails and didn't confirm his email
- **User opens Login and enters correct login and password**
- Keycloak opens Verification Page and sends Email-verification email
- (Problem that we solve): User can spam Email-verification emails by logging-in without email confirmations
- (Solution): Verification email page shows error if limit was reached
- (Configuration):
  - Realm Settings -> Events -> limit-resend-email-event
  - Authentication -> Required actions -> Ensure that Verify Email is enabled and set default action is On

## Feature #3 (Email verification page protection)
- After user registration we show Email confirmation page with Resend link, User clicks Resend link more than 5 times, User didn't open any emails and didn't confirm his email
- (Problem that we solve): User can spam Email-verification emails by clicking Resend link many times
- (Solution): Verification email page shows error if limit was reached
- (Configuration): 
  - Realm Settings -> Events -> limit-resend-email-event
  - Authentication -> Required actions -> Ensure that Verify Email is enabled and set default action is On

---

# Keycloak Custom SPI Extensions

This repository contains custom [Keycloak](https://www.keycloak.org/) Service Provider Interfaces (SPI) for:
- Custom **Authenticator** for blocking sending emails after many Forgot Password clicks 
- Custom **EventListener** for counting how many times user clicked Forgot Password or Resend verification email 
- Custom **VerifyEmail** that overrides "existing VerifyEmail Required Action" for blocking sending emails after many resend clicks or many logins with unverified email

These extensions are designed to enhance the login flow and event tracking features of Keycloak.

---

## 🛠 Installation

### 1. Build the JAR
```bash
mvn clean package
```

### 2. Ensure JAR includes necessary META-INF services
```bash
jar tf target/idnord.keycloak-keycloak-spi-limit-resend-email.jar | grep META-INF/services/
```
Expected services:  
(IMPORTANT: if you see more than this list of services in jar it means that a dependency in pom.xml needs scope `<scope>provided</scope>`)
```text
META-INF/services/
META-INF/services/org.keycloak.authentication.AuthenticatorFactory
META-INF/services/org.keycloak.authentication.RequiredActionFactory
META-INF/services/org.keycloak.events.EventListenerProviderFactory
```
### 3. Copy jar to /opt/keycloak/providers/
### 4. Rebuild Keycloak
### 5. Restart Keycloak
### 6. Adjust settings
```text
KEYCLOAK_LIMIT_RESEND_EMAIL_MAX_RETRIES = 3
KEYCLOAK_LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC = 3600
```

---

## Development Notes
Destroy local keycloak
```bash
docker compose rm -f -s -v keycloak
```
Spin up local keycloak
```bash
 docker compose up
```
```bash
mvn clean package
```
```bash
jar tf target/idnord.keycloak-keycloak-spi-limit-resend-email.jar | grep META-INF/services/
```
Upload spi into local keycloak, build and restart
```bash
./install.sh
```

To see attributes of a user in keycloak 
- Realm settings
  - General
    - Unmanaged Attributes
      - set `Only administrators can view`

---

# License
MIT
