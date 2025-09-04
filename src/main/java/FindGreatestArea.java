import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.*;

public class FindGreatestArea {

    // --- Core Minecraft Logic (ported from your original file) ---

    private static boolean isSlimeChunk(long seed, int x, int z) {
        Random rnd = new Random(
            seed +
            (long) (x * x * 0x4c1906) +
            (long) (x * 0x5ac0db) +
            (long) (z * z) * 0x4307a7L +
            (long) (z * 0x5f24f) ^ 0x3ad8025fL
        );
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
            case 7: return 0;
            case 6: return 3;
            case 5: return 4;
            case 4: return 5;
            case 3: case 2: case 1: return 6;
            default: return 7;
        }
    }

    // --- Helper classes for handling JSON data ---
    static class RequestData {
        long seed;
        int startRadius;
        int endRadius;
    }

    static class ResultData {
        Chunk optimalChunk;
        int slimeChunksInRange;
        ResultData(Chunk optimalChunk, int slimeChunksInRange) {
            this.optimalChunk = optimalChunk;
            this.slimeChunksInRange = slimeChunksInRange;
        }
    }

    static class Chunk {
        int x, z;
        Chunk(int x, int z) { this.x = x; this.z = z; }
    }

    public static void main(String[] args) {
        Gson gson = new Gson();
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));

        get("/", (request, response) -> "Slime Chunk Checker server is alive!");

        // --- Endpoint for finding the optimal chunk to stand in ---
        post("/find-optimal-chunk", (request, response) -> {
            response.type("application/json");
            try {
                RequestData data = gson.fromJson(request.body(), RequestData.class);

                // Safety cap to prevent timeouts
                if (data.endRadius > 2500) {
                    data.endRadius = 2500;
                }
                if (data.startRadius < 0) {
                    data.startRadius = 0;
                }

                int maxCount = -1;
                Chunk bestChunk = null;

                // --- Spiral Search Logic (ported from your original file) ---
                for (int r = data.startRadius; r <= data.endRadius; r++) {
                    if (r == 0) {
                        if (data.startRadius == 0) {
                            int count = numSlimeChunksInRange(data.seed, 0, 0);
                            if (count > maxCount) {
                                maxCount = count;
                                bestChunk = new Chunk(0, 0);
                            }
                        }
                        continue;
                    }
                    // Trace the perimeter of a square
                    for (int i = -r; i < r; i++) { // Top edge
                        int count = numSlimeChunksInRange(data.seed, i, r);
                        if (count > maxCount) { maxCount = count; bestChunk = new Chunk(i, r); }
                    }
                    for (int i = r; i > -r; i--) { // Right edge
                        int count = numSlimeChunksInRange(data.seed, r, i);
                        if (count > maxCount) { maxCount = count; bestChunk = new Chunk(r, i); }
                    }
                    for (int i = r; i > -r; i--) { // Bottom edge
                        int count = numSlimeChunksInRange(data.seed, i, -r);
                        if (count > maxCount) { maxCount = count; bestChunk = new Chunk(i, -r); }
                    }
                    for (int i = -r; i < r; i++) { // Left edge
                        int count = numSlimeChunksInRange(data.seed, -r, i);
                        if (count > maxCount) { maxCount = count; bestChunk = new Chunk(-r, i); }
                    }
                }

                return gson.toJson(new ResultData(bestChunk, maxCount));

            } catch (Exception e) {
                response.status(500);
                return gson.toJson(Map.of("error", "An internal server error occurred: " + e.getMessage()));
            }
        });
    }
}
