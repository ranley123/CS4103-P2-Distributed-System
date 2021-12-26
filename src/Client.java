import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    // initialize socket and input output streams
    private Socket socket            = null;
    private DataInputStream input   = null;
    private DataOutputStream out     = null;

    // constructor to put ip address and port
    public Client(String address, int port)
    {
        // establish a connection
        try
        {
            socket = new Socket(address, port);
            System.out.println("Connected");

            // takes input from terminal
            input  = new DataInputStream(System.in);

            // sends output to the socket
            out    = new DataOutputStream(socket.getOutputStream());
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // string to read message from input
        String line = "";

        // keep reading until "Over" is input
        while (!line.equals("Over"))
        {
            try
            {
                line = input.readLine();
                out.writeUTF(line);
            }
            catch(IOException i)
            {
                System.out.println(i);
            }
        }

        // close the connection
        try
        {
            input.close();
            out.close();
            socket.close();
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public static void main(String args[])
    {

//        Client client = new Client("127.0.0.1", 8001);

        Scanner scanner = new Scanner(System.in);
        Socket socket = null;
        DataInputStream input = null;
        DataOutputStream out = null;
        System.out.println("Enter server ID you want to connect to: ");
        int serverId = scanner.nextInt();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./src/servers.csv"));
            String line = "";

            while((line = reader.readLine()) != null){
                String[] pieces = line.split(",");
                int id = Integer.parseInt(pieces[0]);

                if(id != serverId){
                    continue;
                }

                String host = pieces[1];
                int port = Integer.parseInt(pieces[2]);
                socket = new Socket(host, port);
                break;
            }

            if(socket == null){
                System.err.println("No server found!");
                System.exit(0);
            }

            // takes input from terminal
            input  = new DataInputStream(System.in);

            // sends output to the socket
            out    = new DataOutputStream(socket.getOutputStream());

            System.out.println("What Operation do you want? 0 for Receive, 1 for Send a post");
            int choice = scanner.nextInt();
            if(choice == 0){ // receive
                out.writeUTF("PULL");
                String response = input.readLine();

            }
            else if(choice == 1){ // send
                System.out.println("Enter the message for sending: ");
                String msg = scanner.nextLine();

                System.out.println("Enter the server Id for sending: ");
                int receiverId = scanner.nextInt();

                String post = "POST " + receiverId + "," + msg;
                out.writeUTF(post);
                String response = input.readLine();
                System.out.println(response);
            }
            else{
                System.err.println("No operation available.");
                System.exit(0);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
