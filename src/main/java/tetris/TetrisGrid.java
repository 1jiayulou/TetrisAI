package tetris;

public class TetrisGrid {

    private int[][] grid; // 网格数组 (0 表示空，1 表示已占用)
    private int rows;
    private int cols;

    // 构造函数：初始化网格大小
    public TetrisGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new int[rows][cols]; // 创建网格
    }

    // 检查是否越界或重叠
    public boolean isValidMove(Tetromino tetromino, int newX, int newY) {
        int[][] shape = tetromino.getShapeMatrix();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) { // 检查方块部分
                    int x = newX + col;
                    int y = newY + row;

                    // 边界检查
                    if (x < 0 || x >= cols || y < 0 || y >= rows) {
                        return false; // 超出边界
                    }

                    // 重叠检查
                    if (grid[y][x] == 1) {
                        return false; // 与其他方块重叠
                    }
                }
            }
        }
        return true; // 有效位置
    }

    // 锁定方块到网格中
    public void lockTetromino(Tetromino tetromino) {
        int[][] shape = tetromino.getShapeMatrix();
        int x = tetromino.getX();
        int y = tetromino.getY();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    grid[y + row][x + col] = 1; // 标记位置为已占用
                }
            }
        }
    }

    // 检查并消除满行
    public int clearFullLines() {
        int clearedLines = 0;

        for (int row = 0; row < rows; row++) {
            boolean isFull = true;

            // 检查当前行是否已满
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == 0) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                // 消除满行，向下移动其他行
                for (int r = row; r > 0; r--) {
                    grid[r] = grid[r - 1].clone();
                }
                // 清空顶部行
                grid[0] = new int[cols];
                clearedLines++;
            }
        }
        return clearedLines; // 返回消除的行数
    }

    // 获取网格状态
    public int[][] getGrid() {
        return grid;
    }
}
