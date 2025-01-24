import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, List<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public static synchronized void joinRoom(String roomID, ClientHandler client) {
        rooms.putIfAbsent(roomID, new ArrayList<>());
        rooms.get(roomID).add(client);
    }

    public static synchronized void leaveRoom(String roomID, ClientHandler client) {
        if (rooms.containsKey(roomID)) {
            rooms.get(roomID).remove(client);
            if (rooms.get(roomID).isEmpty()) {
                rooms.remove(roomID);
            }
        }
    }

    public static synchronized void broadcast(String roomID, String message) {
        if (rooms.containsKey(roomID)) {
            for (ClientHandler client : rooms.get(roomID)) {
                client.sendMessage(message);
            }
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String username;
    private String roomID;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your username:");
            username = in.readLine();

            out.println("Enter room ID to join:");
            roomID = in.readLine();

            ChatServer.joinRoom(roomID, this);
            ChatServer.broadcast(roomID, username + " has joined the room!");

            String message;
            while ((message = in.readLine()) != null) {
                ChatServer.broadcast(roomID, username + ": " + message);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            ChatServer.leaveRoom(roomID, this);
            ChatServer.broadcast(roomID, username + " has left the room.");
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}

