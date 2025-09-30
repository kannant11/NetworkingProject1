package smtpserver;

import common.*;
import maildirsupport.MailMessage;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class SmtpConnectionHandler implements Runnable {
    private enum State { NEED_HELO, READY_MAIL, RCPT_DATA, READING_DATA, CLOSE }
    private final Socket sock;
    private final String serverName;
    private final MailQueue queue;
    private final Logger log;

    public SmtpConnectionHandler(Socket sock, String serverName, MailQueue queue, Logger log) {
        this.sock = sock; this.serverName = serverName; this.queue = queue; this.log = log;
    }

    @Override public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            State st = State.NEED_HELO;
            MailMessage cur = null;

            send(out, "220 " + serverName + " SMTP tinysmtp");

            for (;;) {
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

                switch (st) {
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
                        if (U.startsWith("MAIL FROM:")) {
                            String addr = u.substring("MAIL FROM:".length()).trim();
                            addr = SmtpAddrUtil.extractAddrSpec(addr);
                            if (!addr.contains("@") || !addr.endsWith(serverName)) {
                                // Project spec: reject sender that doesn't match server name
                                send(out, "504 5.5.2 " + addr + ": Sender address rejected");
                            } else {
                                cur = new MailMessage();
                                cur.setSender(addr);
                                send(out, "250 Ok");
                                st = State.RCPT_DATA;
                            }
                        } else {
                            send(out, "503 Bad sequence of commands");
                        }
                    }
                    case RCPT_DATA -> {
                        if (U.startsWith("RCPT TO:")) {
                            String rcpt = u.substring("RCPT TO:".length()).trim();
                            if (!rcpt.contains("@")) {
                                send(out, "501 Syntax: improper syntax");
                            } else {
                                cur.addRecipient(rcpt);
                                send(out, "250 Ok");
                            }
                        } else if (U.equals("DATA")) {
                            if (cur == null || cur.getRecipients().isEmpty()) {
                                send(out, "503 Bad sequence of commands");
                            } else {
                                send(out, "354 End data with <CR><LF>.<CR><LF>");
                                st = State.READING_DATA;
                            }
                        } else {
                            send(out, "503 Bad sequence of commands");
                        }
                    }
                    case READING_DATA -> {
                        // Read until a line that is exactly "."
                        while (true) {
                            String bodyLine = in.readLine();
                            if (bodyLine == null) { st = State.CLOSE; break; }
                            if (bodyLine.equals(".")) break;
                            // Dot-stuffing not required by your brief; accept as-is
                            cur.appendBodyLine(bodyLine);
                        }
                        // Enqueue regardless; worker decides local delivery
                        queue.add(cur);
                        send(out, "250 Ok delivered message.");
                        cur = null;
                        st = State.READY_MAIL;
                    }
                    default -> send(out, "502 5.5.2 Error: command not recognized");
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
