package idnord.keycloak;

import org.keycloak.models.UserModel;

public class LimitResendEmailCore {

    public static final String ATTR_FOR_LIMIT_RESEND_EMAIL_COUNT = "LimitResendEmailCount";
    public static final String ATTR_FOR_LIMIT_RESEND_EMAIL_LAST_TIME = "LimitResendEmailLastTime";

    public static boolean isLimitResendEmailReached(UserModel user, final int LIMIT_RESEND_EMAIL_MAX_RETRIES, final int LIMIT_RESEND_EMAIL_BLOCK_DURATION_SECONDS) {

        int count = 0;
        long lastTime = 0;

        try {
            String countAttr = user.getFirstAttribute(LimitResendEmailCore.ATTR_FOR_LIMIT_RESEND_EMAIL_COUNT);
            if (countAttr != null) {
                count = Integer.parseInt(countAttr);
            }
        } catch (Exception ignored) {
        }

        try {
            String lastTimeAttr = user.getFirstAttribute(LimitResendEmailCore.ATTR_FOR_LIMIT_RESEND_EMAIL_LAST_TIME);
            if (lastTimeAttr != null) {
                lastTime = Long.parseLong(lastTimeAttr);
            }
        } catch (Exception ignored) {
        }

        if (count < LIMIT_RESEND_EMAIL_MAX_RETRIES) {
            return false;
        } else {
            long now = System.currentTimeMillis() / 1000; // current time in seconds
            // If last resend was more than X sec ago, reset count
            if (now - lastTime > LIMIT_RESEND_EMAIL_BLOCK_DURATION_SECONDS) {
                return false;
            } else {
                return true;
            }
        }
    }

}
