package idnord.keycloak.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.common.util.Time;
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
        //@TODO add Forgot Password action
        if (event.getType().equals(EventType.SEND_VERIFY_EMAIL)) {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user != null) {
                user.setSingleAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForTime, Integer.toString(Time.currentTime()));
                //
                String attributeNameForCount = LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForCount;
                int count = 0;
                try {
                    count = Integer.parseInt(user.getFirstAttribute(attributeNameForCount));
                } catch (Exception ignored) {
                }
                if (count < Integer.MAX_VALUE) {
                    user.setSingleAttribute(attributeNameForCount, Integer.toString(count + 1));
                }
                log.info("SEND_VERIFY_EMAIL triggered for user {}, count now: {}", user.getUsername(), count);
            }
        }

        // Reset counter on successful verification
        if (event.getType() == EventType.VERIFY_EMAIL) {
            UserModel user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user != null) {
                user.removeAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForCount);
                user.removeAttribute(LimitResendEmailLastTimeAndCountListenerFactory.attributeNameForTime);
                log.info("Email verified for user {}, resend count reset", user.getUsername());
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