import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleThreadedServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9001;
        String dataFile = args.length > 1 ? args[1] : "data/database.txt";
        FileStore store = new FileStore(dataFile);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SingleThreadedServer listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new ClientHandler(client, store).run();
            }
        }
    }
}
