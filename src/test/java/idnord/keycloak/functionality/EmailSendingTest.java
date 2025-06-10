package idnord.keycloak.functionality;

import idnord.keycloak.utilities.MailServerTestUtils;
import idnord.keycloak.utilities.KeycloakTestUtils;
import idnord.keycloak.utilities.UserBehaviorTestUtils;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EmailSendingTest {

    final int MAX_RETRIES = 3;

    static KeycloakTestUtils kc;
    static MailServerTestUtils ms;
    static UserBehaviorTestUtils ub;

    @BeforeAll
    static void setupContainer() {
        ms = new MailServerTestUtils();
        kc = new KeycloakTestUtils()
                .addListenerToEvents()
                .configureSmtpViaAdminClient(ms.getMailServerHost(), ms.getMailServerPort())
                .setUnmanagedAttributesPolicyToAdminOnly();
        ub = new UserBehaviorTestUtils();
    }

    @AfterAll
    static void tearDown() {
        ub.tearDown();
        kc.tearDown();
        ms.tearDown();
    }

    @Test
    void testLimitResendEmailAfterMaxRetries() throws IOException {
        String realm = "master";
        String username = "testuser";
        String password = "testuser";
        UserRepresentation user = kc.getUserRepresentation(username, password);

        Response response = kc.getAdminClient().realm(realm).users().create(user);
        assertEquals(201, response.getStatus());
        String userId = CreatedResponseUtil.getCreatedId(response);

        int expectedEmailSent = 0;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                ub.attemptLogin(username, password, "/login-actions/required-action?execution=VERIFY_EMAIL");
                expectedEmailSent += 1;

                int emailCountFromAttribute = Integer.parseInt(Optional.of(kc.getAttribute(userId, "LimitResendEmailCount")).orElse("0"));
                System.out.println("emailCountFromAttribute LimitResendEmailCount=" + emailCountFromAttribute);
                assertEquals(expectedEmailSent, emailCountFromAttribute);

                //System.out.println("Sleep5...");
                //Thread.sleep(Duration.ofSeconds(i));

                int emailCount = ms.getEmailCount().size();
                System.out.println("Emails received by fake SMTP: " + emailCount);

                // Example assertion - expect at least 1 email sent
                assertEquals(expectedEmailSent, emailCount, "Email should be sent on attempt " + (i + 1));
            } catch (Exception e) {
                expectedEmailSent = -1;
                System.out.println("ERROR: " + e.getMessage());
            }
        }

        int emailCountAfterMaxAttempts = ms.getEmailCount().size();
        for (int i = MAX_RETRIES; i < MAX_RETRIES + 3; i++) {
            try {
                ub.attemptLogin(username, password, "/login-actions/required-action?execution=VERIFY_EMAIL");

                int emailCountFromAttribute = Integer.parseInt(Optional.of(kc.getAttribute(userId, "LimitResendEmailCount")).orElse("0"));
                System.out.println("emailCountFromAttribute LimitResendEmailCount=" + emailCountFromAttribute);
                assertEquals(expectedEmailSent, emailCountFromAttribute);

                //System.out.println("Sleep5...");
                //Thread.sleep(Duration.ofSeconds(i));

                int emailCount = ms.getEmailCount().size();
                System.out.println("Emails received by fake SMTP: " + emailCount);
                assertEquals(emailCountAfterMaxAttempts, emailCount, "Email should NOT be sent on attempt " + (i + 1));
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }

    }

}
