package supercoder79.survivalisland;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import supercoder79.survivalisland.noise.IslandContinentalNoise;
import supercoder79.survivalisland.noise.OctaveNoise;
import supercoder79.survivalisland.noise.OctaveNoiseRecipe;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RenderIslandNoise {
    private static final int SIZE = 2048;
    private static final double FREQUENCY = 4.0;

    public static void main(String[] args) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < SIZE; x++) {
            if (x % Math.max(SIZE / 16, 1) == 0) {
                System.out.println((x / (double) SIZE) * 100 + "%");
            }

            for (int z = 0; z < SIZE; z++) {
                double value = ISLAND_CONTINENTAL_NOISE.compute(FREQUENCY * (x - SIZE / 2.0), FREQUENCY * (z - SIZE / 2.0));
                int color;
                if (value < 0) {
                    value /= -0.4;
                    color = getIntFromColor((int)Mth.lerp(value, 255, 10), (int)Mth.lerp(value, 255, 90), (int)Mth.lerp(value, 255, 180));
                }
                else {
                    color = getIntFromColor((int)Mth.lerp(value, 0, 127), (int)Mth.lerp(value, 127, 255), (int)Mth.lerp(value, 0, 127));
                }

                img.setRGB(x, z, color);
            }
        }

        JFrame frame = new JFrame();
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(img));
        frame.add(imageLabel);
        JButton saveButton = new JButton();
        saveButton.setText("Save");
        saveButton.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("./build"));
            fileChooser.setSelectedFile(new File("island.png"));
            while (true) {
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (!file.getAbsolutePath().endsWith(".png")) {
                        file = new File(file.getAbsolutePath() + ".png");
                    }
                    if (file.exists()) {
                        if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                                frame, "Replace `" + file.getName() + "`?", "Replace file?",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                        ) {
                            continue;
                        }
                    }
                    try {
                        ImageIO.write(img, "png", file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        });
        frame.add(saveButton, BorderLayout.PAGE_START);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    public static int getIntFromColor(int red, int green, int blue) {
        red = Mth.clamp(red, 0, 255);
        green = Mth.clamp(green, 0, 255);
        blue = Mth.clamp(blue, 0, 255);

        red = (red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        green = (green << 8) & 0x0000FF00; //Shift green 8-bits and mask out other stuff
        blue = blue & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    private static OctaveNoise domainWarpNoise = new OctaveNoiseRecipe(1, 24, 24, 14, 1.9, 1.9).makeLive(new XoroshiroRandomSource(101));
    private static OctaveNoise rangeVariationNoise = new OctaveNoiseRecipe(1, 280, 24, 1, 1.9, 1.9).makeLive(new XoroshiroRandomSource(102));
    private static final long SEED = 7;

    private static final double ISLAND_RADIUS = 128.0;
    private static final int ISLAND_SEPARATION_DISTANCE = 800;

    private static final double ISLAND_UNDERWATER_FALLOFF_DISTANCE_RATIO_TO_SIZE = 9;

    private static final float TARGET_MIN_VALUE_A = -0.25f;
    private static final float TARGET_MAX_VALUE_A = 0.7f;
    private static final float TARGET_MIN_VALUE_B = -1.0f;
    private static final float TARGET_MAX_VALUE_B = 1.4f;

    private static final IslandContinentalNoise ISLAND_CONTINENTAL_NOISE = new IslandContinentalNoise(SEED,
            ISLAND_RADIUS, ISLAND_SEPARATION_DISTANCE,
            TARGET_MIN_VALUE_A, TARGET_MAX_VALUE_A,
            TARGET_MIN_VALUE_B, TARGET_MAX_VALUE_B,
            ISLAND_UNDERWATER_FALLOFF_DISTANCE_RATIO_TO_SIZE,
            domainWarpNoise, rangeVariationNoise
    );
}