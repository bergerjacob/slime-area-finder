public class LocalTester {

    private static int maxCount = -1;
    private static FindGreatestArea.Chunk bestChunk = null;

    public static void main(String[] args) {
        // --- CONFIGURE YOUR TEST HERE ---
        long seed = -3984982729468871050L;
        int startRadius = 2700;
        int endRadius = 3000;

        System.out.println("--- Starting Local Slime Chunk Test ---");
        System.out.println("Seed: " + seed);
        System.out.println("Searching from radius " + startRadius + " to " + endRadius + " chunks.\n");

        for (int r = startRadius; r <= endRadius; r++) {
            if (r > startRadius && r % 50 == 0) {
                System.out.println("... Reached radius " + r + " ...");
            }

            if (r == 0) {
                if (startRadius == 0) {
                    checkAndSetMax(seed, 0, 0);
                }
                continue;
            }

            // Top and bottom edges
            for (int x = -r; x <= r; x++) {
                checkAndSetMax(seed, x, r);
                checkAndSetMax(seed, x, -r);
            }
            // Left and right edges (excluding corners)
            for (int z = -r + 1; z < r; z++) {
                checkAndSetMax(seed, r, z);
                checkAndSetMax(seed, -r, z);
            }
        }

        System.out.println("\n--- TEST COMPLETE ---");
        if (bestChunk != null) {
            System.out.println("Final Optimal Chunk: (" + bestChunk.x + ", " + bestChunk.z + ")");
            System.out.println("Slime Chunks in Range: " + maxCount);
        } else {
            System.out.println("No slime chunks found in the specified range.");
        }
    }

    private static void checkAndSetMax(long seed, int x, int z) {
        int count = FindGreatestArea.numSlimeChunksInRange(seed, x, z);
        
        if (count > maxCount) {
            maxCount = count;
            bestChunk = new FindGreatestArea.Chunk(x, z);
            System.out.printf("===> NEW MAX FOUND! Chunk (%d, %d) has %d slime chunks in range! <===%n",
                bestChunk.x, bestChunk.z, maxCount);
        }
    }
}
