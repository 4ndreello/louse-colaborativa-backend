package whiteboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection {

    private final Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean running = false;

    public ClientConnection(Socket socket) {
        this.socket = socket;
        try {
            // initializes streams for reading from and writing to the client
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 'true' enables auto-flush, so messages are sent immediately
            this.output = new PrintWriter(socket.getOutputStream(), true);
            this.running = true;
        } catch (IOException ex) {
            System.err.println("connection error: " + ex.getMessage());
            close();
        }
    }

    // sends a raw string message to this specific client
    public void sendMessage(String message) {
        if (running && output != null) {
            output.println(message);
        }
    }

    // reads a line of text from this client (blocking operation)
    public String readMessage() throws IOException {
        if (running && input != null) {
            return input.readLine();
        }
        return null;
    }

    public boolean isRunning() {
        return running && socket.isConnected() && !socket.isClosed();
    }

    public PrintWriter getOutput() {
        return output;
    }

    public BufferedReader getInput() {
        return input;
    }

    public Socket getSocket() {
        return socket;
    }

    // closes all streams and the socket cleanly
    public void close() {
        running = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignore errors during closing
        }
    }
}