package maildirsupport;

import common.MailBoxException;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class MailboxInMemoryRepresentation {
    private Path spoolRoot;     // .../<spool>/<username>

    // Holds all marked emails for deletion per user
    private final Map<String, Set<Integer>> markedForDeletion = new HashMap<>();

    // Per-user in-memory index: POP3-style 1-based index -> file path in user's "new"
    private final Map<String, NavigableMap<Integer, Path>> userIndexes = new HashMap<>();

    private static MailboxInMemoryRepresentation singleton;

    private MailboxInMemoryRepresentation(Path spoolRoot) {
        this.spoolRoot = spoolRoot;
    }

    public static synchronized MailboxInMemoryRepresentation getInstance(Path spoolRoot) {
        if (singleton == null) {
            singleton = new MailboxInMemoryRepresentation(spoolRoot);
        }
        return singleton;
    }

    public void add(MailMessage msg, String userName) throws MailBoxException {
        var userRoot = spoolRoot.resolve(userName);
        var dirNew = userRoot.resolve("new");
        var dirTmp = userRoot.resolve("tmp");
        try {
            String fname = Instant.now().toString(); // unique enough for this project
            Path tmpFile = dirTmp.resolve(fname);
            Files.writeString(tmpFile, msg.getMessage()); // ASCII/UTF-8 OK for “plain text” here

            Path newFile = dirNew.resolve(fname);
            Files.move(tmpFile, newFile, ATOMIC_MOVE);

            // Invalidate/refresh index for this user (lazy rebuild next time)
            userIndexes.remove(userName);
        } catch (IOException e) {
            throw new MailBoxException("Failed to add mail", e);
        }
    }

    public int messageCount(String userName) throws MailBoxException {
        NavigableMap<Integer, Path> index = buildOrGetIndex(userName);
        Set<Integer> deleted = markedForDeletion.getOrDefault(userName, Set.of());
        int count = 0;
        int idx = 1;
        for (int i = 1; i <= index.size(); i++) {
            if (!deleted.contains(idx)) {
                count++;
            }
            idx++;
        }
        return count;
    }

    public long totalSizeBytes(String userName) throws MailBoxException {
        NavigableMap<Integer, Path> index = buildOrGetIndex(userName);
        long sum = 0;
        for (Path p : index.values()) {
            sum += fileSizeBytes(userName, p);
        }
        return sum;
    }

    public long sizeBytes(String userName, int index) throws MailBoxException {
        Path p = resolve(userName, index);
        return fileSizeBytes(userName, p);
    }

    public List<Map.Entry<Integer, Long>> list(String userName) throws MailBoxException {
        NavigableMap<Integer, Path> index = buildOrGetIndex(userName);
        Set<Integer> deleted = markedForDeletion.getOrDefault(userName, Set.of());
        List<Map.Entry<Integer, Long>> out = new ArrayList<>();
        for (Map.Entry<Integer, Path> e : index.entrySet()) {
            int i = e.getKey();
            if (!deleted.contains(i)) {
                out.add(Map.entry(i, fileSizeBytes(userName, e.getValue())));
            }
        }
        return out;
    }

    //Returns the message of a certain email.
    public String getMessage(String userName, int index) throws MailBoxException {
        Path p = resolve(userName, index);
        try {
            return Files.readString(spoolRoot.resolve(userName).resolve("new").resolve(p));
        } catch (IOException e) {
            throw new MailBoxException("Failed to read message " + index, e);
        }
    }

    public void markDelete(String userName, int index) throws MailBoxException {
        requireIndex(userName, index);
        markedForDeletion.computeIfAbsent(userName, k -> new HashSet<>()).add(index);
    }

    /** Apply deletions (POP3 UPDATE phase). */
    public void deleteMarked(String userName) throws MailBoxException {
        NavigableMap<Integer, Path> index = buildOrGetIndex(userName);
        Set<Integer> toDelete = markedForDeletion.getOrDefault(userName, Set.of());
        for (int idx : toDelete) {
            Path p = resolve(userName, idx);
            try {
                Files.deleteIfExists(spoolRoot.resolve(userName).resolve("new").resolve(p));
            } catch (IOException e) {
                throw new MailBoxException("Failed deleting msg " + idx, e);
            }
        }
        // Rebuild user index and clear marks for this user
        userIndexes.remove(userName);
        markedForDeletion.remove(userName);
    }

    // Unselects all marked emails for user
    public void unmarkAll(String userName) {
        markedForDeletion.remove(userName);
    }

    private Path resolve(String userName, int index) throws MailBoxException {
        requireIndex(userName, index);
        return buildOrGetIndex(userName).get(index);
    }

    private void requireIndex(String userName, int index) throws MailBoxException {
        NavigableMap<Integer, Path> indexMap = buildOrGetIndex(userName);
        if (!indexMap.containsKey(index)) {
            throw new MailBoxException("No such message: " + index);
        }
    }

    private long fileSizeBytes(String userName, Path filenameInNew) throws MailBoxException {
        try {
            return Files.size(spoolRoot.resolve(userName).resolve("new").resolve(filenameInNew));
        } catch (IOException e) {
            throw new MailBoxException("Failed to get size for " + filenameInNew, e);
        }
    }

    // Build or get a cached 1-based index -> Path (relative to user's "new")
    private synchronized NavigableMap<Integer, Path> buildOrGetIndex(String userName) throws MailBoxException {
        NavigableMap<Integer, Path> cached = userIndexes.get(userName);
        if (cached != null) return cached;

        var userRoot = spoolRoot.resolve(userName);
        var dirNew = userRoot.resolve("new");
        NavigableMap<Integer, Path> index = new TreeMap<>();
        try {
            Files.createDirectories(dirNew);
            int idx = 1;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dirNew)) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) {
                        index.put(idx++, p.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            throw new MailBoxException("Failed to load new/", e);
        }
        userIndexes.put(userName, index);
        // Ensure marked set exists
        markedForDeletion.computeIfAbsent(userName, k -> new HashSet<>());
        return index;
    }
}
