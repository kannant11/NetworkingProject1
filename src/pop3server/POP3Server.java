package pop3server;

import common.*;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class Pop3Server {
    public static void main(String[] args) throws Exception {
        Path cfgPath = Paths.get("pop3d.json");
        if (args.length == 2 && ("-c".equals(args[0]) || "--config".equals(args[0]))) {
            cfgPath = Paths.get(args[1]);
        }
        JSONObject obj = JsonIO.readObject(cfgPath.toFile());
        Pop3Config cfg = Pop3Config.fromJson(obj);

        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %3$s: %5$s%6$s%n");
        Logger log = Logger.getLogger("pop3d");
        log.setUseParentHandlers(false);
        FileHandler fh = new AutoFlushFileHandler(cfg.getLog(), true);
        fh.setFormatter(new SimpleFormatter());
        log.addHandler(fh);

        AccountsDb accounts = AccountsDb.load(Paths.get(cfg.accounts));
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (ServerSocket ss = new ServerSocket(cfg.getPort())) {
            while (true) {
                Socket s = ss.accept();
                pool.submit(new Pop3ConnectionHandler(s, cfg, accounts, log));
            }
        }
    }
}
