package tetris;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class InputHandler {

    private boolean moveLeft;
    private boolean moveRight;
    private boolean rotate;
    private boolean drop;

    public InputHandler() {
        reset();
    }

    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            moveLeft = true;  // left
        } else if (event.getCode() == KeyCode.RIGHT) {
            moveRight = true; // right
        } else if (event.getCode() == KeyCode.UP) {
            rotate = true;    // rotate
        } else if (event.getCode() == KeyCode.DOWN) {
            drop = true;      // drop
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            moveLeft = false;
        } else if (event.getCode() == KeyCode.RIGHT) {
            moveRight = false;
        } else if (event.getCode() == KeyCode.UP) {
            rotate = false;
        } else if (event.getCode() == KeyCode.DOWN) {
            drop = false;
        }
    }

    public boolean isMoveLeft() {
        return moveLeft;
    }

    public boolean isMoveRight() {
        return moveRight;
    }

    public boolean isRotate() {
        return rotate;
    }

    public boolean isDrop() {
        return drop;
    }

    // 重置按键状态
    public void reset() {
        moveLeft = false;
        moveRight = false;
        rotate = false;
        drop = false;
    }

}
