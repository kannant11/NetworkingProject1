package maildirsupport;

import common.MailBoxException;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NavigableMap;
import java.util.TreeMap;

public class MailboxInMemoryRepresentation {
    private Path userRoot;     // .../<spool>/<username>
    private Path dirTmp;
    private Path dirNew;

    private final NavigableMap<Integer, Path> indexToPath = new TreeMap<>();

    // Holds all marked emails for deletion
    LinkedList<String> markedForDeletion;

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

    /** Load current NEW directory into an index map (lazy: don’t read file contents). */
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

    public int messageCount() { return indexToPath.size(); }

    //Gets a list of all files in a directory and sorts the by alphabetical order for a consistent order
    public String[] LoadMessages(String user){

        File list = new File("mail/" + user + "/new");
        String[] msge = list.list();
        if (msge != null) {
            Arrays.sort(msge);
            return msge;
        } else {
            return new String[0];
        }

    }

    //Returns the message of a certain email. Note: In order to make sure the code worked onm multiple computers I made the
    //paths refer to local directories instead of going on my computer and copying the path. If error occurs might be pathing. Srry
    public String GetMessage(String user, int i){

        String name = this.LoadMessages(user)[i];

        try (BufferedReader br = new BufferedReader(new FileReader("mail/" + user + "/new/" + name))){

            String msge;

            br.readLine();

            return br.readLine();

            //while ((msge = br.readLine()) != null){}

        }catch (IOException e){

            System.out.println("Could not find or read file");
            return null;

        }

    }

    //Returns size of message in bytes
    public long GetMessageSize(String user, int i){

        String name = this.LoadMessages(user)[i];

        File file = new File("mail/" + user + "/new/" + name);

        return file.length();

    }

    //Returns the size of all messages for one user
    public long GetAllMessageSize(String user){

        long temp = 0;

        for (int i = 0; i < this.LoadMessages(user).length; i++){

            temp = temp + this.GetMessageSize(user, i);

        }

        System.out.println(this.LoadMessages(user).length + " Email(s).\n" + temp + " Bytes");

        return temp;

    }

    //Marks an email for deletion
    public void MarkEmail(String user, int i){

        String name = this.LoadMessages(user)[i];

        String path = ("mail/" + user + "/new/" + name);

        markedForDeletion.add(path);

    }

    //Deletes all marked emails. Note: Idk if the paths need the file type with the name in order to find the correct file
    //will test this tommorrow as it is late
    public void DeleteEmail(){

        for (int i = 0; i < markedForDeletion.size(); i++){

            File file = new File(markedForDeletion.get(i));

            if(file.delete()){

                System.out.println("Files deleted");

            }
           
            else{

                System.out.println("No files");

            }

        }

    }

    //Unselects all marked emails
    public void Unselected(){

        markedForDeletion.clear();

    }

}
