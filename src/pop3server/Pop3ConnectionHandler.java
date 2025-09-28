package pop3server;

import common.*;
import maildirsupport.MailboxInMemoryRepresentation;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Pop3ConnectionHandler implements Runnable {
    private enum State {AUTHORIZATION, TRANSACTION, UPDATE, CLOSE}

    private final Socket sock;
    private final Pop3Config cfg;
    private final AccountsDb accounts;
    private final Logger log;

    public Pop3ConnectionHandler(Socket sock, Pop3Config cfg, AccountsDb accounts, Logger log) {
        this.sock = sock;
        this.cfg = cfg;
        this.accounts = accounts;
        this.log = log;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            State st = State.AUTHORIZATION;
            Account acc = null;
            MailboxInMemoryRepresentation mb = null;

            send(out, "+OK welcome to " + cfg.server_name + " tinypop3 server.");

            for (; ; ) {
                String line = in.readLine();
                if (line == null) break;
                log.info("c: " + line);
                String[] parts = line.trim().split("\\s+");
                String cmd = parts[0].toUpperCase();

                if ("QUIT".equals(cmd)) {
                    if (st == State.TRANSACTION && mb != null) {
                        // enter UPDATE: delete marked, then bye
                        try {
                            mb.deleteMarked();
                        } catch (Exception ignore) {
                        }
                    }
                    send(out, "+OK bye");
                    break;
                }

                switch (st) {
                    case AUTHORIZATION -> {
                        if ("USER".equals(cmd) && parts.length == 2) {
                            String u = parts[1];
                            acc = accounts.get(u);
                            if (acc != null) send(out, "+OK Hello " + u);
                            else send(out, "-ERR no such user");
                        } else if ("PASS".equals(cmd) && parts.length == 2) {
                            if (acc == null) {
                                send(out, "-ERR USER required first");
                                break;
                            }
                            String pw = parts[1];
                            if (!acc.pass().equals(pw)) {
                                send(out, "-ERR invalid credentials");
                                acc = null;
                                break;
                            }
                            // load mailbox
                            Path spoolRoot = Paths.get(acc.spool());
                            mb = new MailboxInMemoryRepresentation(spoolRoot, acc.username());
                            mb.loadNew();
                            send(out, "+OK authenticated user.");
                            st = State.TRANSACTION;
//                        } else if ("CAPA".equals(cmd)) {
//                            send(out, "+OK Capability list follows");
//                            send(out, "TOP");
//                            send(out, "UIDL");
//                            send(out, ".");
                        } else {
                            send(out, "-ERR invalid in AUTHORIZATION");
                        }
                    }
                    case TRANSACTION -> {
                        switch (cmd) {
                            case "STAT" -> {
                                int count = mb.messageCount();
                                long octets = mb.totalSizeBytes();
                                send(out, "+OK " + count + " " + octets);
                            }
                            case "UIDL" -> {
                                if (parts.length == 1) {
                                    send(out, "+OK");
                                    for (var e : mb.list()) {
                                        int i = e.getKey();
                                        String uid = String.valueOf(i);
                                        send(out, i + " " + uid);
                                    }
                                    send(out, ".");
                                } else {
                                    int idx = parseIndex(parts[1]); // message number
                                    // find the entry with key == idx
                                    var list = mb.list();
                                    Map.Entry<Integer, Long> entry = null;
                                    for (var e : list) {
                                        if (e.getKey() == idx) {
                                            entry = e;
                                            break;
                                        }
                                    }
                                    if (entry == null) {
                                        send(out, "-ERR no such message");
                                    } else {
                                        String uid = String.valueOf(entry.getKey());
                                        send(out, "+OK " + entry.getKey() + " " + uid);
                                    }                                }
                            }
                            case "LIST" -> {
                                if (parts.length == 2) {
                                    int idx = parseIndex(parts[1]);
                                    long size = mb.sizeBytes(idx);
                                    send(out, "+OK " + idx + " " + size);
                                } else {
                                    List<Map.Entry<Integer, Long>> list = mb.list();
                                    send(out, "+OK " + list.size() + " messages (" + mb.totalSizeBytes() + " octets)");
                                    for (var e : list) send(out, e.getKey() + " " + e.getValue());
                                    send(out, ".");
                                }
                            }
                            case "RETR" -> {
                                if (parts.length != 2) {
                                    send(out, "-ERR syntax");
                                    break;
                                }
                                int idx = parseIndex(parts[1]);
                                String msg = mb.getMessage(idx);
                                // Size hint can remain as-is; strict octet count isn’t required by most clients.
                                send(out, "+OK " + msg.getBytes().length + " octets");
                                writeRetr(out, msg);   // <-- now dot-stuffed & CRLF-normalized
                                send(out, ".");
                            }
                            case "DELE" -> {
                                if (parts.length != 2) {
                                    send(out, "-ERR syntax");
                                    break;
                                }
                                int idx = parseIndex(parts[1]);
                                mb.markDelete(idx);
                                send(out, "+OK message " + idx + " deleted.");
                            }
                            case "NOOP" -> send(out, "+OK");
                            case "RSET" -> {
                                mb.unmarkAll();
                                mb.loadNew(); // reload mapping
                                send(out, "+OK");
                            }
                            case "TOP" -> {
                                if (parts.length != 3) {
                                    send(out, "-ERR syntax: TOP <msg> <n>");
                                    break;
                                }
                                int idx = parseIndex(parts[1]);
                                int n = parseIndex(parts[2]); // clamp to >= 0
                                String msg = mb.getMessage(idx);
                                send(out, "+OK top of message follows");
                                writeTop(out, msg, n);   // <-- see helper below (does dot-stuffing)
                                send(out, ".");          // end of multi-line
                            }
                            default -> send(out, "-ERR unknown or invalid in TRANSACTION");
                        }
                    }
                    default -> send(out, "-ERR invalid state");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int parseIndex(String s) {
        return Integer.parseInt(s);
    }

    private void send(BufferedWriter out, String s) throws IOException {
        out.write(s + "\r\n");
        out.flush();
        log.info("s: " + s);
    }

    private void writeRaw(BufferedWriter out, String s) throws IOException {
        // s already contains CRLF lines (SMTP DATA preserved)
        out.write(s);
        out.write("\r\n");
        out.flush();
    }

    private void dotWriteLine(BufferedWriter out, String line) throws IOException {
        if (line.startsWith(".")) line = "." + line; // POP3 dot-stuffing
        out.write(line);
        out.write("\r\n");
    }

    private void writeRetr(BufferedWriter out, String msg) throws IOException {
        try (BufferedReader r = new BufferedReader(new StringReader(msg))) {
            for (String line; (line = r.readLine()) != null; ) {
                dotWriteLine(out, line);
            }
        }
    }

    private void writeTop(BufferedWriter out, String msg, int nBodyLines) throws IOException {
        boolean inHeaders = true;
        int outBody = 0;
        try (BufferedReader r = new BufferedReader(new StringReader(msg))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (inHeaders) {
                    dotWriteLine(out, line);
                    if (line.isEmpty()) { // blank line separates headers/body
                        inHeaders = false;
                        if (nBodyLines == 0) break; // only headers requested
                    }
                } else {
                    if (outBody++ < nBodyLines) {
                        dotWriteLine(out, line);
                    } else {
                        break;
                    }
                }
            }
            // If message had no blank line, we already output "headers only" which is fine.
        }
    }
}
