package whiteboard;

import whiteboard.validation.ProtocolValidator;

import java.io.IOException;

public class ClientHandler extends Thread {

    private final ClientConnection client;
    private final WhiteboardServer server;

    public ClientHandler(ClientConnection client, WhiteboardServer server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            System.out.println("sending history to new client...");
            // syncs initial state by sending all past actions to the new client
            synchronized (server.getHistory()) {
                for (String action : server.getHistory()) {
                    client.sendMessage(action);
                }
            }

            // main loop: wait for messages from this client
            String message;
            while ((message = client.readMessage()) != null) {
                // ignore empty lines (keep-alive or glitches)
                if (message.trim().isEmpty()) continue;

                System.out.println(message);

                // check using ProtocolValidator
                if (!ProtocolValidator.isValid(message)) {
                    client.sendMessage("ERROR;INVALID_FORMAT");
                    continue;
                }

                // broadcast the received message to EVERYONE (including sender)
                server.broadcast(message);
            }
        } catch (IOException e) {
            // client likely disconnected abruptly
            System.out.println("Client likely disconnected");
        } finally {
            // cleanup when loop ends
            server.removeClient(client);
        }
    }
}