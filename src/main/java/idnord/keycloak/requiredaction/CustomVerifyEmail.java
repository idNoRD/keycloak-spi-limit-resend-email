package idnord.keycloak.requiredaction;

import idnord.keycloak.LimitResendEmailCore;
import idnord.keycloak.config.Configuration;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

@Slf4j
public class CustomVerifyEmail extends VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    private Configuration configuration;

    @Override
    public void init(Config.Scope config) {
        this.configuration = Configuration.loadFromEnv();
        super.init(config);
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();
        if (LimitResendEmailCore.isLimitResendEmailReached(user, configuration.getLimitResendEmailMaxRetries(), configuration.getLimitResendEmailRetryBlockDurationInSec())) {
            log.info("requiredActionChallenge failure {}", user.getEmail());

            String email = context.getUser().getEmail();
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail("email", email);
            event.clone().event(EventType.SEND_VERIFY_EMAIL).detail("reason", "Too many emails sent. Please wait before trying again.").user(user).error("email_send_failed");
            log.warn("Failed to send verification email for user={}. Too many emails sent.", email);
            context.failure("emailSendErrorMessage");
            context.challenge(context.form().setError("emailSendErrorMessage", new Object[0]).createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        } else {
            super.requiredActionChallenge(context);
        }
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return super.getId();
    }

}

