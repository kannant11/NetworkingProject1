package smtpserver;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class MailQueue {

    private LinkedBlockingQueue<File> mailQueue; //mail queue

    private File mailMessage; //mail message {will assign later}
    
    //constructor creating a linked blocking queue
    public MailQueue() {
        //creates a linked blocking queue
        this.mailQueue = new LinkedBlockingQueue<>();
    }

    //getter receiving the linked blocking queue
    public LinkedBlockingQueue<File> getLinkedBlockingQueue() {
        return mailQueue;
    }

    //add new email message to front of queue
    public void addEmail(File newMailMessage) throws InterruptedException {

        mailQueue.add(mailMessage); //add message to end of queue for now
        
        LinkedBlockingQueue<File> tempMailQueue = new LinkedBlockingQueue<>(); //temporary mail queue to hold all current 
                                                                               //contents
                                                                               
        mailQueue.drainTo(tempMailQueue); //take out all contents of the temporary mail queue

        mailQueue.put(newMailMessage); //put the new mail message in the original mail queue for now (while all other contents are 
                                       //removed)
        
        //add the rest of the message back into the queue
        for(File mailMessage : tempMailQueue) {
            mailQueue.put(mailMessage);
        }

        //takes the email message from the front of the queue (that mail message gets removed from the queue)
        mailQueue.take();
    }
}