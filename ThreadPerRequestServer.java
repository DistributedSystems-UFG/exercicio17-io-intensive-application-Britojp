import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadPerRequestServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9002;
        String dataFile = args.length > 1 ? args[1] : "data/database.txt";
        FileStore store = new FileStore(dataFile);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ThreadPerRequestServer listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client, store)).start();
            }
        }
    }
}
