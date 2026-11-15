package interface_adapter.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface-adapter view model that publishes menu navigation requests.
 * The presenter supplies a {@code ScreenId}, and registered Java callbacks
 * receive the requested destination. The Swing navigation menu handles
 * the actual panel change.
 */

public final class NavigationViewModel {
    private final List<Consumer<ScreenId>> screenListeners = new ArrayList<>();
    private ScreenId requestedScreen = ScreenId.TITLE;

    public void addScreenListener(Consumer<ScreenId> listener) {
        screenListeners.add(listener);
        listener.accept(requestedScreen);
    }

    public void requestScreen(ScreenId screen) {
        requestedScreen = screen;
        for (Consumer<ScreenId> listener : screenListeners) {
            listener.accept(screen);
        }
    }
}
