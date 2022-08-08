package supercoder79.survivalisland;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import supercoder79.survivalisland.noise.OctaveNoise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class TestIslandGen {
    public static void main(String[] args) {
        int size = 2048;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        for (int x = -(size/2); x < size/2; x++) {
            if (x % Math.max(size / 16, 1) == 0) {
                System.out.println(((x + (size/2)) / (double)size) * 100 + "%");
            }

            for (int z = -(size/2); z < size/2; z++) {
                int color = genIsland(x, z);
//                int color = genIslandCircle(x, z);

                img.setRGB(x + size/2, z + size/2, color);
            }
        }

        Path p = Paths.get("build", "island.png");
        try {
            ImageIO.write(img, "png", p.toAbsolutePath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static OctaveNoise circleNoise = new OctaveNoise(2, new Random(10), 50, 50, 0.65, 1.9, 1.2);
    private static OctaveNoise radiusNoise = new OctaveNoise(2, new Random(11), 24, 24, 14, 1.9, 1.9);
    private static OctaveNoise sXNoise = new OctaveNoise(1, new Random(101), 24, 24, 14, 1.9, 1.9);
    private static OctaveNoise sZNoise = new OctaveNoise(1, new Random(102), 24, 24, 14, 1.9, 1.9);
    private static int genIsland(int x, int z) {
        Vec2i center = findClosestCentroid(x, z);

        double ra = radiusNoise.sample(x, z);
        double cx = center.x + sXNoise.sample(x, z);
        double cz = center.z + sZNoise.sample(x, z);
        double dx = (x - cx) / (32.0 + ra);
        double dz = (z - cz) / (32.0 + ra);
        double dist = dx * dx + dz * dz;
        double dstAdd = circleNoise.sample(x, z);
        if (dist < 1 + dstAdd) {
            int col = (int)Mth.clampedMap(Math.sqrt(dist), 0, 1, 255, 0);
            return getIntFromColor(col, col, col);
        }

        // Water
        return getIntFromColor(10, 90, 180);
    }

    private static int genIslandCircle(int x, int z) {
        Vec2i center = findClosestCentroid(x, z);

        double ra = 0;
        double cx = center.x;
        double cz = center.z;
        double dx = (x - cx) / (64.0 + ra);
        double dz = (z - cz) / (64.0 + ra);
        double dist = dx * dx + dz * dz;
        if (dist < 1) {
            int col = (int)Mth.clampedMap(Math.sqrt(dist), 0, 1, 255, 0);
//            int col = (int)Mth.clampedMap(dist, 0, 1, 255, 0);
            return getIntFromColor(col, col, col);
        }

        // Water
        return getIntFromColor(10, 90, 180);

//        int acx = x / GRID_SIZE;
//        int acz = z / GRID_SIZE;

//        return getIntFromColor(0, 0, 0);
    }

    private static final int GRID_SIZE = 240;

    private static Vec2i findClosestCentroid(int x, int z) {
        int cx = x / GRID_SIZE;
        int cz = z / GRID_SIZE;

        Random random = new Random();
        Vec2i min = null;
        int minDist = Integer.MAX_VALUE;
        for (int ax = -1; ax <= 1; ax++) {
            for (int az = -1; az <= 1; az++) {
                int dx = cx + ax;
                int dz = cz + az;
                setDecorationSeed(random, 100, dx, dz);

                int rx = random.nextInt(120) + (dx * GRID_SIZE);
                int rz = random.nextInt(120) + (dz * GRID_SIZE);

                int dist = ((x - rx) * (x - rx)) + ((z - rz) * (z - rz));
                if (minDist > dist) {
                    minDist = dist;
                    min = new Vec2i(rx, rz);
                }
            }
        }

        return min;
    }

    public static void setDecorationSeed(Random r, long l, int i, int j) {
        r.setSeed(l);
        long m = r.nextLong() | 1L;
        long n = r.nextLong() | 1L;
        long o = (long)i * m + (long)j * n ^ l;
        r.setSeed(o);
    }

    private record Vec2i(int x, int z) {}

    public static int getIntFromColor(int red, int green, int blue) {
        red = Mth.clamp(red, 0, 255);
        green = Mth.clamp(green, 0, 255);
        blue = Mth.clamp(blue, 0, 255);

        red = (red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        green = (green << 8) & 0x0000FF00; //Shift green 8-bits and mask out other stuff
        blue = blue & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }
}
