package idnord.keycloak.provider;

import org.junit.jupiter.api.*;
import org.keycloak.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProvidersTest {

    static AtomicReference<Keycloak> kc = new AtomicReference<>();
    static org.keycloak.admin.client.Keycloak adminClient;

    @BeforeAll
    public static void setUp() throws Exception {
        new EnvironmentVariables(
                "KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin"
        ).execute(() -> {
            kc.set(Keycloak.builder()
                    .setVersion("26.2.5")
                    .addDependency("idnord.keycloak", "keycloak-spi-limit-resend-email", "0.1.1-SNAPSHOT")
                    .start());
        });

        adminClient = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080")
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                .username("admin")
                .password("admin")
                .build();
    }

    @AfterAll
    static void tearDown() throws TimeoutException {
        kc.get().stop();
    }

    @Test
    void providerLimitResendEmailAuthenticatorFactoryTest() {
        ServerInfoRepresentation serverInfoRepresentation = adminClient.serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation eventsListener = providers.get("eventsListener");
        assertTrue(eventsListener.getProviders().containsKey("limit-resend-email-event"));
    }

    @Test
    void providerLimitResendEmailLastTimeAndCountListenerFactoryTest() {
        ServerInfoRepresentation serverInfoRepresentation = adminClient.serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation authenticator = providers.get("authenticator");
        assertTrue(authenticator.getProviders().containsKey("limit-resend-email-authenticator"));
    }

    @Test
    void providerCustomVerifyEmailTest() {
        ServerInfoRepresentation serverInfoRepresentation = adminClient.serverInfo().getInfo();
        Map<String, SpiInfoRepresentation> providers = serverInfoRepresentation.getProviders();
        SpiInfoRepresentation requiredAction = providers.get("required-action");
        assertTrue(requiredAction.getProviders().containsKey("VERIFY_EMAIL"));
    }

}