package idnord.keycloak.utilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class EmailMessage {
    public String subject;
    public String text;
    public String html;
    public Map<String, Object> headers;
}
