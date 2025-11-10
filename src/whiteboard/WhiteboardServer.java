package whiteboard;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhiteboardServer {

    private ServerSocket serverSocket;
    // thread-safe list to keep track of all active client connections
    private final List<ClientConnection> clients = Collections.synchronizedList(new ArrayList<>());
    // history of all drawing actions to sync new clients
    private final List<String> actionHistory = Collections.synchronizedList(new ArrayList<>());

    public WhiteboardServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("server running on port: " + port);
    }

    public void start() throws IOException {
        while (true) {
            // waits for a new client to connect
            Socket socket = this.serverSocket.accept();

            // wraps the socket in our connection class
            ClientConnection client = new ClientConnection(socket);
            addClient(client);

            // starts a dedicated thread to handle this client
            new ClientHandler(client, this).start();
            System.out.println("new client connected: " + socket.getInetAddress().getHostAddress());
        }
    }

    // sends a message to ALL connected clients
    public void broadcast(String message) {
        // save to history so future clients receive it too
        actionHistory.add(message);

        synchronized (clients) {
            for (ClientConnection client : clients) {
                // only try to send if the client is still actively connected
                if (client.isRunning()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void addClient(ClientConnection client) {
        clients.add(client);
    }

    public void removeClient(ClientConnection client) {
        clients.remove(client);
        client.close();
        System.out.println("client disconnected");
    }

    public List<String> getHistory() {
        return actionHistory;
    }

    // entry point of the backend application
    public static void main(String[] args) {
        try {
            // tries to get port from environment variable (useful for cloud/docker)
            String portEnv = System.getenv("PORT");
            // defaults to 12345 if no env var is set
            int port = (portEnv != null) ? Integer.parseInt(portEnv) : 12345;

            new WhiteboardServer(port).start();
        } catch (IOException e) {
            System.err.println("server error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("error: invalid PORT environment variable.");
        }
    }
}