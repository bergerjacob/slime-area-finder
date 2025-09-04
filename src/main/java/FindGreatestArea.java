import static spark.Spark.*;
import com.google.gson.Gson;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FindGreatestArea {

    // --- Core Minecraft Logic ---
    private static boolean isSlimeChunk(long seed, int x, int z) {
        Random rnd = new Random(
            seed + (long) (x * x * 0x4c1906) + (long) (x * 0x5ac0db) +
            (long) (z * z) * 0x4307a7L + (long) (z * 0x5f24f) ^ 0x3ad8025fL);
        return rnd.nextInt(10) == 0;
    }

    private static int numSlimeChunksInRange(long seed, int chunkX, int chunkZ) {
        int sum = 0;
        for (int j = chunkZ - 7; j <= chunkZ + 7; j++) {
            for (int i = chunkX - circleInteriorXOffset(j - chunkZ); i <= chunkX + circleInteriorXOffset(j - chunkZ); i++) {
                if (isSlimeChunk(seed, i, j)) sum++;
            }
        }
        return sum;
    }

    private static int circleInteriorXOffset(int z) {
        switch (Math.abs(z)) {
            case 7: return 0; case 6: return 3; case 5: return 4;
            case 4: return 5; case 3: case 2: case 1: return 6;
            default: return 7;
        }
    }

    // --- Helper classes for data structure ---
    static class RequestData { long seed; int startRadius; int endRadius; }
    static class Chunk { int x, z; Chunk(int x, int z) { this.x = x; this.z = z; } }
    
    static class Result implements Comparable<Result> {
        Chunk chunk;
        int slimeCount;
        Result(Chunk chunk, int slimeCount) { this.chunk = chunk; this.slimeCount = slimeCount; }
        @Override
        public int compareTo(Result other) { return Integer.compare(this.slimeCount, other.slimeCount); }
    }

    public static void main(String[] args) {
        final int MAX_RADIUS = 6250;
        final int TOP_RESULTS_COUNT = 5;
        Gson gson = new Gson();
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));

        // Enable CORS
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "Content-Type");
            res.header("Access-Control-Allow-Methods", "POST, OPTIONS");
        });
        options("/*", (req, res) -> { res.status(204); return ""; });

        get("/", (req, res) -> "Slime Chunk Checker server is alive!");

        post("/find-optimal-chunk", (req, res) -> {
            res.type("text/plain; charset=utf-8");
            OutputStream os = res.raw().getOutputStream();
            
            try {
                RequestData data = gson.fromJson(req.body(), RequestData.class);

                if (data.endRadius > MAX_RADIUS) {
                    throw new IllegalArgumentException("Search radius cannot exceed " + MAX_RADIUS + " chunks.");
                }

                PriorityQueue<Result> topResults = new PriorityQueue<>(TOP_RESULTS_COUNT);

                os.write("{\"message\":\"Search started...\"}\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
                
                for (int r = data.startRadius; r <= data.endRadius; r++) {
                    // --- THIS SECTION IS NEW: Stream progress updates ---
                    if (r > data.startRadius && r % 25 == 0) {
                        os.write(gson.toJson(Map.of("progress", Map.of("currentRadius", r, "endRadius", data.endRadius)))
                            .getBytes(StandardCharsets.UTF_8));
                        os.write("\n".getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                    // --- END NEW SECTION ---

                    LambdaCheck chunkChecker = (x, z) -> {
                        int count = numSlimeChunksInRange(data.seed, x, z);
                        if (topResults.size() < TOP_RESULTS_COUNT || count > topResults.peek().slimeCount) {
                            if (topResults.size() == TOP_RESULTS_COUNT) {
                                topResults.poll();
                            }
                            topResults.add(new Result(new Chunk(x, z), count));
                            
                            List<Result> sortedResults = new ArrayList<>(topResults);
                            sortedResults.sort(Collections.reverseOrder());
                            os.write(gson.toJson(Map.of("results", sortedResults)).getBytes(StandardCharsets.UTF_8));
                            os.write("\n".getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        }
                    };

                    if (r == 0 && data.startRadius == 0) {
                        chunkChecker.check(0, 0);
                        continue;
                    }
                    if (r > 0) {
                        for (int i = -r; i < r; i++) chunkChecker.check(i, r);
                        for (int i = r; i > -r; i--) chunkChecker.check(r, i);
                        for (int i = r; i > -r; i--) chunkChecker.check(i, -r);
                        for (int i = -r; i < r; i++) chunkChecker.check(-r, i);
                    }
                }
                os.write("{\"message\":\"Search complete!\"}\n".getBytes(StandardCharsets.UTF_8));

            } catch (Exception e) {
                res.status(400);
                os.write(gson.toJson(Map.of("error", e.getMessage())).getBytes(StandardCharsets.UTF_8));
            } finally {
                os.close();
            }
            return "";
        });
    }

    @FunctionalInterface
    interface LambdaCheck { void check(int x, int z) throws Exception; }
}
