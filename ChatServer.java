

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private String filterFile;
    private final static Object object = new Object();

    private ChatServer(int port, String filterFile) {
        this.port = port;
        this.filterFile = filterFile;
    }

    public String getFilterFile() {
        return this.filterFile;
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                clients.add((ClientThread) r);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        ChatServer server;
        if (args.length == 0) {
            server = new ChatServer(1500, "badwords.txt");
        } else if (args.length == 1) {
            server = new ChatServer(Integer.parseInt(args[0]), "badwords.txt");
        } else {
            server = new ChatServer(Integer.parseInt(args[0]), args[1]);
        }
        server.start();
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private boolean writeMessage(String msg) {
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return socket.isConnected();
        }

        private void remove(int d) {
            synchronized (object) {
                clients.remove(d);
            }
        }

        private void broadcast(String message) {
            ChatFilter filter = new ChatFilter(getFilterFile());
            synchronized (object) {
                for (int i = 0; i < clients.size(); i++) {
                    clients.get(i).writeMessage(filter.filter(message));
                }
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client
            while (true) {
                String message = "";
                try {
                    cm = (ChatMessage) sInput.readObject();
                if (cm.getType() == 1) {
                    message = username + " disconnected with a LOGOUT message.\n";
                    broadcast(message);
                    remove(id);
                    close();
                } else if (cm.getType() == 2) {
                    message = date() + username + " -> " + cm.getRecipient() + ": " + cm.getMessage();
                    directMessage(message, cm.getRecipient());
                } else if (cm.getType() == 3) {
                    message = list();
                    broadcast(message);
                } else {
                    message = date() + username + ": " + cm.getMessage();
                    broadcast(message);
                }
                } catch (IOException | ClassNotFoundException e) {
                    message = username + " disconnected with a LOGOUT message.\n";
                    remove(id);
                    close();
                }
                System.out.print(message);
            }

            // Send message back to the client
        }

        public String date() {
            Date date = new Date();
            SimpleDateFormat dT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss ");
            return dT.format(date);
        }

        private synchronized void close() {
            try {
                socket.close();
                sOutput.close();
                sInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void directMessage(String message, String username) {
            ChatFilter filter = new ChatFilter(getFilterFile());
            int n = 0;
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).username.equalsIgnoreCase(username)) {
                    n = i;
                    break;
                }
            }
            clients.get(n).writeMessage(filter.filter(message));
        }

        private String list() {
            String[] userList = new String[clients.size() - 1];
            String nameList = "List of users: \n";
            int n = 0;
            for (int i = 0; i < clients.size(); i++) {
                if (!(clients.get(i).username).equals(this.username)) {
                    System.out.println("here");
                    userList[n] = clients.get(i).username;
                    n++;
                }
            }
            for (int i = 0; i < userList.length; i++) {
                nameList += userList[i] + "\n";
            }
            return nameList;
        }
    }
}
