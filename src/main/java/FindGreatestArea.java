import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedWriter;
import java.io.IOException;

public class FindGreatestArea implements HttpFunction {

    private static final Gson gson = new Gson();
    private static final int MAX_ALLOWED_RADIUS = 6250;

    // --- Member variables to hold the best result ---
    private int max;
    private String maxCoordChunk;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        // Set CORS headers to allow requests from your website
        response.appendHeader("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equals(request.getMethod())) {
            response.appendHeader("Access-Control-Allow-Methods", "POST");
            response.appendHeader("Access-Control-Allow-Headers", "Content-Type");
            response.setStatusCode(204);
            return;
        }

        SlimeRequest req;
        try {
            req = gson.fromJson(request.getReader(), SlimeRequest.class);
        } catch (JsonSyntaxException | IOException e) {
            response.setStatusCode(400, "Invalid JSON format.");
            return;
        }

        if (req.endRadius <= req.startRadius) {
            response.setStatusCode(400, "End radius must be greater than start radius.");
            return;
        }
        if (req.endRadius > MAX_ALLOWED_RADIUS) {
            response.setStatusCode(400, "Search radius cannot exceed " + MAX_ALLOWED_RADIUS + ".");
            return;
        }

        // --- Setup for Streaming ---
        response.setContentType("text/plain; charset=utf-8");
        BufferedWriter writer = response.getWriter();
        SlimeChunkChecker s = new SlimeChunkChecker(req.seed);

        // Initialize results for this request
        this.max = 0;
        this.maxCoordChunk = "";
        
        writer.write("Search started. Scanning from radius " + req.startRadius + " to " + req.endRadius + "...\n");
        writer.flush();

        // --- Spiral Search Logic ---
        for (int r = req.startRadius; r <= req.endRadius; r++) {
            // ====================================================================
            // === NEW: Progress Logging ==========================================
            // ====================================================================
            // Log progress every 25 radii, but not on the very first one.
            if (r > req.startRadius && r % 25 == 0) {
                double totalRange = req.endRadius - req.startRadius;
                double currentProgress = r - req.startRadius;
                // Avoid division by zero if start and end are the same
                int percentage = totalRange > 0 ? (int) ((currentProgress / totalRange) * 100) : 100;

                int barWidth = 20;
                int filledLength = (int) (barWidth * (percentage / 100.0));
                StringBuilder bar = new StringBuilder("[");
                for (int k = 0; k < barWidth; k++) {
                    bar.append(k < filledLength ? "#" : " ");
                }
                bar.append("]");

                String progressUpdate = String.format("%s %d%% (Scanning radius %d / %d)\n", bar.toString(), percentage, r, req.endRadius);
                writer.write(progressUpdate);
                writer.flush();
            }
            // ====================================================================

            // Handle the center chunk only on the first iteration if startRadius is 0.
            if (r == 0) {
                if (req.startRadius == 0) checkChunkAndUpdate(s, 0, 0, writer);
                continue;
            }

            // Trace the perimeter of a square with a "radius" of r.
            for (int i = -r; i < r; i++) checkChunkAndUpdate(s, i, r, writer);      // Top edge
            for (int i = r; i > -r; i--) checkChunkAndUpdate(s, r, i, writer);     // Right edge
            for (int i = r; i > -r; i--) checkChunkAndUpdate(s, i, -r, writer);    // Bottom edge
            for (int i = -r; i < r; i++) checkChunkAndUpdate(s, -r, i, writer);    // Left edge
        }
        
        writer.write("\nSearch complete! Final best location: " + this.maxCoordChunk + " with " + this.max + " slime chunks.");
    }

    // Helper method to reduce code duplication in the search loop
    private void checkChunkAndUpdate(SlimeChunkChecker s, int x, int z, BufferedWriter writer) throws IOException {
        int currentSlimeCount = numSlimeChunksInRange(s, x, z);
        if (currentSlimeCount > this.max) {
            this.max = currentSlimeCount;
            this.maxCoordChunk = x + "," + z;
            String update = "New max found: " + this.maxCoordChunk + " with " + this.max + " slime chunks.\n";
            writer.write(update);
            writer.flush();
        }
    }

    // Helper class for parsing JSON
    static class SlimeRequest {
        long seed;
        int startRadius;
        int endRadius;
    }

    public static int numSlimeChunksInRange(SlimeChunkChecker checker, int chunkX, int chunkZ) {
        int sum = 0;
        for (int j = chunkZ - 7; j <= chunkZ + 7; j++) {
            for (int i = chunkX - circleInteriorXOffset(j - chunkZ); i <= chunkX + circleInteriorXOffset(j - chunkZ); i++) {
                if (checker.isSlimeChunk(i, j)) sum++;
            }
        }
        return sum;
    }

    public static int circleInteriorXOffset(int z) {
        switch (Math.abs(z)) {
            case 7: return 0;
            case 6: return 3;
            case 5: return 4;
            case 4: return 5;
            case 3: case 2: case 1: return 6;
            default: return 7;
        }
    }
}
