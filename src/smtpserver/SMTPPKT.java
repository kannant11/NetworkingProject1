package smtpserver;

import maildirsupport.MailMessage;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.LinkedList;
import java.io.File;

public class SMTPPKT implements Runnable {
    private enum State {NEED_HELO, READY_MAIL, RCPT_DATA, READING_DATA, CLOSE}

    private final Socket sock;
    private final String serverName;
    private final MailQueue queue;
    private final Logger log;

    public SMTPPKT(Socket sock, String serverName, MailQueue queue, Logger log) {
        this.sock = sock;
        this.serverName = serverName;
        this.queue = queue;
        this.log = log;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            State st = State.NEED_HELO;
            MailMessage cur = null;

            send(out, "220 " + serverName + " SMTP tinysmtp");
            for (; ; ) {
                String line = in.readLine(); // lines already stripped of \r\n by Reader
                if (line == null) break;
                log.info("c: " + line);

                String u = line.trim();
                String U = u.toUpperCase();
                if (U.startsWith("QUIT")) {
                    send(out, "221 Bye");
                    break;
                } else if (U.startsWith("NOOP")) {
                    send(out, "250 Ok");
                    continue;
                } else if (U.startsWith("RSET")) {
                    if (st == State.READY_MAIL || st == State.RCPT_DATA) {
                        cur = null;
                        st = State.READY_MAIL;
                        send(out, "250 Ok");
                    } else {
                        send(out, "503 Bad sequence of commands");
                    }
                    continue;
                }

                switch(st) {
                    case NEED_HELO -> {
                        if (U.startsWith("EHLO")) {
                            String clientIp = sock.getInetAddress().getHostAddress();
                            // EHLO requires a 250-multiline; last line must start with "250 "
                            send(out, "250-" + serverName + " greets " + clientIp);
                            send(out, "250 HELP");
                            st = State.READY_MAIL;
                        } else if (U.startsWith("HELO")) {
                            send(out, "250 <" + sock.getInetAddress().getHostAddress() + ">, I am glad to meet you.");
                            st = State.READY_MAIL;
                        } else {
                            send(out, "503 Bad sequence of commands");
                        }
                    }

                    case READY_MAIL -> {

                        if(U.startsWith("AMIL")){
                            send(out, "MAIL FROM: " + queue.getLinkedBlockingQueue().peek().getSender() + "@wonderland");
                            st = State.RCPT_DATA;
                        }

                        else if(U.startsWith("MAIL")){
                            send(out, "250 OK");
                            st = State.RCPT_DATA;
                        }
                        // MailMessage mail = queue.take();
                        // LinkedList<String> recp = mail.getRecipients();
                        else {
                            send(out, "503 Bad sequence of commands");
                        }

                    }

                    case RCPT_DATA -> {

                        if(U.startsWith("CRPT")){

                            MailMessage mail = queue.take();
                            LinkedList<String> recp = mail.getRecipients();

                            for(int i = 0; i < recp.size(); i++){

                                send(out, "RCPT TO: " + recp.get(i) + "@wonderland");

                            }

                            st = State.READING_DATA;

                        }

                        else if(U.startsWith("RCPT")){

                            send(out, "250 OK");

                            st = State.READING_DATA;

                        }

                        else {
                            send(out, "503 Bad sequence of commands");
                        }

                    }

                    case READING_DATA -> {

                        if (U.startsWith("ADTA")){

                            send(out, "DATA");

                            st = State.CLOSE;

                        }

                        else if (U.startsWith("DATA")){

                            send(out, "354 End data with <CR><LR>. <CR><LR>");
                            send(out, "250 Ok delivered message");

                            st = State.CLOSE;

                        }

                        else {
                            send(out, "503 Bad sequence of commands");
                        }

                    }

                case CLOSE -> {
                    
                    break;
                    
                }

                    }

                }
            }
        } catch (IOException e) {
            // log & close
            e.printStackTrace();
        }
    }

    private void send(BufferedWriter out, String s) throws IOException {
        String line = s + "\r\n";
        out.write(line);
        out.flush();
        log.info("s: " + s);
    }

}


