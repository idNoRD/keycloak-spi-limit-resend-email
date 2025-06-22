package idnord.keycloak;

import org.keycloak.common.util.Time;
import org.keycloak.models.UserModel;

public class LimitResendEmailCore {

    public static final String ATTR_FOR_LIMIT_RESEND_EMAIL_COUNT = "LimitResendEmailCount";
    public static final String ATTR_FOR_LIMIT_RESEND_EMAIL_LAST_TIME = "LimitResendEmailLastTime";

    public static boolean isLimitResendEmailReached(UserModel user, final int LIMIT_RESEND_EMAIL_MAX_RETRIES, final int LIMIT_RESEND_EMAIL_BLOCK_DURATION_SECONDS) {

        int count = 0;
        int lastEmailSentTime = 0;

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
                lastEmailSentTime = Integer.parseInt(lastTimeAttr);
            }
        } catch (Exception ignored) {
        }

        if (count < LIMIT_RESEND_EMAIL_MAX_RETRIES) {
            // send emails immediately until MAX_RETRIES, then only one email after each BLOCK_DURATION is allowed (see reset logic in listener)
            return false;
        } else {
            int nowTimeInSeconds = Time.currentTime();
            // If last email was more than DURATION seconds ago allow sending email
            if (nowTimeInSeconds - lastEmailSentTime > LIMIT_RESEND_EMAIL_BLOCK_DURATION_SECONDS) {
                return false;
            } else {
                return true;
            }
        }
    }

}
