package tetris;

import javafx.application.Platform;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class AIControllerV1 implements Runnable{
    private boolean running = true;
    private int currentId = -1;
    private Tetromino currentTetromino ;
    private int[][] currentGrid;
    private int highest;
    private boolean isRunning = true;
    private boolean isPaused = false;

    public static class DestinationResult {
        public int[] bestMove;
        public List<Integer> bestMovement;

        public DestinationResult(int[] bestMove, List<Integer> bestMovement) {
            this.bestMove = bestMove;
            this.bestMovement = bestMovement;
        }

        public int[] getBestMove() {
            return bestMove;
        }

        public List<Integer> getBestMovement() {
            return bestMovement;
        }

    }

    @Override
    public void run() {
        try {
            while (running) {
                Thread.sleep(1000);
                getCurrentInfo();
                Platform.runLater(() -> { // 在 JavaFX 主线程上更新 UI
                    if (currentTetromino.getId() != currentId) {
                        currentId = currentTetromino.getId();
                        moveToBestDestination();
                    }
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 外部调用，让 AI 线程整个停止
    public void stop() {
        isRunning = false;
        // 为了避免AI阻塞在 wait 状态，先把 isPaused 设为 false 并调用 notify
        setPaused(false);
    }

    // 外部调用，暂停 AI
    public void pauseAI() {
        setPaused(true);
    }

    // 内部方法：切换 isPaused 并通知线程
    private synchronized void setPaused(boolean paused) {
        this.isPaused = paused;
        if (!isPaused) {
            notify(); // 唤醒在 wait() 中阻塞的线程
        }
    }

    // 外部调用，恢复 AI
    public void resumeAI() {
        setPaused(false);
    }

    private void getCurrentInfo() {
        currentGrid = TetrisMain.getCurrentGrid();
        currentTetromino = TetrisMain.getCurrentTetromino();
    }

    private void moveToBestDestination() {
        // bestMove 存储最优解的 (row, col)
        //   row = bestMove[0], col = bestMove[1]
        // 初始化时先给它一个极小值或非法值
        int[] bestMove = {-1, -1};

        // 用于存储最优解对应的形状矩阵 & 移动序列
        int[][] bestShape = copyMatrix(currentTetromino.getShapeMatrix());
        List<Integer> bestMovement = new ArrayList<>();

        // 这个是“当前最好的左下角行号”，我们想要尽量大
        int bestBottom = -1;

        // 这个是“当前最好的移动序列长度”，如果底边行号一样才比较步数
        int bestMoveSteps = Integer.MAX_VALUE;

        // 先取一个副本出来，以便在循环里做旋转尝试
        int[][] rotatedShape = copyMatrix(currentTetromino.getShapeMatrix());

        // 尝试 0~3 次旋转
        for (int i = 0; i < 4; i++) {
            // 让 AI 算出在此形状朝向下的最佳落点
            DestinationResult destinationResult = getBestDestination(rotatedShape);
            int[] move = destinationResult.getBestMove();          // (row, col)
            List<Integer> tempMovement = destinationResult.getBestMovement();

            // 如果 getBestDestination 返回的 move 是无效值(例如 row < 0)，可做跳过
            // 这里示例假设如果 row = -1, 表示无效
            if (move[0] < 0) {
                // 说明这次旋转形状无法放置到任何位置
                // 直接跳过
            } else {
                // 计算“当前形状左下角”的行号
                int currentBottom = move[0] + (rotatedShape.length - 1);

                // 先比谁更“低”（currentBottom 较大者胜）
                if (currentBottom > bestBottom) {
                    bestBottom = currentBottom;
                    bestMove = new int[]{move[0], move[1]};
                    bestShape = copyMatrix(rotatedShape);
                    bestMovement = List.copyOf(tempMovement);
                    bestMoveSteps = tempMovement.size();
                }
                // 如果两者 bottom 一样低，则比谁的移动步数更少
                else if (currentBottom == bestBottom) {
                    if (tempMovement.size() < bestMoveSteps) {
                        bestMove = new int[]{move[0], move[1]};
                        bestShape = copyMatrix(rotatedShape);
                        bestMovement = List.copyOf(tempMovement);
                        bestMoveSteps = tempMovement.size();
                    }
                    // 如果还想再继续比较“更靠左”或“更靠右”，可在这里再加一层比较
                }
            }

            // 把 rotatedShape 再旋转一次
            rotatedShape = rotateOnce(rotatedShape);
        }

        // 最终将最优形状赋值给当前 Tetromino
        currentTetromino.setShapeMatrix(bestShape);

        // 如果你想真正执行移动或直接把方块摆到最终位置，可以在这里做：
        // executeMove(bestMovement);
         currentTetromino.setPosition(bestMove[1], bestMove[0]);
    }

    private DestinationResult getBestDestination(int[][] shape) {
        int totalRows = currentGrid.length;
        int totalCols = currentGrid[0].length;

        // shape 本身的高度、宽度
        int shapeRows = shape.length;
        int shapeCols = shape[0].length;

        // 用于记录“最佳落点”和“最佳移动序列”
        // bestMove[0] = 最终 row，bestMove[1] = 最终 col
        int[] bestMove = new int[]{-1, -1};
        List<Integer> bestMovement = new ArrayList<>();

        // 用来比较哪个落点“更好”的指标
        // 这里示例用 “谁放得更低 就更好”
        int bestRowSoFar = -1;

        // 从左到右遍历列
        for (int col = 0; col <= totalCols - shapeCols; col++) {
            // 1) 找到在该 col 下能放到的最底 row
            int row = findLowestRow(shape, col);
            if (row < 0) {
                // 说明在这一列根本放不下，直接跳过
                continue;
            }

            // 2) 找到最低点后，尝试看看能否再向左移动
            int shiftedCol = shiftLeftIfPossible(shape, row, col);
            // shiftedCol 一般 <= col

            // 3) 这里你还可以再看看是否要向右移动，或者加其它逻辑

            // 4) 评分：如果 row 比之前记录的 bestRowSoFar 更大，则替换
            if (row > bestRowSoFar) {
                bestRowSoFar = row;
                bestMove[0] = row;
                bestMove[1] = shiftedCol;

                // 生成一条大致可行的移动序列
                bestMovement = computeMovement(row, shiftedCol);
            }
        }

        // 把结果封装回来
        return new DestinationResult(bestMove, bestMovement);
    }

    private void printMove(int[][] currentMatrix) {
        for (int i = 0; i<currentMatrix.length; i++) {
            for (int j = 0; j < currentMatrix[0].length; j++)
                System.out.print(currentMatrix[i][j] + " ");
            System.out.println();
        }
    }

    private int findLowestRow(int[][] shape, int colOffset) {
        int shapeRows = shape.length;
        int totalRows = currentGrid.length;

        int bestRow = -1;
        // rowOffset 的最大值不能超过 totalRows - shapeRows
        for (int rowOffset = 0; rowOffset <= totalRows - shapeRows; rowOffset++) {
            if (isValidPlacement(shape, rowOffset, colOffset)) {
                bestRow = rowOffset;
                // 先记录下这个 rowOffset。因为要找“最底”，我们不马上 break
                // 如果想要“第一个可放置点”就可以 break；这里要找“最后一个可放置点”
            }
        }
        return bestRow;
    }

    private int shiftLeftIfPossible(int[][] shape, int rowOffset, int startCol) {
        int finalCol = startCol;
        // 不断地尝试往左移一格
        while (finalCol > 0) {
            // 检测左移一格后是否可行
            if (isValidPlacement(shape, rowOffset, finalCol - 1)) {
                finalCol--;
            } else {
                // 不能再左移了
                break;
            }
        }
        return finalCol;
    }

    // x , y 表示 shape 左上角坐标
    private boolean isValidPlacement(int[][] shape, int rowOffset, int colOffset) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 1) {
                    int gridRow = rowOffset + r;
                    int gridCol = colOffset + c;

                    // 越界
                    if (gridRow < 0 || gridRow >= currentGrid.length
                            || gridCol < 0 || gridCol >= currentGrid[0].length) {
                        return false;
                    }
                    // 重叠
                    if (currentGrid[gridRow][gridCol] == 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private List<Integer> computeMovement(int targetRow, int targetCol) {
        List<Integer> moves = new ArrayList<>();

        // 当前的 row/col 必须和 tetromino.getY()/getX() 保持一致(行=Y,列=X)
        int currentRow = currentTetromino.getY();
        int currentCol = currentTetromino.getX();

        // 水平移动到 targetCol
        while (currentCol < targetCol) {
            moves.add(+1); // 表示向右
            currentCol++;
        }
        while (currentCol > targetCol) {
            moves.add(-1); // 表示向左
            currentCol--;
        }

        // 垂直移动到 targetRow
        while (currentRow < targetRow) {
            moves.add(0);  // 表示向下
            currentRow++;
        }

        return moves;
    }

    private int[][] rotate(int[][] matrix, int times) {
        int[][] result = matrix;
        for (int i = 0; i < times; i++) {
            result = rotateOnce(result);
        }
        return result;
    }

    private int[][] rotateOnce(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] rotated = new int[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - i - 1] = matrix[i][j];
            }
        }
        return rotated;
    }

    private int[][] copyMatrix(int[][] source) {
        int[][] copy = new int[source.length][source[0].length];
        for (int i = 0; i < source.length; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, source[i].length);
        }
        return copy;
    }

}
