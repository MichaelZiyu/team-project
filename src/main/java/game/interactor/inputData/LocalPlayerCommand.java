package game.interactor.inputData;

import java.util.Map;

public class LocalPlayerCommand {
    public enum CommandType {

        // character movements
        MOVE_FORWARD,
        CANCEL_FORWARD,
        MOVE_BACKWARD,
        CANCEL_BACKWARD,
        MOVE_LEFT,
        CANCEL_LEFT,
        MOVE_RIGHT,
        CANCEL_RIGHT,
        RUN,
        CANCEL_RUN,

        // character interaction
        INTERACT,
        ATTACK,

        // invalid command
        NULL
    }

    private static final Map<String, CommandType> COMMAND_MAP = Map.ofEntries(
            Map.entry("+w", CommandType.MOVE_FORWARD),
            Map.entry("-w", CommandType.CANCEL_FORWARD),
            Map.entry("+s", CommandType.MOVE_BACKWARD),
            Map.entry("-s", CommandType.CANCEL_BACKWARD),
            Map.entry("+a", CommandType.MOVE_LEFT),
            Map.entry("-a", CommandType.CANCEL_LEFT),
            Map.entry("+d", CommandType.MOVE_RIGHT),
            Map.entry("-d", CommandType.CANCEL_RIGHT),
            Map.entry("+shift", CommandType.RUN),
            Map.entry("-shift", CommandType.CANCEL_RUN),
            Map.entry("+e", CommandType.INTERACT),
            Map.entry("+space", CommandType.ATTACK)
    );

    private CommandType command;

    public LocalPlayerCommand(String command) {
        CommandType playerCommand = COMMAND_MAP.get(command);
        if (playerCommand != null) this.command = playerCommand;
    }

    public CommandType getCommand() {
        return command;
    }
}
