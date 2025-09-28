package smtpserver;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.LinkedList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class SMTPServer {
    
    LinkedList<Path> Mailboxes = new LinkedList<>(
        java.util.Arrays.asList(Paths.get("maildirsupport/mail/adamroche"), Paths.get("maildirsupport/mail/joeywilliams"), Paths.get("maildirsupport/mail/nickjohnson"))
    );

    public static void main(String[] args){

        ExecutorService pool = Executors.newFixedThreadPool(10);

        try{

            ServerSocket SMTPServer = new ServerSocket(5000);

            while (true){

                Socket sock = SMTPServer.accept();

                pool.execute(new MailQueueThread(sock));

            }

        }

    }

}
