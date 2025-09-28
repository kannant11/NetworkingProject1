package smtpserver;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class MailQueueThread {

    LinkedBlockingQueue<File> mailQueueCopy; //copy of mail queue

    //mail queue thread constructor to take copy of mail queue
    public MailQueueThread(MailQueue mailQueue) {
        this.mailQueueCopy = mailQueue.getLinkedBlockingQueue(); //copy of mail queue taken
    }

    public void run() {
        for(int i = 0; i < mailQueueCopy.size(); i++) {
            //get mail message from front of queue, throw exception if there is error
            try {
                //get mail message from front of queue
                mailQueueCopy.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}