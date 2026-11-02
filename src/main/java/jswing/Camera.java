package jswing;

public class Camera {

    int x, y;

    void transform(
            int playerX, int playerY,
            int objectX, int objectY,
            int width, int height
    ) {
        x = objectX - playerX + WindowManager.FRAME_WIDTH / 2 - width / 2;
        y = WindowManager.FRAME_HEIGHT / 2 - (objectY - playerY) - height;
    }
}
