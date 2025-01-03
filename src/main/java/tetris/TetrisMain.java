package tetris;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TetrisMain extends Application {

    private Stage primaryStage;
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int BLOCK_SIZE = 30;
    private InputHandler inputHandler;
    private Tetromino currentTetromino;
    private GraphicsContext gc;
    private TetrisGrid grid;
    private int score = 0;
    private Text scoreText;
    private boolean isGameOver;
    private Timeline timeline;

    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);
        gc = canvas.getGraphicsContext2D();
        drawGrid(gc);

        inputHandler = new InputHandler();

        scoreText = new Text("Score: 0");
        scoreText.setStyle("-fx-font: 20 arial;");
        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setTop(scoreText);
        BorderPane.setAlignment(scoreText, Pos.CENTER);


        Scene scene = new Scene(root, COLS * BLOCK_SIZE + 20, ROWS * BLOCK_SIZE + 100);
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(event -> inputHandler.handleKeyReleased(event));

        this.primaryStage = primaryStage;
        primaryStage.setTitle("Tetris Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        currentTetromino = new Tetromino(ShapeType.T);// 初始显示 T 形状
        drawTetromino(gc, currentTetromino);

        timeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> autoDrop()));
        timeline.setCycleCount(Timeline.INDEFINITE); // 无限循环
        timeline.play();

        grid = new TetrisGrid(ROWS, COLS);
    }

    private void drawTetromino(GraphicsContext gc, Tetromino tetromino, int x, int y) {
        int[][] shape = tetromino.getShapeMatrix();
        Color color = tetromino.getColor();

        gc.setFill(color);
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    gc.fillRect((x + col) * BLOCK_SIZE, (y + row) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect((x + col) * BLOCK_SIZE, (y + row) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.GRAY);
        for (int x = 0; x <= COLS * BLOCK_SIZE; x += BLOCK_SIZE) {
            gc.strokeLine(x, 0, x, ROWS * BLOCK_SIZE);
        }
        for (int y = 0; y <= ROWS * BLOCK_SIZE; y += BLOCK_SIZE) {
            gc.strokeLine(0, y, COLS * BLOCK_SIZE, y);
        }
    }

    private void drawTetromino(GraphicsContext gc, Tetromino tetromino) {
        int[][] shape = tetromino.getShapeMatrix();
        Color color = tetromino.getColor();
        int x = tetromino.getX();
        int y = tetromino.getY();

        gc.setFill(color);
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) { // 绘制方块
                    gc.fillRect((x + col) * BLOCK_SIZE, (y + row) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect((x + col) * BLOCK_SIZE, (y + row) * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
    }

    private void handleKeyPress(KeyEvent event) {
        // 如果游戏结束，不再处理按键事件
        if (isGameOver) {
            return;
        }

        // 处理输入按键
        inputHandler.handleKeyPressed(event);

        // 获取当前方块的位置
        int newX = currentTetromino.getX();
        int newY = currentTetromino.getY();

        // 根据按键执行操作
        if (inputHandler.isMoveLeft()) {
            newX--; // 左移
        } else if (inputHandler.isMoveRight()) {
            newX++; // 右移
        } else if (inputHandler.isDrop()) {
            newY++; // 向下移动
        } else if (inputHandler.isRotate()) {
            // 尝试旋转方块
            currentTetromino.rotate();
            // 如果旋转无效，撤销旋转
            if (!grid.isValidMove(currentTetromino, currentTetromino.getX(), currentTetromino.getY())) {
                currentTetromino.rotate();
                currentTetromino.rotate();
                currentTetromino.rotate(); // 回到原始方向
            }
        }

        // 检查移动是否有效
        if (grid.isValidMove(currentTetromino, newX, newY)) {
            currentTetromino.setPosition(newX, newY); // 更新方块位置
        } else if (inputHandler.isDrop()) {
            // 如果向下移动无效，锁定方块
            grid.lockTetromino(currentTetromino);

            // 检查并消除满行
            int clearedLines = grid.clearFullLines();
            if (clearedLines > 0) {
                score += clearedLines * 100; // 每行100分
                scoreText.setText("Score: " + score); // 更新分数显示
            }

            // 生成新的方块
            currentTetromino = new Tetromino(ShapeType.values()[(int) (Math.random() * 7)]);

            // 检查游戏结束条件
            if (!grid.isValidMove(currentTetromino, currentTetromino.getX(), currentTetromino.getY())) {
                System.out.println("Game Over!");
                timeline.stop();
                isGameOver = true;
                // primaryStage.close(); // 关闭窗口
                scoreText.setText("Game Over! Final Score: " + score);
            }
        }

        // 更新画面
        gc.clearRect(0, 0, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE); // 清空画布
        drawGrid(gc); // 重新绘制网格
        drawLockedBlocks();
        drawTetromino(gc, currentTetromino); // 绘制当前方块
    }

    private void autoDrop() {
        if (isGameOver) {
            return;
        }

        int newY = currentTetromino.getY() + 1;
        if (grid.isValidMove(currentTetromino, currentTetromino.getX(), newY)) {
            currentTetromino.moveDown();
        } else {
            lockAndHandleNewTetromino();
        }

        // 更新画面
        gc.clearRect(0, 0, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);
        drawGrid(gc);
        drawLockedBlocks();
        drawTetromino(gc, currentTetromino);
    }

    private void lockAndHandleNewTetromino() {
        grid.lockTetromino(currentTetromino);

        // 检查并消除满行
        int clearedLines = grid.clearFullLines();
        if (clearedLines > 0) {
            score += clearedLines * 100; // 每行100分
            scoreText.setText("Score: " + score); // 更新分数显示
        }

        // 生成新的方块
        currentTetromino = new Tetromino(ShapeType.values()[(int) (Math.random() * 7)]);

        // 检查游戏结束条件
        if (!grid.isValidMove(currentTetromino, currentTetromino.getX(), currentTetromino.getY())) {
            System.out.println("Game Over!");
            timeline.stop();
            isGameOver = true;
            // primaryStage.close(); // 关闭窗口
            scoreText.setText("Game Over! Final Score: " + score);
        }
    }

    private void drawLockedBlocks() {
        int[][] gridArray = grid.getGrid();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (gridArray[row][col] == 1) {
                    gc.setFill(Color.GRAY);
                    gc.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
