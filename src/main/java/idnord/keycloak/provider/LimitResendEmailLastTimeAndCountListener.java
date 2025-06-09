package idnord.keycloak.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

@Slf4j
@RequiredArgsConstructor
public class LimitResendEmailLastTimeAndCountListener implements EventListenerProvider {

    private final KeycloakSession session;

    @Override
    public void onEvent(Event event) {
        if (EventType.SEND_VERIFY_EMAIL == event.getType() || EventType.SEND_RESET_PASSWORD == event.getType()) {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user != null) {
                user.setSingleAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForTime, Integer.toString(Time.currentTime()));
                // Increase counter on each sent email
                String attributeNameForCount = LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForCount;
                int emailsSentCount = 0;
                try {
                    emailsSentCount = Integer.parseInt(user.getFirstAttribute(attributeNameForCount));
                } catch (Exception ignored) {
                }
                if (emailsSentCount < Integer.MAX_VALUE) {
                    user.setSingleAttribute(attributeNameForCount, Integer.toString(emailsSentCount + 1));
                }
                log.debug("Event={} triggered for username={}, emailsSentCount={}", event.getType(), user.getUsername(), emailsSentCount);
            }
        } else if (EventType.VERIFY_EMAIL == event.getType()
                || (EventType.UPDATE_CREDENTIAL == event.getType() && "password".equalsIgnoreCase(event.getDetails().get(Details.CREDENTIAL_TYPE)))) {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user != null) {
                // Reset counter on successful email verification or successful password reset
                user.removeAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForCount);
                user.removeAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForTime);
                log.debug("Email verified for username={}, count reset", user.getUsername());
            }
        }

    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }

}