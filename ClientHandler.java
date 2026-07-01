import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final FileStore store;

    public ClientHandler(Socket socket, FileStore store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            String request = in.readLine();
            if (request == null) {
                return;
            }
            if (request.startsWith("GET ")) {
                handleGet(request.substring(4).trim(), out);
            } else {
                out.println("ERROR unknown command");
            }
        } catch (IOException e) {
            System.err.println("Error handling client " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    private void handleGet(String idText, PrintWriter out) {
        try {
            int id = Integer.parseInt(idText);
            String value = store.readRecord(id);
            out.println(value != null ? "OK " + value : "NOT_FOUND");
        } catch (NumberFormatException e) {
            out.println("ERROR invalid id");
        } catch (IOException e) {
            out.println("ERROR " + e.getMessage());
        }
    }
}
