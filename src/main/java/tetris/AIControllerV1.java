package tetris;

import javafx.application.Platform;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class AIControllerV1 implements Runnable{
    private boolean running = true;
    private int currentId = -1;
    private Tetromino currentTetromino ;
    private int[][] currentGrid;
    private int highest;
    private boolean isRunning = true;
    private boolean isPaused = false;

    @Override
    public void run() {
        try {
            while (running) {
                Thread.sleep(100);
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
        int[][] temp = currentTetromino.getShapeMatrix();
        int rotateTime = 0;
        int[] pair = new int[]{currentGrid[0].length, -1};

        for (int i =0 ; i<4; i++) {
            int[] currentBest = getBestDestination(temp);
            if (TetrisMain.grid.isValidMove(currentTetromino, currentBest[0], currentBest[1]) && currentBest[1] >= pair[1]) {
                rotateTime = i;
                if (currentBest[1] == pair[1]) pair[0] = Math.min(pair[0], currentBest[0]);else
                {
                    pair[0] = currentBest[0];
                    pair[1] = currentBest[1];
                }
            }
            temp = rotate(temp);
        }

        if (rotateTime == 1) currentTetromino.rotate(); else if (rotateTime == 2) {
            currentTetromino.rotate();
            currentTetromino.rotate();
        } else {
            currentTetromino.rotate();
            currentTetromino.rotate();
            currentTetromino.rotate();
        }

        if (!TetrisMain.grid.isValidMove(currentTetromino, pair[0], pair[1])) {
            printMove(currentTetromino.getShapeMatrix());
            System.out.println("Something wrong" + rotateTime + " " + pair[0] + " " + pair[1]);
            printMove(currentGrid);
            currentTetromino.setPosition(4,0);
        } else currentTetromino.setPosition(pair[0],pair[1]);

    }

    private int[] getBestDestination(int[][] currentMatrix) {
        int height = currentMatrix.length;
        int width = currentMatrix[0].length;
        int rows = currentGrid.length;       // 网格的行数
        int cols = currentGrid[0].length;    // 网格的列数

        for (int i = rows - height; i >= 0; i--) { // 从底部向上遍历
            for (int j = 0; j <= cols - width; j++) { // 从左向右遍历
                boolean h = true;
                for (int x = 0; x < height; x++) {
                    for (int y = 0; y < width; y++) {
                        if ( (currentMatrix[x][y]==1) && (currentGrid[i + x][j + y]== 1)) { // 检查重叠
                            h = false;
                            break;
                        }
                    }
                    if (!h) break;
                }
                if (h) {
                    // TetrisMain.getCurrentTetromino().setPosition(j, i); // 设置方块位置
                    return new int[]{j,i};
                }
            }
        }
        return new int[]{0,4};
    }

    private void printMove(int[][] currentMatrix) {
        for (int i = 0; i<currentMatrix.length; i++) {
            for (int j = 0; j < currentMatrix[0].length; j++)
                System.out.print(currentMatrix[i][j] + " ");
            System.out.println();
        }
    }

    private int[][] rotate(int[][] matrix) {
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

}
