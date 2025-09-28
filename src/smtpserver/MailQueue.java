package smtpserver;

import maildirsupport.MailMessage;

import java.util.concurrent.LinkedBlockingQueue;

public class MailQueue {

    private LinkedBlockingQueue<MailMessage> mailQueue; //mail queue

    public MailQueue() {
        this.mailQueue = new LinkedBlockingQueue<>();
    }

    //getter receiving the linked blocking queue
    public LinkedBlockingQueue<MailMessage> getLinkedBlockingQueue() {
        return mailQueue;
    }

    public void add(MailMessage m) {
        mailQueue.add(m);
    }

    public MailMessage take() throws InterruptedException {
        return mailQueue.take();
    }
}