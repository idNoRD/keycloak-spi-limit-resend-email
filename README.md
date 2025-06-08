# keycloak-spi-limit-resend-email
Keycloak spi that 
- limits resend email verification 
([#192341](https://github.com/keycloak/keycloak/issues/19234), 
 [#24558](https://github.com/keycloak/keycloak/issues/24558))
- limits forgot password emails
([#24914](https://github.com/keycloak/keycloak/issues/24914), 
[#26182](https://github.com/keycloak/keycloak/issues/26182), 
[#16574](https://github.com/keycloak/keycloak/issues/16574))

## Notice:
This repository is under active development and not yet ready for production

---

## 🛠 Installation
### 1. Download a jar from [Releases](https://github.com/idNoRD/keycloak-spi-limit-resend-email/releases/latest)
### 2. Copy jar to /opt/keycloak/providers/
### 3. Rebuild Keycloak `/opt/keycloak/bin/kc.sh build`
### 4. Restart Keycloak
### 5. Adjust settings
```text
KEYCLOAK_LIMIT_RESEND_EMAIL_MAX_RETRIES = 3
KEYCLOAK_LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC = 3600
```
### 6. Configure Realm
- Open master realm and check that "Provider info" contains
  - eventsListener contains limit-resend-email-event
  - authenticator contains limit-resend-email-authenticator
  - required-action contains VERIFY_EMAIL
- Go to "Realm Settings" of your realm → Events → Event Listeners and add `limit-resend-email-event` and click Save.
- Go to "Authentication" → "Flows" tab and Duplicate the **"reset credentials"** flow and insert **"LimitResendEmail Authenticator"** (mark as **Required**). 
  - Reorder and Place the **"LimitResendEmail Authenticator"** above **"Send Reset Email"** so that it runs before "Send Reset Email".
- In your duplicated flow click Action → "Bind flow" to set it as the active **"Reset credentials flow"** .
- Go to "Authentication" → "Required Actions" tab → Make sure **Verify Email** is enabled and the **Default Action** is set to **On**.
### 7. Optional setting
To see attributes of a user in keycloak
- Realm settings
  - General
    - Unmanaged Attributes
      - set `Only administrators can view`

## ✨ Features

## Feature #1 (Forgot password protection):

- After user registration, the Email Verification page is shown with a "Resend" link.  
  The user clicks "Resend" more than 3 times but does not open or confirm any of the emails.
- **User then opens the Login page and clicks "Forgot password".**
- **(Problem we solve):** The user can abuse the system by repeatedly triggering "Forgot password", spamming password reset emails.
- **(Solution):** If more than 3 verification or reset emails were sent within the last hour and none were confirmed, an error is shown to block further emails.

## Feature #2 (Login protection):

- After registration, the user is shown the Email Verification page with a "Resend" link.  
  The user clicks "Resend" more than 3 times but does not confirm any of the emails.
- **User then opens the Login page and enters the correct username and password.**
- Keycloak redirects to the Verification page and sends a new email verification email.
- **(Problem we solve):** The user can repeatedly log in to trigger email-verification emails without confirming any, leading to spam.
- **(Solution):** If the limit is reached, the Verification page shows an error and no email is sent.

## Feature #3 (Email verification page protection)

- After registration, the user is shown the Email Verification page with a "Resend" link.  
  The user clicks "Resend" more than 3 times without confirming any emails.
- **(Problem we solve):** The user can trigger excessive email-verification messages by repeatedly clicking "Resend".
- **(Solution):** If the resend limit is reached, the page shows an error and no email is sent.

---

# Keycloak Custom SPI Extensions

This repository contains custom [Keycloak](https://www.keycloak.org/) Service Provider Interfaces (SPI) for:
- Custom **Authenticator** for blocking sending emails after many Forgot Password clicks 
- Custom **EventListener** for counting how many times user clicked Forgot Password or Resend verification email 
- Custom **VerifyEmail** that overrides "existing VerifyEmail Required Action" for blocking sending emails after many resend clicks or many logins with unverified email

These extensions are designed to enhance the login flow and event tracking features of Keycloak.

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
Build the JAR
```bash
mvn clean package
```
Ensure JAR includes necessary META-INF services
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
