package idnord.keycloak.utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.keycloak.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class KeycloakTestUtils {

    private final AtomicReference<Keycloak> kc = new AtomicReference<>();
    @Getter
    private final org.keycloak.admin.client.Keycloak adminClient;

    public KeycloakTestUtils() {
        this(new HashMap<String, String>());
    }

    public KeycloakTestUtils(Map<String, String> envs) {
        envs.putIfAbsent("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        envs.putIfAbsent("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
        try {
            new EnvironmentVariables(envs).execute(() -> {
                kc.set(Keycloak.builder().setVersion("26.2.5").addDependency("idnord.keycloak", "keycloak-spi-limit-resend-email", "0.1.1-SNAPSHOT").start());
            });
        } catch (Exception e) {
            System.out.println("Unable to start keycloak " + e.getMessage());
        }

        adminClient = KeycloakBuilder.builder().serverUrl("http://localhost:8080").realm("master").clientId("admin-cli").grantType("password").username("admin").password("admin").build();
    }

    public void tearDown() {
        try {
            kc.get().stop();
        } catch (TimeoutException e) {
            System.out.println("Unable to gracefully stop keycloak " + e.getMessage());
        }
    }

    public KeycloakTestUtils configureSmtpViaAdminClient(String mailServerHost, String mailServerPort) {
        // 1. Get the current realm representation
        RealmRepresentation realm = adminClient.realm("master").toRepresentation();

        // 2. Setup SMTP config map
        Map<String, String> smtpConfig = new HashMap<>();
        smtpConfig.put("host", mailServerHost);
        smtpConfig.put("port", mailServerPort);
        smtpConfig.put("from", "no-reply@example.com");
        smtpConfig.put("auth", "false");
        smtpConfig.put("starttls", "false");
        smtpConfig.put("ssl", "false");
        // Add username/password if needed:
        // smtpConfig.put("username", "smtp-user");
        // smtpConfig.put("password", "smtp-password");

        // 3. Set the SMTP config into the realm
        realm.setSmtpServer(smtpConfig);

        // 4. Update the realm with new SMTP settings
        adminClient.realm("master").update(realm);

        // Now the realm is configured to send emails via your fake SMTP server
        return this;
    }

    private List<String> getCurrentListeners(RealmRepresentation realm) {
        // Get current list or create a new one if null
        List<String> currentListeners = realm.getEventsListeners();
        if (currentListeners == null) {
            currentListeners = new ArrayList<>();
        }
        return currentListeners;
    }

    public KeycloakTestUtils addListenerToEvents() {
        RealmRepresentation beforeRealm = getAdminClient().realm("master").toRepresentation();
        List<String> currentListenersBeforeChange = getCurrentListeners(beforeRealm);
        System.out.println("BeforeAddedListenerToEvents=" + currentListenersBeforeChange);

        // Add your custom listener if it's not already there
        if (!currentListenersBeforeChange.contains("limit-resend-email-event")) {
            currentListenersBeforeChange.add("limit-resend-email-event");
            beforeRealm.setEventsListeners(currentListenersBeforeChange);

            // Apply the updated beforeRealm config
            getAdminClient().realm("master").update(beforeRealm);
        }

        RealmRepresentation afterRealm = getAdminClient().realm("master").toRepresentation();
        List<String> currentListenersAfterChange = getCurrentListeners(afterRealm);
        System.out.println("AfterAddedListenerToEvents=" + currentListenersAfterChange);

        return this;
    }


    public String getAttribute(String userId, String attributeName) {
        UserRepresentation userAfterSend = getAdminClient()
                .realm("master")
                .users()
                .get(userId)
                .toRepresentation();

        Map<String, List<String>> attributes = userAfterSend.getAttributes();
        System.out.println("attributes");
        String attributeValue = null;
        if (attributes != null) {
            System.out.println(attributes.keySet());

            List<String> resendCounter = attributes.get(attributeName);
            if (resendCounter != null && !resendCounter.isEmpty()) {
                System.out.println("Resend email count: " + resendCounter.getFirst());
                attributeValue = resendCounter.getFirst();
            }
        }
        return attributeValue;
    }

    public ComponentRepresentation getUserProfileComponent() {
        RealmResource realmResource = getAdminClient().realm("master");
        ComponentsResource componentsResource = realmResource.components();

        List<ComponentRepresentation> allComponents = componentsResource
                .query();

        for (ComponentRepresentation r : allComponents) {
            System.out.println(r.getParentId() + " " + r.getProviderType() + " " + r.getName());
        }

        // Filter components by type
        List<ComponentRepresentation> userProfileComponents = componentsResource
                .query(null, "org.keycloak.userprofile.UserProfileProvider");

        System.out.println("Found " + userProfileComponents.size() + " user profile components:");
        for (ComponentRepresentation comp : userProfileComponents) {
            System.out.println("Component ID: " + comp.getId());
            System.out.println("Name: " + comp.getName());
            System.out.println("Provider ID: " + comp.getProviderId());
            System.out.println("Config: " + comp.getConfig());
            System.out.println("-----------------------------------");
        }

        return userProfileComponents.getFirst();
    }

    public KeycloakTestUtils setUnmanagedAttributesPolicyToAdminOnly() {
        try {
            RealmResource realm = getAdminClient().realm("master");
            ComponentRepresentation userProfileComponent = getUserProfileComponent();
            // Extract and parse kc.user.profile.config
            String rawConfig = userProfileComponent.getConfig().get("kc.user.profile.config").get(0);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> configMap = mapper.readValue(rawConfig, new TypeReference<>() {
            });

            // Set unmanagedAttributePolicy
            configMap.put("unmanagedAttributePolicy", "ADMIN_VIEW");

            // Write it back to JSON and update component
            String updatedJson = mapper.writeValueAsString(configMap);
            userProfileComponent.getConfig().put("kc.user.profile.config", Collections.singletonList(updatedJson));
            realm.components().component(userProfileComponent.getId()).update(userProfileComponent);

            System.out.println("Updated unmanagedAttributePolicy to ADMIN_VIEW");

        } catch (Exception e) {
            throw new RuntimeException("Failed to update unmanagedAttributePolicy", e);
        }

        return this;
    }

    @NotNull
    public UserRepresentation getUserRepresentation(String username, String password, String email) {

        // 1. Create user with unverified email
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);

        // Create a CredentialRepresentation for password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);  // false if password is permanent

        // Set credentials list on the user
        user.setCredentials(Collections.singletonList(credential));

        user.setEmail(email);
        user.setEmailVerified(false);
        user.setEnabled(true);

        // Add "VERIFY_EMAIL" as required action
        user.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));
        return user;
    }

}
