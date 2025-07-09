package idnord.keycloak;

import org.keycloak.Keycloak;

// KeycloakTestUtils is based on this approach:

/**
 * <p>Run this application from your IDE passing any CLI arguments. For instance, start-dev.
 * Make sure to add the following system properties before running:
 * <p>
 * * -Djava.util.logging.manager=org.jboss.logmanager.LogManager
 * * -Djava.util.concurrent.ForkJoinPool.common.threadFactory=io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory
 *
 * <p>The custom provider is added as a dependency and you should be able to debug within the
 * same JVM as the server.</p>
public class KeycloakServer {

    public static void main(String[] args) {
        Keycloak kc = Keycloak.builder()
                .setVersion("26.3.1")
                .addDependency("idnord.keycloak", "keycloak-spi-limit-resend-email", "0.1.1-SNAPSHOT")
                .start(args);
    }
}
*/