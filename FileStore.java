import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileStore {

    private static final int SIMULATED_LATENCY_MS = Integer.getInteger("io.delay.ms", 15);

    private final String filePath;

    public FileStore(String filePath) {
        this.filePath = filePath;
    }
    public String readRecord(int id) throws IOException {
        try (BufferedReader raf = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = raf.readLine()) != null) {
                int sep = line.indexOf(',');
                if (sep <= 0) {
                    continue;
                }
                try {
                    if (Integer.parseInt(line.substring(0, sep)) == id) {
                        simulateDiskLatency();
                        return line.substring(sep + 1);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        simulateDiskLatency();
        return null;
    }

    private void simulateDiskLatency() {
        if (SIMULATED_LATENCY_MS > 0) {
            try {
                Thread.sleep(SIMULATED_LATENCY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
