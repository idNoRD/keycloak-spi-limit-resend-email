package idnord.keycloak.authenticator;

import idnord.keycloak.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@Slf4j
public class LimitResendEmailVerificationAuthenticator implements Authenticator {

    private static final String ATTR_FOR_LIMIT_RESEND_EMAIL_VERIFICATION_COUNT = "LimitResendEmailVerificationCount";
    private static final String ATTR_FOR_LIMIT_RESEND_EMAIL_VERIFICATION_LAST_TIME = "LimitResendEmailVerificationLastTime";
    private final int LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES;
    private final int LIMIT_RESEND_EMAIL_VERIFICATION_BLOCK_DURATION_SECONDS;

    public LimitResendEmailVerificationAuthenticator(Configuration configuration) {
        this.LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES = configuration.getLimitResendEmailVerificationMaxRetries();
        this.LIMIT_RESEND_EMAIL_VERIFICATION_BLOCK_DURATION_SECONDS = configuration.getLimitResendEmailVerificationRetryBlockDurationInSec();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        int count = 0;
        long lastTime = 0;

        try {
            String countAttr = user.getFirstAttribute(ATTR_FOR_LIMIT_RESEND_EMAIL_VERIFICATION_COUNT);
            if (countAttr != null) {
                count = Integer.parseInt(countAttr);
            }
        } catch (Exception ignored) {
        }

        try {
            String lastTimeAttr = user.getFirstAttribute(ATTR_FOR_LIMIT_RESEND_EMAIL_VERIFICATION_LAST_TIME);
            if (lastTimeAttr != null) {
                lastTime = Long.parseLong(lastTimeAttr);
            }
        } catch (Exception ignored) {
        }

        if (count < LIMIT_RESEND_EMAIL_VERIFICATION_MAX_RETRIES) {
            context.success();
        } else {
            long now = System.currentTimeMillis() / 1000; // current time in seconds
            // If last resend was more than 1 hour ago, reset count
            if (now - lastTime > LIMIT_RESEND_EMAIL_VERIFICATION_BLOCK_DURATION_SECONDS) {
                context.success();
            } else {
                context.failure(AuthenticationFlowError.USER_TEMPORARILY_DISABLED);
            }
        }
        // @TODO a page with custom message
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

