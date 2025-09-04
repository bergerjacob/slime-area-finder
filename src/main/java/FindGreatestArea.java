import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FindGreatestArea {

    /**
     * This is the slime chunk checking logic, moved from your SlimeChunkChecker class.
     * It determines if a chunk at the given coordinates is a slime chunk for a specific seed.
     */
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

    public static void main(String[] args) {
        Gson gson = new Gson();

        // Get the port number assigned by Render
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));

        // A simple root endpoint to confirm the server is up
        get("/", (request, response) -> "Slime Chunk Checker server is alive!");

        // Define the primary endpoint. Example usage: /isSlimeChunk?seed=12345&x=10&z=-20
        get("/isSlimeChunk", (request, response) -> {
            response.type("application/json");

            // Get query parameters from the URL
            String seedStr = request.queryParams("seed");
            String xStr = request.queryParams("x");
            String zStr = request.queryParams("z");

            // --- Input Validation ---
            if (seedStr == null || xStr == null || zStr == null) {
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", "Missing required query parameters: seed, x, z"));
            }

            try {
                // --- Parse Parameters ---
                long seed = Long.parseLong(seedStr);
                int x = Integer.parseInt(xStr);
                int z = Integer.parseInt(zStr);

                // --- Call the Core Logic ---
                boolean isSlime = isSlimeChunk(seed, x, z);

                // --- Create a Success Response ---
                Map<String, Object> result = new HashMap<>();
                result.put("seed", seed);
                result.put("x", x);
                result.put("z", z);
                result.put("isSlimeChunk", isSlime);

                response.status(200); // OK
                return gson.toJson(result);

            } catch (NumberFormatException e) {
                // Handle cases where parameters are not valid numbers
                response.status(400); // Bad Request
                return gson.toJson(Map.of("error", "Query parameters 'seed', 'x', and 'z' must be valid numbers."));
            }
        });
    }
}
