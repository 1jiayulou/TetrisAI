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
        int[] bestMove = {currentGrid[0].length, -1};
        int bottom = 0;
        int[][] bestShape = currentTetromino.getShapeMatrix();
        for (int i = 0; i < 4; i++) {
            int[][] rotatedShape = rotate(currentTetromino.getShapeMatrix(), i);
            int[] move = getBestDestination(rotatedShape);
            currentTetromino.setShapeMatrix(rotatedShape);

            if (move[1] >= bestMove[1] && move[1]+rotatedShape.length >= bottom) {
                bestMove = move;
                bestShape = rotatedShape;
            }
        }
        currentTetromino.setShapeMatrix(bestShape);
        currentTetromino.setPosition(bestMove[0], bestMove[1]);
    }


    private int[] getBestDestination(int[][] shape) {
        int rows = currentGrid.length;
        int cols = currentGrid[0].length;
        int height = shape.length;
        int width = shape[0].length;

        for (int i = rows - height; i >= 0; i--) {
            for (int j = 0; j <= cols - width; j++) {
                if (isValidPlacement(shape, i, j)) {
                    return new int[]{j, i};
                }
            }
        }
        return new int[]{0, 4};
    }

    private void printMove(int[][] currentMatrix) {
        for (int i = 0; i<currentMatrix.length; i++) {
            for (int j = 0; j < currentMatrix[0].length; j++)
                System.out.print(currentMatrix[i][j] + " ");
            System.out.println();
        }
    }

    private boolean isValidPlacement(int[][] shape, int row, int col) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] == 1 && currentGrid[row + i][col + j] == 1) {
                    return false;
                }
            }
        }
        return true;
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

}
