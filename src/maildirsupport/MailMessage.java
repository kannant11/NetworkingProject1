package maildirsupport;

import java.util.LinkedList;

public class MailMessage {

    private String message;
    private String sender;
    private LinkedList<String> recipients = new LinkedList<>();

    public MailMessage() {
    }

    public MailMessage(String msge, String sender, LinkedList<String> recipients) {
        this.message = msge;
        this.sender = sender;
        this.recipients = recipients;

    }

    public String getMessage() {
        return this.message;
    }

    public String getSender() {
        return this.sender;
    }

    public LinkedList<String> getRecipients() {
        return this.recipients;
    }

    public void setMessage(String msge) {
        this.message = msge;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setRecipients(LinkedList<String> recipients) {
        this.recipients = recipients;
    }


    public void addRecipient(String rcpt) {
        this.recipients.add(rcpt);
    }

    public void appendBodyLine(String line) {
        // Preserve \r\n protocol lines exactly:
        this.message += line + "\r\n";
    }

}