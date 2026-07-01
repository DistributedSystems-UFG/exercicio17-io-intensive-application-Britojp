import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9003;
        int poolSize = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        String dataFile = args.length > 2 ? args[2] : "data/database.txt";
        FileStore store = new FileStore(dataFile);

        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ThreadPoolServer listening on port " + port + " (pool size=" + poolSize + ")");
            while (true) {
                Socket client = serverSocket.accept();
                pool.execute(new ClientHandler(client, store));
            }
        } finally {
            pool.shutdown();
        }
    }
}
