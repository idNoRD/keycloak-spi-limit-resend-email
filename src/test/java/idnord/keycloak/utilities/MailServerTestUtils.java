package idnord.keycloak.utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MailServerTestUtils {

    private final GenericContainer fakeSmtpServer;

    public MailServerTestUtils() {
        fakeSmtpServer = new GenericContainer<>("reachfive/fake-smtp-server")
                .withExposedPorts(1080, 1025)
                .withCommand("node", "index.js", "--headers");

        fakeSmtpServer.start();

        System.out.println("Fake SMTP server running at: "
                + fakeSmtpServer.getHost() + ":" + fakeSmtpServer.getMappedPort(1025));
        System.out.println("Fake SMTP UI at: http://"
                + fakeSmtpServer.getHost() + ":" + fakeSmtpServer.getMappedPort(1080) + "/api/emails");
    }

    public void tearDown() {
        if (fakeSmtpServer != null) {
            String tearingDownFakeSmtpServerPort = fakeSmtpServer.getMappedPort(1080).toString();
            System.out.println("[START] Tearing down Mail Server " + tearingDownFakeSmtpServerPort);
            fakeSmtpServer.stop();
            System.out.println("[END] Tearing down Mail Server " + tearingDownFakeSmtpServerPort);
        }
    }

    public List<EmailMessage> getEmailCount() throws IOException {

        String apiUrl = String.format("http://%s:%d/api/emails",
                fakeSmtpServer.getHost(), fakeSmtpServer.getMappedPort(1080));

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Failed to get emails from fake SMTP server, HTTP code: " + responseCode);
        }

        try (InputStream is = conn.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, new TypeReference<List<EmailMessage>>() {
            });
        }
    }

    public String getMailServerPort() {
        return fakeSmtpServer.getMappedPort(1025).toString();
    }

    public String getMailServerHost() {
        return fakeSmtpServer.getHost();
    }
}


