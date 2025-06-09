package idnord.keycloak.authenticator;

import idnord.keycloak.LimitResendEmailCore;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_MAX_RETRIES;
import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC;

@Slf4j
public class LimitResendEmailAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (LimitResendEmailCore.isLimitResendEmailReached(user, LIMIT_RESEND_EMAIL_MAX_RETRIES, LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC)) {
            log.info("Email sending limited for username={}, clientId={}, IP={}.",
                    user.getUsername(), context.getAuthenticationSession().getClient().getClientId(), context.getSession().getContext().getConnection().getRemoteAddr());
            context.failure(AuthenticationFlowError.USER_TEMPORARILY_DISABLED);
            // @TODO a page with custom message
        } else {
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}

