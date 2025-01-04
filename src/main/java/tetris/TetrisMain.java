package tetris;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TetrisMain extends Application {

    // 网格大小
    public static final int COLS = 10;
    public static final int ROWS = 20;
    public static final int BLOCK_SIZE = 30;

    // 画布与图形上下文
    private Canvas canvas;
    private static GraphicsContext gc;

    // 游戏元素
    static TetrisGrid grid;
    public static Tetromino currentTetromino;

    // 输入处理
    private InputHandler inputHandler;
    private boolean isPaused = false; // 新增：记录游戏是否处于暂停状态

    // 分数与计分显示
    private int score = 0;
    private Label scoreLabel;

    // 计时器
    private boolean isGameOver = false;
    private int elapsedTime = 0;
    private Label timerLabel;
    private Timeline dropTimeline;   // 控制方块自动下落
    private Timeline timerTimeline;  // 控制游戏时间

    // AI 线程
    private Thread aiThread;
    private AIControllerV1 aiControllerV1;

    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化画布
        canvas = new Canvas(COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);
        gc = canvas.getGraphicsContext2D();

        // 2. 初始化顶部布局（分数 + 时间）
        scoreLabel = createLabel("Score: 0");
        timerLabel = createLabel("Time: 0s");
        HBox topBox = new HBox(40, scoreLabel, timerLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #333333;");

        // 3. 布局根节点
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(canvas);

        Scene scene = new Scene(root, COLS * BLOCK_SIZE + 500, ROWS * BLOCK_SIZE + 200);
        // 绑定按键
        inputHandler = new InputHandler();
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(inputHandler::handleKeyReleased);

        // 4. 初始化舞台
        primaryStage.setTitle("Tetris Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 5. 创建网格和初始方块
        grid = new TetrisGrid(ROWS, COLS);
        currentTetromino = new Tetromino(ShapeType.T);

        // 6. 启动两个 Timeline
        // 6.1 方块每 0.5 秒自动下落
        dropTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> autoDrop()));
        dropTimeline.setCycleCount(Timeline.INDEFINITE);
        dropTimeline.play();

        // 6.2 游戏时间每秒加 1
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isGameOver) {
                elapsedTime++;
                timerLabel.setText("Time: " + elapsedTime + "s");
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();

        // 7. 启动 AI（可选）
        aiControllerV1 = new AIControllerV1();
        aiThread = new Thread(aiControllerV1);
        aiThread.start();

        // 8. 初次绘制
        updateDisplay();
    }

    // ======== 按键处理 ========
    public void handleKeyPress(KeyEvent event) {


        switch (event.getCode()) {
            case P:
                togglePause();
                return; // 这里 return 是为了防止暂停时还继续响应其他按键
            default:
                break;
        }
        if (isPaused || isGameOver) return;

        inputHandler.handleKeyPressed(event);
        int newX = currentTetromino.getX();
        int newY = currentTetromino.getY();

        // 根据输入决定新位置或旋转
        if (inputHandler.isMoveLeft()) {
            newX--;
        } else if (inputHandler.isMoveRight()) {
            newX++;
        } else if (inputHandler.isDrop()) {
            newY++;
        } else if (inputHandler.isRotate()) {
            currentTetromino.rotate();
            if (!grid.isValidMove(currentTetromino, currentTetromino.getX(), currentTetromino.getY())) {
                // 撤销旋转
                currentTetromino.rotate();
                currentTetromino.rotate();
                currentTetromino.rotate();
            }
        }

        // 检查是否能移动到 (newX, newY)
        if (grid.isValidMove(currentTetromino, newX, newY)) {
            currentTetromino.setPosition(newX, newY);
        } else if (inputHandler.isDrop()) {
            // 如果无法下落，锁定
            lockAndSpawnNew();
        }

        updateDisplay();
    }

    // ======== 自动下落 ========
    private void autoDrop() {
        if (isGameOver) return;

        int newY = currentTetromino.getY() + 1;
        if (grid.isValidMove(currentTetromino, currentTetromino.getX(), newY)) {
            currentTetromino.moveDown();
        } else {
            lockAndSpawnNew();
        }

        updateDisplay();
    }

    // ======== 锁定当前方块并生成新方块 ========
    private void lockAndSpawnNew() {
        grid.lockTetromino(currentTetromino);
        clearLines();
        currentTetromino = new Tetromino(ShapeType.values()[(int) (Math.random() * 7)]);
        // 检查是否游戏结束
        checkGameOver();
    }

    // ======== 检查是否游戏结束 ========
    private void checkGameOver() {
        if (!grid.isValidMove(currentTetromino, currentTetromino.getX(), currentTetromino.getY())) {
            endGame();
        }
    }

    // ======== 消除行并更新分数 ========
    private void clearLines() {
        int clearedLines = grid.clearFullLines();
        if (clearedLines > 0) {
            score += clearedLines * 100;
            scoreLabel.setText("Score: " + score);
        }
    }

    // ======== 结束游戏 ========
    private void endGame() {
        System.out.println("Game Over!");
        isGameOver = true;
        dropTimeline.stop();
        timerTimeline.stop();
        scoreLabel.setText("Game Over! Final Score: " + score);
        aiControllerV1.stop();
    }

    // ======== 更新画布显示 ========
    private void updateDisplay() {
        // 清空画布
        gc.clearRect(0, 0, COLS * BLOCK_SIZE, ROWS * BLOCK_SIZE);
        // 先画网格
        drawGrid(gc);
        // 再画已锁定的方块
        drawLockedBlocks();
        // 最后画当前方块
        drawTetromino(gc, currentTetromino);
    }

    // ======== 绘制网格 ========
    public static void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.DARKGRAY);
        for (int x = 0; x <= COLS * BLOCK_SIZE; x += BLOCK_SIZE) {
            gc.strokeLine(x, 0, x, ROWS * BLOCK_SIZE);
        }
        for (int y = 0; y <= ROWS * BLOCK_SIZE; y += BLOCK_SIZE) {
            gc.strokeLine(0, y, COLS * BLOCK_SIZE, y);
        }
    }

    // ======== 绘制锁定的方块 ========
    public static void drawLockedBlocks() {
        int[][] gridArray = grid.getGrid();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (gridArray[row][col] == 1) {
                    gc.setFill(Color.DARKGRAY);
                    gc.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
        }
    }

    // ======== 绘制当前下落方块 ========
    public static void drawTetromino(GraphicsContext gc, Tetromino tetromino) {
        int[][] shape = tetromino.getShapeMatrix();
        Color color = tetromino.getColor();
        int x = tetromino.getX();
        int y = tetromino.getY();

        gc.setFill(color);
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    gc.fillRect((x + col) * BLOCK_SIZE,
                            (y + row) * BLOCK_SIZE,
                            BLOCK_SIZE,
                            BLOCK_SIZE);
                    gc.setStroke(Color.BLACK);
                    gc.strokeRect((x + col) * BLOCK_SIZE,
                            (y + row) * BLOCK_SIZE,
                            BLOCK_SIZE,
                            BLOCK_SIZE);
                }
            }
        }
    }

    private void togglePause() {
        // 如果当前处于暂停状态，则恢复游戏
        if (isPaused) {
            isPaused = false;
            dropTimeline.play();
            timerTimeline.play();

            // 如果有 AI 线程，也恢复
            if (aiControllerV1 != null) {
                aiControllerV1.resumeAI();
            }

            // 如果需要，可以在界面上提醒玩家游戏已继续
            System.out.println("Game Resumed!");

        } else {
            // 如果当前处于正常状态，则暂停游戏
            isPaused = true;
            dropTimeline.stop();
            timerTimeline.stop();

            // 如果有 AI 线程，也暂停
            if (aiControllerV1 != null) {
                aiControllerV1.pauseAI();
            }

            // 如果需要，可以在界面上提醒玩家游戏已暂停
            System.out.println("Game Paused!");
        }
    }

    // ======== 简化创建 Label 的小方法 ========
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setTextFill(Color.WHITE);
        return label;
    }

    // ======== 对外暴露一些获取方法 (AI 或其他类需要) ========
    public static Tetromino getCurrentTetromino() {
        return currentTetromino;
    }

    public static int[][] getCurrentGrid() {
        return grid.getGrid();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
