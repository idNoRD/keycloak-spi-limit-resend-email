package idnord.keycloak.provider;

import idnord.keycloak.utilities.KeycloakTestUtils;
import org.junit.jupiter.api.*;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProvidersTest {

    static KeycloakTestUtils kc;

    @BeforeAll
    public static void setUp() {
        kc = new KeycloakTestUtils();
    }

    @AfterAll
    static void tearDown() {
        kc.tearDown();
    }

    @Test
    void providerLimitResendEmailAuthenticatorFactoryTest() {
        ServerInfoRepresentation serverInfoRepresentation = kc.getAdminClient().serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation eventsListener = providers.get("eventsListener");
        assertTrue(eventsListener.getProviders().containsKey("limit-resend-email-event"));
    }

    @Test
    void providerLimitResendEmailLastTimeAndCountListenerFactoryTest() {
        ServerInfoRepresentation serverInfoRepresentation = kc.getAdminClient().serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation authenticator = providers.get("authenticator");
        assertTrue(authenticator.getProviders().containsKey("limit-resend-email-authenticator"));
    }

    @Test
    void providerCustomVerifyEmailTest() {
        ServerInfoRepresentation serverInfoRepresentation = kc.getAdminClient().serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation requiredAction = providers.get("required-action");
        assertTrue(requiredAction.getProviders().containsKey("VERIFY_EMAIL"));
    }

}