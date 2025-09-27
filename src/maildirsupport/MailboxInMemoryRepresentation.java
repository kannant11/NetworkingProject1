package maildirsupport;

 import java.util.Arrays;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class MailboxInMemoryRepresentation {
    
    // Holds all marked emails for deletion
    LinkedList<String> marked;

    public void MailToDirectory(MessageInMemoryRepresentation msge){

        //Makes a list of those reciving the email and then creates a file for each recipient.
        LinkedList<String> list = msge.getRecipients();

        for(int i = 0; i < list.size(); i++){

            //Name of file
            String name = Instant.now().toString();

            //Destinations for its initial creation and end point
            String path = "mail/" + list.get(i) + "/tmp/" + name + ".txt";
            String dest = "mail/" + list.get(i) + "/new/" + name + ".txt";

            File file = new File(path);

            //  try(FileWriter fw = new FileWriter(file.getPath(), true));
            //  BufferedWriter bw = new BufferedWriter(fw) {

            //Adds sender email to the first line and message on the second line
            try (FileWriter fw = new FileWriter(file.getPath(), true);
            BufferedWriter bw = new BufferedWriter(fw)) {

                //Potential error could be from \n since multiple reasources offered different ways to move lines, will look at later if needed
                bw.write(msge.getSender() + "\n" + msge.getMessage());

            } catch (IOException e) {

                System.out.println("Can't find file");

            }

            // Moves files from tmp to new
             try {

                Files.move(Paths.get(path), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);

             } catch (IOException e) {

                System.out.println("Error moving file");

             }

        }

    }

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

        marked.add(path);

    }

    //Deletes all marked emails. Note: Idk if the paths need the file type with the name in order to find the correct file
    //will test this tommorrow as it is late
    public void DeleteEmail(){

        for (int i = 0; i < marked.size(); i++){

            File file = new File(marked.get(i));

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

        marked.clear();

    }

}
