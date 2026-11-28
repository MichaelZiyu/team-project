package game.interactor.inputData;

import java.util.Map;

public class LocalPlayerCommand {

    public enum MovementCommandType extends CommandType{
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
    }

    public enum OtherCommandType {

        // character interaction
        INTERACT,
        ATTACK,

        // invalid command
        NULL
    }

    private static final Map<String, CommandType> COMMAND_MAP = Map.ofEntries(
            Map.entry("+w", MovementCommandType.MOVE_FORWARD),
            Map.entry("-w", MovementCommandType.CANCEL_FORWARD),
            Map.entry("+s", MovementCommandType.MOVE_BACKWARD),
            Map.entry("-s", MovementCommandType.CANCEL_BACKWARD),
            Map.entry("+a", MovementCommandType.MOVE_LEFT),
            Map.entry("-a", MovementCommandType.CANCEL_LEFT),
            Map.entry("+d", MovementCommandType.MOVE_RIGHT),
            Map.entry("-d", MovementCommandType.CANCEL_RIGHT),
            Map.entry("+shift", MovementCommandType.RUN),
            Map.entry("-shift", MovementCommandType.CANCEL_RUN),
            Map.entry("+e", CommandType.INTERACT),
            Map.entry("+space", CommandType.ATTACK)
    );

    public static CommandType getCommand(String command) {
        CommandType playerCommand = COMMAND_MAP.get(command);
        if (playerCommand != null) return playerCommand;
        return CommandType.NULL;
    }
}
