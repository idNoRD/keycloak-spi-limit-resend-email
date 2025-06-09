package idnord.keycloak.authenticator;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@Slf4j
@AutoService(AuthenticatorFactory.class)
public class LimitResendEmailAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "limit-resend-email-authenticator";

    private static final Authenticator SINGLETON = new LimitResendEmailAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getDisplayType() {
        return "LimitResendEmail Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return ""; // just a check in a flow
    }

    @Override
    public boolean isConfigurable() {
        return false; //@TODO for now we can load config from environment variables and then think about Admin UI config
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Blocks keycloak from sending emails if attempts like forgot password and resend email verify exceed a configured limit.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
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
