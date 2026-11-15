package interface_adapter.start_menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class StartMenuViewModel {
    private final List<Consumer<String>> messageListeners = new ArrayList<>();
    private String message = "";

    public void addMessageListener(Consumer<String> listener) {
        messageListeners.add(listener);
        listener.accept(message);
    }

    public void setMessage(String message) {
        this.message = message;
        for (Consumer<String> listener : messageListeners) {
            listener.accept(message);
        }
    }
}
