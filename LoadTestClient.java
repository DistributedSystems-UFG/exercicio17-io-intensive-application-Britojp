import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestClient {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9001;
        int numClients = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int requestsPerClient = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        int maxId = args.length > 4 ? Integer.parseInt(args[4]) : 3000;

        Result result = run(host, port, numClients, requestsPerClient, maxId);
        System.out.println(result);
    }

    public static Result run(String host, int port, int numClients, int requestsPerClient, int maxId)
            throws InterruptedException {
        ExecutorService clientPool = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        long start = System.nanoTime();
        for (int t = 0; t < numClients; t++) {
            clientPool.submit(() -> {
                Random rnd = new Random();
                try {
                    for (int i = 0; i < requestsPerClient; i++) {
                        if (doRequest(host, port, rnd.nextInt(maxId))) {
                            success.incrementAndGet();
                        } else {
                            failure.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long elapsedNanos = System.nanoTime() - start;

        clientPool.shutdown();
        clientPool.awaitTermination(10, TimeUnit.SECONDS);

        int total = numClients * requestsPerClient;
        double elapsedSec = elapsedNanos / 1_000_000_000.0;
        double throughput = total / elapsedSec;
        return new Result(total, success.get(), failure.get(), elapsedSec, throughput);
    }

    private static boolean doRequest(String host, int port, int id) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("GET " + id);
            String response = in.readLine();
            return response != null && response.startsWith("OK");
        } catch (IOException e) {
            return false;
        }
    }

    public static final class Result {
        public final int totalRequests;
        public final int success;
        public final int failure;
        public final double elapsedSeconds;
        public final double throughputPerSecond;

        Result(int totalRequests, int success, int failure, double elapsedSeconds, double throughputPerSecond) {
            this.totalRequests = totalRequests;
            this.success = success;
            this.failure = failure;
            this.elapsedSeconds = elapsedSeconds;
            this.throughputPerSecond = throughputPerSecond;
        }

        @Override
        public String toString() {
            return String.format(
                    "Total requests: %d (ok=%d, fail=%d) | Elapsed: %.3f s | Throughput: %.2f req/s",
                    totalRequests, success, failure, elapsedSeconds, throughputPerSecond);
        }
    }
}
