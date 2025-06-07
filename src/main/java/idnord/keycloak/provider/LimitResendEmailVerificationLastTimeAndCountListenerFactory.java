package idnord.keycloak.provider;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(EventListenerProviderFactory.class)
@Slf4j
public class LimitResendEmailVerificationLastTimeAndCountListenerFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "resend-email-verification-last-time-and-count";

    static String attributeNameForTime;
    static String attributeNameForCount;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LimitResendEmailVerificationLastTimeAndCountListener(session);
    }

    @Override
    public void init(Config.Scope config) {
        // We use our own configuration as I don't want to mess around with XML from two thousand years ago
        attributeNameForTime = config.get("attribute-name-for-time", "LimitResendEmailVerificationLastTime");
        attributeNameForCount = config.get("attribute-name-for-count", "LimitResendEmailVerificationCount");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
