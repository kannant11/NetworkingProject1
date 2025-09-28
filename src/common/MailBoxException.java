package common;

public class MailBoxException extends Exception {
    public MailBoxException(String m) {
        super(m);
    }

    public MailBoxException(String m, Throwable t) {
        super(m, t);
    }
}
