package game;

import file.FileManager;

public class GameController {

    private static World world;
    private static FileManager file;

    public static void initializeGame(World world, FileManager file) {
        GameController.world = world;
        GameController.file = file;
    }

    public static void run() {

    }
}
