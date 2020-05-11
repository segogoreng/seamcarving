import java.awt.Color;

import edu.princeton.cs.algs4.Picture;
import edu.princeton.cs.algs4.StdOut;


public class SeamCarver {

    private static final double BORDER_ENERGY = 1000.0;

    private Picture picture;
    private double[][] energyMatrix;
    private double[][] distTo;
    private int[][] edgeTo;

    // create a seam carver object based on the given picture
    public SeamCarver(Picture picture) {
        if (picture == null) throw new IllegalArgumentException();

        this.picture = new Picture(picture);
    }

    // current picture
    public Picture picture() {
        return new Picture(picture);
    }

    // width of current picture
    public int width() {
        return picture.width();
    }

    // height of current picture
    public int height() {
        return picture.height();
    }

    // energy of pixel at column x and row y
    public double energy(int x, int y) {
        if (x < 0 || x > width() - 1 || y < 0 || y > height() - 1) {
            throw new IllegalArgumentException();
        }

        if (x == 0 || x == width() - 1 || y == 0 || y == height() - 1) {
            return BORDER_ENERGY;
        }

        Color left = picture.get(x - 1, y);
        Color right = picture.get(x + 1, y);
        Color top = picture.get(x, y - 1);
        Color bottom = picture.get(x, y + 1);
        
        int rx = left.getRed() - right.getRed();
        int gx = left.getGreen() - right.getGreen();
        int bx = left.getBlue() - right.getBlue();
        int dx2 = rx * rx + gx * gx + bx * bx;

        int ry = top.getRed() - bottom.getRed();
        int gy = top.getGreen() - bottom.getGreen();
        int by = top.getBlue() - bottom.getBlue();
        int dy2 = ry * ry + gy * gy + by * by;

        return Math.sqrt(dx2 + dy2);
    }

    // sequence of indices for horizontal seam
    public int[] findHorizontalSeam() {
        buildEnergyMatrix();
        initialize(true);
        int w = width(), h = height();
        int[] seam = new int[w];

        for (int x = 0; x < w - 1; x++) {
            for (int y = 0; y < h; y++) {
                relaxHorizontally(x, y, x + 1, y);
                if (y > 0) relaxHorizontally(x, y, x + 1, y - 1);
                if (y < h - 1) relaxHorizontally(x, y, x + 1, y + 1);
            }
        }

        double minDist = Double.POSITIVE_INFINITY;
        int minY = 0;
        for (int y = 0; y < h; y++) {
            if (distTo[w-1][y] < minDist) {
                minDist = distTo[w-1][y];
                minY = y;
            }
        }

        seam[w-1] = minY;
        for (int x = w-2; x >= 0; x--) {
            seam[x] = edgeTo[x+1][seam[x+1]];
        }

        cleanUp();
        return seam;
    }

    // sequence of indices for vertical seam
    public int[] findVerticalSeam() {
        buildEnergyMatrix();
        initialize(false);
        int w = width(), h = height();
        int[] seam = new int[h];

        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w; x++) {
                relaxVertically(x, y, x, y + 1);
                if (x > 0) relaxVertically(x, y, x - 1, y + 1);
                if (x < w - 1) relaxVertically(x, y, x + 1, y + 1);
            }
        }

        double minDist = Double.POSITIVE_INFINITY;
        int minX = 0;
        for (int x = 0; x < w; x++) {
            if (distTo[x][h-1] < minDist) {
                minDist = distTo[x][h-1];
                minX = x;
            }
        }

        seam[h-1] = minX;
        for (int y = h-2; y >= 0; y--) {
            seam[y] = edgeTo[seam[y+1]][y+1];
        }

        cleanUp();
        return seam;
    }

    // remove horizontal seam from current picture
    public void removeHorizontalSeam(int[] seam) {
        if (seam == null || seam.length != width()) throw new IllegalArgumentException();
        if (height() <= 1) throw new IllegalArgumentException();
        if (!isSeamValid(seam, height() - 1)) throw new IllegalArgumentException();

        Picture updatedPicture = new Picture(width(), height() - 1);
        for (int x = 0; x < updatedPicture.width(); x++) {
            for (int y = 0; y < updatedPicture.height(); y++) {
                if (y < seam[x]) {
                    updatedPicture.setRGB(x, y, picture.getRGB(x, y));
                } else {
                    updatedPicture.setRGB(x, y, picture.getRGB(x, y + 1));
                }
            }
        }

        picture = updatedPicture;
    }

    // remove vertical seam from current picture
    public void removeVerticalSeam(int[] seam) {
        if (seam == null || seam.length != height()) throw new IllegalArgumentException();
        if (width() <= 1) throw new IllegalArgumentException();
        if (!isSeamValid(seam, width() - 1)) throw new IllegalArgumentException();

        Picture updatedPicture = new Picture(width() - 1, height());
        for (int x = 0; x < updatedPicture.width(); x++) {
            for (int y = 0; y < updatedPicture.height(); y++) {
                if (x < seam[y]) {
                    updatedPicture.setRGB(x, y, picture.getRGB(x, y));
                } else {
                    updatedPicture.setRGB(x, y, picture.getRGB(x + 1, y));
                }
            }
        }

        picture = updatedPicture;
    }

    //  unit testing (optional)
    public static void main(String[] args) {
        SeamCarver seamCarver = new SeamCarver(new Picture(args[0]));
        StdOut.println(seamCarver.energy(1, 1));

        int[] seam = seamCarver.findVerticalSeam();
        for (int i = 0; i < seam.length; i++) {
            StdOut.print(seam[i] + " -> ");
        }
        StdOut.println();
    }

    private boolean isSeamValid(int[] seam, int upperLimit) {
        int last = seam[0];
        for (int i = 0; i < seam.length; i++) {
            if (seam[i] < 0 || seam[i] > upperLimit) return false;
            int diff = seam[i] - last;
            if (diff < -1 || diff > 1) return false;
            last = seam[i];
        }
        return true;
    }

    private void buildEnergyMatrix() {
        int w = width(), h = height();
        energyMatrix = new double[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                energyMatrix[x][y] = energy(x, y);
            }
        }
    }

    private void initialize(boolean horizontal) {
        int w = width(), h = height();
        distTo = new double[w][h];
        edgeTo = new int[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (horizontal && x == 0) distTo[x][y] = BORDER_ENERGY;
                else if (!horizontal && y == 0) distTo[x][y] = BORDER_ENERGY;
                else distTo[x][y] = Double.POSITIVE_INFINITY;
                edgeTo[x][y] = -1;
            }
        }
    }

    private void relaxHorizontally(int fromX, int fromY, int toX, int toY) {
        relax(fromX, fromY, toX, toY, true);
    }

    private void relaxVertically(int fromX, int fromY, int toX, int toY) {
        relax(fromX, fromY, toX, toY, false);
    }
    
    private void relax(int fromX, int fromY, int toX, int toY, boolean horizontal) {
        if (distTo[fromX][fromY] + energyMatrix[toX][toY] < distTo[toX][toY]) {
            distTo[toX][toY] = distTo[fromX][fromY] + energyMatrix[toX][toY];
            edgeTo[toX][toY] = horizontal ? fromY : fromX;
        }
    }

    private void cleanUp() {
        energyMatrix = null;
        distTo = null;
        edgeTo = null;
    }

}