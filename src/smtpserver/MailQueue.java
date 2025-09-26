package smtpserver;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class MailQueue {

    private LinkedBlockingQueue<File> mailQueue; //mail queue

    private File mailMessage;
    
    //constructor creating a linked blocking queue
    public MailQueue() {
        //creates a linked blocking queue
        this.mailQueue = new LinkedBlockingQueue<>();
    }

    //getter receiving the linked blocking queue
    public LinkedBlockingQueue<File> getLinkedBlockingQueue() {
        return mailQueue;
    }

    public void addEmail() throws InterruptedException {
        //add mail message to queue
        mailQueue.put(mailMessage);
    }
}