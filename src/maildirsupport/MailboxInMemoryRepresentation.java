package maildirsupport;

import common.MailBoxException;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class MailboxInMemoryRepresentation {
    private Path userRoot;     // .../<spool>/<username>
    private Path dirTmp;
    private Path dirNew;

    private final NavigableMap<Integer, Path> indexToPath = new TreeMap<>();

    // Holds all marked emails for deletion
    private final Set<Integer> markedForDeletion = new HashSet<>();

    public MailboxInMemoryRepresentation() {
        this.userRoot = Path.of("mail");
        this.dirNew = userRoot.resolve("new");
        this.dirTmp = userRoot.resolve("tmp");
    }

    public MailboxInMemoryRepresentation(Path spoolRoot, String username) {
        this.userRoot = spoolRoot.resolve(username);
        this.dirTmp = userRoot.resolve("tmp");
        this.dirNew = userRoot.resolve("new");
    }

    public void add(MailMessage msg) throws MailBoxException {
        try {
            String fname = Instant.now().toString(); // unique enough for this project
            Path tmpFile = dirTmp.resolve(fname);
            Files.writeString(tmpFile, msg.getMessage()); // ASCII/UTF-8 OK for “plain text” here

            Path newFile = dirNew.resolve(fname);
            Files.move(tmpFile, newFile, ATOMIC_MOVE);
        } catch (IOException e) {
            throw new MailBoxException("Failed to add mail", e);
        }
    }

    /**
     * Load current NEW directory into an index map (lazy: don’t read file contents).
     */
    public void loadNew() throws MailBoxException {
        indexToPath.clear();
        int idx = 1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dirNew)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    indexToPath.put(idx, p.getFileName());
                    idx++;
                }
            }
        } catch (IOException e) {
            throw new MailBoxException("Failed to load new/", e);
        }
        markedForDeletion.clear();
    }

    public int messageCount() {
        return indexToPath.size();
    }

    public long totalSizeBytes() throws MailBoxException {
        long sum = 0;
        for (Path p : indexToPath.values()) {
            sum += fileSizeBytes(p);
        }
        return sum;
    }

    public long sizeBytes(int index) throws MailBoxException {
        Path p = resolve(index);
        return fileSizeBytes(p);
    }

    public List<Map.Entry<Integer, Long>> list() throws MailBoxException {
        List<Map.Entry<Integer, Long>> out = new ArrayList<>();
        for (Map.Entry<Integer, Path> e : indexToPath.entrySet()) {
            out.add(Map.entry(e.getKey(), fileSizeBytes(e.getValue())));
        }
        return out;
    }

    //Returns the message of a certain email. Note: In order to make sure the code worked onm multiple computers I made the
    //paths refer to local directories instead of going on my computer and copying the path. If error occurs might be pathing. Srry
    public String getMessage(int index) throws MailBoxException {

        Path p = resolve(index);
        try {
            return Files.readString(userRoot.resolve("new").resolve(p));
        } catch (IOException e) {
            throw new MailBoxException("Failed to read message " + index, e);
        }

    }

    public void markDelete(int index) throws MailBoxException {
        requireIndex(index);
        markedForDeletion.add(index);
    }

    /** Apply deletions (POP3 UPDATE phase). */
    public void deleteMarked() throws MailBoxException {
        for (int idx : markedForDeletion) {
            Path p = resolve(idx);
            try {
                Files.deleteIfExists(userRoot.resolve("new").resolve(p));
            } catch (IOException e) {
                throw new MailBoxException("Failed deleting msg " + idx, e);
            }
        }
        markedForDeletion.clear();
    }

    //Unselects all marked emails
    public void unmarkAll() {

        markedForDeletion.clear();

    }

    private Path resolve(int index) throws MailBoxException {
        requireIndex(index);
        return indexToPath.get(index);
    }

    private void requireIndex(int index) throws MailBoxException {
        if (!indexToPath.containsKey(index))
            throw new MailBoxException("No such message: " + index);
    }

    private long fileSizeBytes(Path filenameInNew) throws MailBoxException {
        try {
            return Files.size(userRoot.resolve("new").resolve(filenameInNew));
        } catch (IOException e) {
            throw new MailBoxException("Failed to get size for " + filenameInNew, e);
        }
    }


}
