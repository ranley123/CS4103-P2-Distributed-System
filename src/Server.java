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

    int coordinatorPort = 8002;
    int coordinatorId = 2;
    boolean isElecting = false;

    int successorPort = -1;
    int successorId = -1;



    public Server(int id, String host, int port){
        this.id = id;
        this.host = host;
        this.port = port;

        if(id == coordinatorId){
            isCoordinator = true;

            initNodes();
        }
        LOGFILE = "./src/" + port + ".log";
    }

    public void checkValidCoordinator(){
        if(successorId != -1 && id > coordinatorId && !isElecting){
            String msg = "ELECTION {" + id + "}";
            writeToSuccessor(msg);
            isElecting = true;
        }
    }

    public void start(){
        try{
                serverSocket = new ServerSocket(port);
                System.out.println("Port " + port + " server started");

                // check if all nodes are online
                if (isCoordinator) {
                    boolean res = checkOnlineNodes();
                    if (!res)
                        System.exit(0);
                }


            while(true) {
                checkValidCoordinator();
//                System.out.println("Waiting for a client ...");
                socket = serverSocket.accept();
//                System.out.println("Client accepted");

                // takes input from the client socket
                in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));

                String line = "";

                // reads message from client until "Over" is sent
                if (!line.equals("OVER")) {
                    try {
                        line = in.readUTF();
                        System.out.println(line);
                        String[] pieces = line.split(" ");
                        String command = pieces[0];

                        if (command.equals("INIT")) {
                            String[] tmp = pieces[1].split(",");
                            successorPort = Integer.parseInt(tmp[0]);
                            successorId = Integer.parseInt(tmp[1]);
                        } else if (command.equals("ELECTION")) {
                            // check is back
                            String[] tmp = pieces[1].substring(1, pieces[1].length() - 1).split(",");
                            int i = Integer.parseInt(tmp[0]);
                            if (i == id) {
                                // find the largest one
                                int max = -1;

                                for (String s : tmp) {
                                    max = Math.max(max, Integer.parseInt(s));
                                }

                                String msg = "COORDINATOR {" + max + "}";
                                writeToSuccessor(msg);

                            } else {
                                String msg = pieces[1].substring(0, pieces[1].length() - 1) + "," + id + "}";

                                writeToSuccessor("ELECTION " + msg);
                                System.out.println("Send to " + successorId + ": ELECTION " + msg);
                            }
                        } else if (command.equals("COORDINATOR")) {
                            coordinatorId = Integer.parseInt(pieces[1].substring(1, pieces[1].length() - 1));
                            coordinatorPort = 8000 + coordinatorId;
                            if (coordinatorId != id)
                                writeToSuccessor(line);
                            else{
                                isElecting = false;
                                isCoordinator = true;
                                initNodes();
                                boolean res = checkOnlineNodes();
                                if (!res)
                                    System.exit(0);
                            }
                        }

                    } catch (IOException i) {
                        System.out.println(i);
                    }
                }
                else{
                    break;
                }
//                System.out.println("Closing connection");
                // close connection
                if (socket != null)
                    socket.close();
                in.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToSuccessor(String msg){
        try {
            Socket successor = new Socket(host, successorPort);
            DataOutputStream out = new DataOutputStream(successor.getOutputStream());
            out.writeUTF(msg);

            out.close();
            successor.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initNodes(){
        nodes = new ArrayList<>();
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

    public boolean checkOnlineNodes(){
        NodeInfo n = null;
        try{
            for(int i = 0; i < nodes.size(); i++){
                n = nodes.get(i);
                socket = new Socket(n.host, n.port);
                out = new DataOutputStream(socket.getOutputStream());

                if(i < nodes.size() - 1){
                    out.writeUTF("INIT " + nodes.get(i + 1).port + "," + nodes.get(i + 1).id);
                }
                else{
                    out.writeUTF("INIT " + nodes.get(0).port + "," + nodes.get(0).id);
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
