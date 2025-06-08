package idnord.keycloak.authenticator;

import idnord.keycloak.LimitResendEmailCore;
import idnord.keycloak.config.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@Slf4j
@RequiredArgsConstructor
public class LimitResendEmailAuthenticator implements Authenticator {

    private final Configuration configuration;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (LimitResendEmailCore.isLimitResendEmailReached(user, configuration.getLimitResendEmailMaxRetries(), configuration.getLimitResendEmailRetryBlockDurationInSec())) {
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

