package idnord.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import idnord.keycloak.LimitResendEmailCore;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_MAX_RETRIES;
import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC;

@Slf4j
@AutoService(RequiredActionFactory.class)
public class CustomVerifyEmail extends VerifyEmail implements RequiredActionProvider, RequiredActionFactory {

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();
        if (LimitResendEmailCore.isLimitResendEmailReached(user, LIMIT_RESEND_EMAIL_MAX_RETRIES, LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC)) {
            String email = context.getUser().getEmail();
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail("email", email);
            event.clone().event(EventType.SEND_VERIFY_EMAIL).detail("reason", "Too many emails sent. Please wait before trying again.").user(user).error("email_send_failed");
            log.info("Email sending limited for username={}, clientId={}, IP={}.",
            user.getUsername(), context.getAuthenticationSession().getClient().getClientId(), context.getSession().getContext().getConnection().getRemoteAddr());

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

