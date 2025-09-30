package smtpserver;

import maildirsupport.MailMessage;
import maildirsupport.MailboxInMemoryRepresentation;

import java.nio.file.Path;
import java.util.LinkedList;
import common.SmtpAddrUtil;

public class MailQueueThread extends Thread {

    MailQueue mailQueueCopy; //copy of mail queue
    private Path spoolRoot;
    private String serverName;

    //mail queue thread constructor to take copy of mail queue
    public MailQueueThread(MailQueue queue, Path spoolRoot, String serverName) {
        this.mailQueueCopy = queue;
        this.spoolRoot = spoolRoot;
        this.serverName = serverName;
    }

    public void run() {
        while (true) {
            try {
                MailMessage msg = mailQueueCopy.take();
                String normalized = ensureRfc5322Headers(
                        msg.getMessage(),
                        msg.getSender(),
                        msg.getRecipients(),
                        this.serverName);
                for (String rcpt : msg.getRecipients()) {
                    String[] parts = SmtpAddrUtil.extractAddrSpec(rcpt).split("@", 2);
                    if (parts.length != 2 || !this.serverName.equalsIgnoreCase(parts[1])) continue;
                    MailboxInMemoryRepresentation mb = MailboxInMemoryRepresentation.getInstance(spoolRoot);
                    // Temporarily wrap the normalized text into a MailMessage for existing API
                    String msgToStore = normalized.endsWith("\r\n") ? normalized.substring(0, normalized.length() - 2) : normalized;
                    // Preserve \r\n protocol lines exactly:
                    msgToStore += msgToStore + "\r\n";
                    LinkedList<String> recipients = (LinkedList<String>) msg.getRecipients().stream().toList();
                    MailMessage toStore = new MailMessage(msgToStore, msg.getSender(), recipients);
                    toStore.setSender(msg.getSender());
                    mb.add(toStore, parts[0]);
                }
            } catch (InterruptedException ignored) {
                return;
            } catch (Exception e) {
                // log error (swallow to keep worker alive)
                e.printStackTrace();
            }
        }
    }

    private String ensureRfc5322Headers(String raw, String envelopeFrom, java.util.List<String> rcpts, String serverName) {
        // Normalize
        String msg = raw == null ? "" : raw;
        // Detect header/body split: first empty line ends header section
        int hdrEnd = msg.indexOf("\r\n\r\n");
        boolean hasHeaderBlock = hdrEnd >= 0;

        // Helper: case-insensitive header search within header block
        java.util.function.Predicate<String> hasFromHeader = (h) -> {
            int end = hasHeaderBlock ? hdrEnd : Math.max(0, msg.length());
            // scan line-by-line until hdrEnd
            int start = 0;
            while (start < end) {
                int nl = msg.indexOf("\r\n", start);
                if (nl < 0 || nl > end) nl = end;
                String line = msg.substring(start, nl);
                if (line.isEmpty()) break; // end of headers
                if (line.regionMatches(true, 0, "From:", 0, 5)) return true;
                start = nl + 2;
            }
            return false;
        };

        boolean hasFrom = hasHeaderBlock && hasFromHeader.test(msg);

        // Build minimal headers we may need
        String now = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
        String fromLine = "From: " + (envelopeFrom.startsWith("<") ? envelopeFrom : "<" + envelopeFrom + ">") + "\r\n";
        String toLine = "To: " + String.join(", ", rcpts) + "\r\n";
        String dateLine = "Date: " + now + "\r\n";
        String msgId = "Message-ID: <" + java.util.UUID.randomUUID() + "@" + serverName + ">\r\n";
        String retPath = "Return-Path: " + (envelopeFrom.startsWith("<") ? envelopeFrom : "<" + envelopeFrom + ">") + "\r\n";

        if (!hasHeaderBlock) {
            // No headers at all → synthesize a header block, then a blank line, then the original body
            StringBuilder b = new StringBuilder();
            b.append(retPath).append(fromLine).append(toLine).append(dateLine).append(msgId).append("\r\n");
            b.append(msg);
            return b.toString();
        }
        if (!hasFrom) {
            // Has headers but missing From: → inject just a From: (and optional Return-Path) before hdrEnd
            String inject = retPath + fromLine;
            return msg.substring(0, hdrEnd) + inject + msg.substring(hdrEnd);
        }
        return msg;
    }
}