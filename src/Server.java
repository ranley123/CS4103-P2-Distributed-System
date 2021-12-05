import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server {
    ArrayList<NodeInfo> nodes;
    String FILENAME = "./src/servers.csv";
    String LOGFILE;
    int id;
    String host;
    int port;
    private Socket socket   = null;
    private ServerSocket serverSocket   = null;
    private DataInputStream in       =  null;
    private DataOutputStream out     = null;
    boolean isCoordinator = false;

    Socket coordinatorSocket = null;
    int coordinatorPort = 8006;
    int coordinatorId = 5;

    int successorPort;
    int successorId;



    public Server(int id, String host, int port){
        this.id = id;
        this.host = host;
        this.port = port;

        if(id == 5){
            isCoordinator = true;
            nodes = new ArrayList<>();
            initNodes();
        }
        LOGFILE = "./src/" + port + ".log";
    }

    public void checkValidCoordinator(){
        if(id > coordinatorId){
            String msg = "ELECTION,{" + id + "}";
            writeToSuccessor(msg);
        }
    }

    public void start(){
        try{
            serverSocket = new ServerSocket(port);
            System.out.println("Port " + port + " server started");

            // check if all nodes are online
            if(isCoordinator){
                boolean res = checkNodeList();
                if(!res)
                    System.exit(0);
            }

            System.out.println("Waiting for a client ...");
            socket = serverSocket.accept();
            System.out.println("Client accepted");

            // takes input from the client socket
            in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            String line = "";

            // reads message from client until "Over" is sent
            if (!line.equals("over"))
            {
                try
                {
                    line = in.readUTF();
                    String[] pieces = line.split(",");
                    String command = pieces[0];

                    if(command.equals("init")){
                        successorPort = Integer.parseInt(pieces[2]);
                        successorId = Integer.parseInt(pieces[3]);
                    }
                    else if (command.equals("ELECTION")){
                        // check is back
                        String[] tmp = pieces[1].substring(1, pieces[1].length() - 1).split(",");
                        int i = Integer.parseInt(tmp[0]);
                        if(i == id){
                            // find the largest one
                            int max = -1;

                            for(String s: tmp){
                                max = Math.max(max, Integer.parseInt(s));
                            }
                            writeToSuccessor("COORDINATOR,{" + max + "}");
                        }
                        else{
                            String msg = pieces[1].substring(0, pieces[1].length() - 1) + "," + id + "}";

                            writeToSuccessor(msg);
                        }
                    }
                    else if(command.equals("COORDINATOR")){
//                        coordinatorId = pieces[1];
                    }

                }
                catch(IOException i)
                {
                    System.out.println(i);
                }
            }
            System.out.println("Closing connection");

            // close connection
            if(socket!= null)
                socket.close();
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToSuccessor(String msg){
        try {
            Socket successor = new Socket(host, successorPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(msg);

            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initNodes(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
            String line = "";

            while((line = reader.readLine()) != null){
                String[] pieces = line.split(",");
                int id = Integer.parseInt(pieces[0]);
                String host = pieces[1];
                int port = Integer.parseInt(pieces[2]);

                NodeInfo node = new NodeInfo(id, host, port);
                nodes.add(node);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeLog(){

    }

    public boolean checkNodeList(){
        NodeInfo n = null;
        try{
            for(int i = 0; i < nodes.size(); i++){
                n = nodes.get(i);
                if(n.id == id)
                    continue;
                socket = new Socket(n.host, n.port);
                out = new DataOutputStream(socket.getOutputStream());
                if(i < nodes.size() - 1){
                    out.writeUTF("init,successor," + nodes.get(i + 1).port + "," + nodes.get(i + 1).id);
                }
                else{
                    out.writeUTF("init,successor," + nodes.get(0).port + "," + nodes.get(0).id);
                }
                socket.close();
                out.close();

            }
        }catch (ConnectException e){
            System.err.println("port " + n.port + " is not online");
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    public static void main(String[] args){
        if(args.length != 3){
            System.out.println("Invalid arguments");
            System.exit(0);
        }

        int id = Integer.parseInt(args[0]);
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        Server server = new Server(id, host, port);
        server.start();

    }

}
