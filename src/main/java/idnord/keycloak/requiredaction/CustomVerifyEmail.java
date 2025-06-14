package idnord.keycloak.requiredaction;

import com.google.auto.service.AutoService;
import idnord.keycloak.LimitResendEmailCore;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.VerifyEmail;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_MAX_RETRIES;
import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC;

@Slf4j
@AutoService(RequiredActionFactory.class)
public class CustomVerifyEmail extends VerifyEmail implements RequiredActionProvider, RequiredActionFactory {

    /**
     * Applies protection logic before displaying a page that automatically triggers an email.
     * For example, after a successful login, the verification page should not be shown
     * if the email send limit has already been reached.
     */
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();
        if (LimitResendEmailCore.isLimitResendEmailReached(user, LIMIT_RESEND_EMAIL_MAX_RETRIES, LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC)) {
            handleReachedLimit(context, user);
        } else {
            super.requiredActionChallenge(context);
        }
    }

    /**
     * Applies protection logic after the user clicks "Resend Verification Email".
     * For example, if a resend limit has been reached, the page with the resend link should not be displayed.
     */
    @Override
    public void processAction(RequiredActionContext context) {
        UserModel user = context.getUser();
        if (LimitResendEmailCore.isLimitResendEmailReached(user, LIMIT_RESEND_EMAIL_MAX_RETRIES, LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC)) {
            handleReachedLimit(context, user);
        } else {
            super.processAction(context);
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

    private void handleReachedLimit(RequiredActionContext context, UserModel user) {
        context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL)
                .detail(Details.REASON, "Too many emails sent. Please wait before trying again.")
                .user(user)
                .error(Errors.EMAIL_SEND_FAILED);

        log.info("Email sending limited for username={}, clientId={}, IP={}.",
                user.getUsername(), context.getAuthenticationSession().getClient().getClientId(), context.getSession().getContext().getConnection().getRemoteAddr());

        context.challenge(
                context.form()
                        .setError(user.isEmailVerified() ? Messages.EMAIL_SENT_ERROR : Messages.VERIFY_EMAIL)
                        .createErrorPage(Response.Status.TOO_MANY_REQUESTS)
        );
    }

}

