package idnord.keycloak.provider;

import com.google.auto.service.AutoService;
import idnord.keycloak.LimitResendEmailCore;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_MAX_RETRIES;
import static idnord.keycloak.config.LimitResendEmailConfiguration.LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC;

@AutoService(EventListenerProviderFactory.class)
@Slf4j
public class LimitResendEmailLastTimeAndCountListenerFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "limit-resend-email-event";

    static String attributeNameForTime;
    static String attributeNameForCount;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LimitResendEmailLastTimeAndCountListener(session);
    }

    @Override
    public void init(Config.Scope config) {
        log.info("config loaded: KEYCLOAK_LIMIT_RESEND_EMAIL_MAX_RETRIES={}, KEYCLOAK_LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC={}",
                LIMIT_RESEND_EMAIL_MAX_RETRIES, LIMIT_RESEND_EMAIL_RETRY_BLOCK_DURATION_IN_SEC);
        attributeNameForTime = config.get("attribute-name-for-time", LimitResendEmailCore.ATTR_FOR_LIMIT_RESEND_EMAIL_LAST_TIME);
        attributeNameForCount = config.get("attribute-name-for-count", LimitResendEmailCore.ATTR_FOR_LIMIT_RESEND_EMAIL_COUNT);
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
