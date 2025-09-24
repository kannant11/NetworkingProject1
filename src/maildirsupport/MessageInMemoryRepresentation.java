package maildirsupport;
import java.util.LinkedList;

public class MessageInMemoryRepresentation{

    private String message;
    private String sender;
    private LinkedList<String> recipients;

    MessageInMemoryRepresentation(String msge, String id, LinkedList<String> recipients){

        this.message = msge;
        this.sender = id;
        this.recipients = recipients;

    }

    public String getMessage(){
        return this.message;
    }

    public String getSender(){
        return this.sender;
    }

    public LinkedList<String> getRecipients(){
        return this.recipients;
    }

    public void setMessage(String msge){
        this.message = msge;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public void setRecipients(LinkedList<String> recipients){
        this.recipients = recipients;
    }

}