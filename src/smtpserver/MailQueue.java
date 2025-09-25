package smtpserver;

import java.util.concurrent.LinkedBlockingQueue;

public class MailQueue {

    private LinkedBlockingQueue<String> mailQueue;
    
    //constructor creating a linked blocking queue
    public MailQueue() {
        //creates a linked blocking queue
        this.mailQueue = new LinkedBlockingQueue<>();
    }

    //getter receiving the linked blocking queue
    public LinkedBlockingQueue<String> getLinkedBlockingQueue() {
        return mailQueue;
    }
}