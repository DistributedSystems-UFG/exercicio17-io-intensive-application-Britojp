import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;


public class BenchmarkRunner {

    private static final int BASE_PORT = 9101;
    private static final String DATA_FILE = "data/database.txt";
    private static final int MAX_ID = 3000;

    public static void main(String[] args) throws Exception {
        int numClients = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        int requestsPerClient = args.length > 1 ? Integer.parseInt(args[1]) : 20;

        Map<String, String[]> servers = new LinkedHashMap<>();
        servers.put("Single-threaded", new String[] {"SingleThreadedServer", String.valueOf(BASE_PORT)});
        servers.put("Thread-per-request", new String[] {"ThreadPerRequestServer", String.valueOf(BASE_PORT + 1)});
        servers.put("Thread pool (20)", new String[] {"ThreadPoolServer", String.valueOf(BASE_PORT + 2), "20"});

        System.out.printf("Benchmark config: clients=%d, requests/client=%d, total requests=%d%n%n",
                numClients, requestsPerClient, numClients * requestsPerClient);

        Map<String, LoadTestClient.Result> results = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : servers.entrySet()) {
            String label = entry.getKey();
            String[] cmdArgs = entry.getValue();
            int port = Integer.parseInt(cmdArgs[1]);

            System.out.println("=== " + label + " ===");
            Process process = startServer(cmdArgs);
            try {
                waitForPort("localhost", port, 5000);
                LoadTestClient.Result result = LoadTestClient.run("localhost", port, numClients, requestsPerClient, MAX_ID);
                results.put(label, result);
                System.out.println(result);
            } finally {
                process.destroy();
                process.waitFor();
            }
            System.out.println();
        }

        System.out.println("=== Summary ===");
        System.out.printf("%-20s %15s%n", "Version", "Throughput (req/s)");
        for (Map.Entry<String, LoadTestClient.Result> entry : results.entrySet()) {
            System.out.printf("%-20s %15.2f%n", entry.getKey(), entry.getValue().throughputPerSecond);
        }
    }

    private static Process startServer(String[] cmdArgs) throws IOException {
        // cmdArgs = { mainClass, port [, poolSize] }; append the data file path as the last main() arg.
        String[] mainArgs = new String[cmdArgs.length + 1];
        System.arraycopy(cmdArgs, 0, mainArgs, 0, cmdArgs.length);
        mainArgs[cmdArgs.length] = DATA_FILE;

        String[] command = new String[mainArgs.length + 3];
        command[0] = "java";
        command[1] = "-cp";
        command[2] = ".";
        System.arraycopy(mainArgs, 0, command, 3, mainArgs.length);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return builder.start();
    }

    private static void waitForPort(String host, int port, long timeoutMs) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 200);
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IOException("Server on port " + port + " did not start within " + timeoutMs + "ms");
    }
}
