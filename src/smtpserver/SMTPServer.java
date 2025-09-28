package smtpserver;

import common.AutoFlushFileHandler;
import common.JsonConfig;
import merrimackutil.json.JsonIO;

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
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SMTPServer {
    
    LinkedList<Path> Mailboxes = new LinkedList<>(
        java.util.Arrays.asList(Paths.get("maildirsupport/mail/adamroche"), Paths.get("maildirsupport/mail/joeywilliams"), Paths.get("maildirsupport/mail/nickjohnson"))
    );

    public static void main(String[] args) throws Exception {

        Path cfgPath = Paths.get("smtpd.json");
        if (args.length == 2 && ("-c".equals(args[0]) || "--config".equals(args[0]))) {
            cfgPath = Paths.get(args[1]);
        }
        var obj = JsonIO.readObject(cfgPath.toFile());
        JsonConfig cfg = JsonConfig.fromJson(obj);

        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %3$s: %5$s%6$s%n");
        Logger log = Logger.getLogger("smtpd");
        log.setUseParentHandlers(false);
        FileHandler fh = new AutoFlushFileHandler(cfg.log, true);
        fh.setFormatter(new SimpleFormatter());
        log.addHandler(fh);

        MailQueue queue = new MailQueue();
        MailQueueThread worker = new MailQueueThread(queue, Paths.get(cfg.spool), cfg.server_name);
        worker.start();

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (ServerSocket ss = new ServerSocket(cfg.port)) {
            while (true) {
                Socket sock = ss.accept();
//                pool.submit(new SmtpConnectionHandler(sock, cfg.server_name, queue, log));
            }
        }

    }

}
