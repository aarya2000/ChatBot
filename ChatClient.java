import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    private ChatClient(String username, int port, String server) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    public ObjectInputStream getsInput() {
        return sInput;
    }

    public ObjectOutputStream getsOutput() {
        return sOutput;
    }

    public Socket getSocket() {
        return socket;
    }

    /*
     * This starts the Chat Client
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * This method is used to send a ChatMessage Objects to the server
     */
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults

        // Create your client and start it
        ChatClient client;
        if (args.length == 0) {
            client = new ChatClient("Anonymous", 1500, "localhost");
        } else if (args.length == 1) {
            client = new ChatClient(args[0], 1500, "localhost");
        } else if (args.length == 2) {
            client = new ChatClient(args[0], Integer.parseInt(args[1]), "localhost");
        } else {
            client = new ChatClient(args[0], Integer.parseInt(args[1]), args[2]);
        }
        client.start();

        // Send an empty message to the server
        Scanner input = new Scanner(System.in);
        while (true) {
            String message = input.nextLine();
            if (message.equals("/logout")) {
                client.sendMessage(new ChatMessage(1, "/logout", null));
                break;
            } else if (message.contains("/msg")) {
                String[] words = message.split(" ");
                message = "";
                for (int i = 2; i < words.length; i++) {
                    message += words[i] + " ";
                }
                client.sendMessage(new ChatMessage(2, message + "\n", words[1]));
            } else if (message.contains("/list")) {
                client.sendMessage(new ChatMessage(3, "/list", null));
            } else {
                client.sendMessage(new ChatMessage(0, message + "\n", null));
            }
        }
        try {
            client.sOutput.close();
            client.sInput.close();
            client.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.print(msg);
                } catch (IOException | ClassNotFoundException e) {
                    return;
                }
            }
        }
    }
}
