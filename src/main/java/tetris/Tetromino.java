package tetris;

import javafx.scene.paint.Color;

import java.util.Arrays;

public class Tetromino {

    private static int idCounter = 0;

    private static final int[][][] SHAPES = {
            // I
            {{1, 1, 1, 1}},
            // O
            {{1, 1}, {1, 1}},
            // T
            {{0, 1, 0}, {1, 1, 1}},
            // S
            {{0, 1, 1}, {1, 1, 0}},
            // Z
            {{1, 1, 0}, {0, 1, 1}},
            // L
            {{1, 0}, {1, 0}, {1, 1}},
            // J
            {{0, 1}, {0, 1}, {1, 1}}
    };
    // Assign Different Color
    private static final Color[] COLORS = {
            Color.CYAN, Color.YELLOW, Color.PURPLE,
            Color.GREEN, Color.RED, Color.ORANGE, Color.BLUE
    };
    private int id;
    private ShapeType type;       // 方块类型
    private int rotation;         // 当前旋转状态
    private int[][] shapeMatrix;  // 当前形状矩阵
    private Color color;          // 方块颜色
    private int x,y;              // x 通常代表列坐标（column index），即从左到右的偏移量。y 通常代表行坐标（row index），即从上到下的偏移量。


    public Tetromino(ShapeType type) {
        this.type = type;
        this.rotation = 0; // 初始旋转状态
        this.shapeMatrix = SHAPES[type.ordinal()];
        this.color = COLORS[type.ordinal()];
        this.x = 4;
        this.y = 0;
        this.id = idCounter++;
    }

    public Color getColor() {
        return color;
    }

    public int[][] getShapeMatrix() {
        return shapeMatrix;
    }

    public void setShapeMatrix(int[][] finalShape) {
        int[][] temp = new int[finalShape.length][finalShape[0].length];
        for (int j = 0; j<temp.length; j++) temp[j] = Arrays.copyOf(finalShape[j],finalShape[j].length);
        shapeMatrix = temp;
    }

    public int getId() {
        return id;
    }

    public ShapeType getType() {
        return type;
    }

    public int getHeight() {
        return shapeMatrix.length;
    }

    public int getWidth() {
        return shapeMatrix[0].length;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveLeft() {
        x--; // 向左移动
    }

    public void moveRight() {
        x++; // 向右移动
    }

    public void moveDown() {
        y++; // 向下移动
    }

    public void rotate() {
        int rows = shapeMatrix.length;
        int cols = shapeMatrix[0].length;
        int[][] rotated = new int[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - i - 1] = shapeMatrix[i][j];
            }
        }
        shapeMatrix = rotated; // 更新形状矩阵
    }
}
