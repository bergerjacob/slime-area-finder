import java.util.Random; 

public class SlimeChunkChecker{ 
	public static long seed;

	public SlimeChunkChecker(long s) {
		seed = s;
	}
	
    public boolean isSlimeChunk(int chunkX, int chunkZ) { 
        // the seed from /seed as a 64bit long literal

    	Random rnd = new Random(
                seed +
                (int) (chunkX * chunkX * 0x4c1906) +
                (int) (chunkX * 0x5ac0db) +
                (int) (chunkZ * chunkZ) * 0x4307a7L +
                (int) (chunkZ * 0x5f24f) ^ 0x3ad8025fL
        );

        return rnd.nextInt(10) == 0;
    } 
}