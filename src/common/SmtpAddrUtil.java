package common;

public class SmtpAddrUtil {

    public static String extractAddrSpec(String rawAfterCmd) {
        // Trim and strip ESMTP params after the mailbox if present (we only care about the mailbox)
        String s = rawAfterCmd.trim();

        // If angle brackets exist, take exactly what's inside <...>
        int lt = s.indexOf('<');
        int gt = (lt >= 0) ? s.indexOf('>', lt + 1) : -1;
        if (lt >= 0 && gt > lt) {
            s = s.substring(lt + 1, gt).trim();
        } else {
            // No brackets: take first token up to whitespace (before any params)
            int ws = s.indexOf(' ');
            s = (ws >= 0) ? s.substring(0, ws) : s;
        }
        return s;
    }

}
