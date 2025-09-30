package smtpserver;

import maildirsupport.MailMessage;

import java.util.concurrent.LinkedBlockingQueue;

public class MailQueue {

    private LinkedBlockingQueue<MailMessage> mailQueue; //mail queue

    //mail queue constructed as linked blocking queue
    public MailQueue() {
        this.mailQueue = new LinkedBlockingQueue<>();
    }

    //getter receiving the linked blocking queue
    public LinkedBlockingQueue<MailMessage> getLinkedBlockingQueue() {
        return mailQueue;
    }

    //add mail message to the queue (in front/head of queue)
    public void add(MailMessage m) {
        LinkedBlockingQueue<MailMessage> tempQueue = new LinkedBlockingQueue<>(); //temporary queue

        mailQueue.drainTo(tempQueue); //all messages in original mail queue transferred to temporary mail queue

        mailQueue.add(m); //message will be added to queue (end up in front because messages previously in queue moved to the
                          //temporary queue)

        mailQueue.addAll(tempQueue); //messages in temporary queue moved back into the original queue
    }

    //take the mail message from the front of the queue
    public MailMessage take() throws InterruptedException {
        return mailQueue.take();
    }
}