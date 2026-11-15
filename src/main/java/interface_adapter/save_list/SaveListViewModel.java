package interface_adapter.save_list;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SaveListViewModel {
    private final List<Consumer<List<Option>>> optionListeners = new ArrayList<>();
    private List<Option> options = List.of();

    public void addOptionsListener(Consumer<List<Option>> listener) {
        optionListeners.add(listener);
        listener.accept(options);
    }

    public void setOptions(List<Option> options) {
        this.options = List.copyOf(options);
        for (Consumer<List<Option>> listener : optionListeners) {
            listener.accept(this.options);
        }
    }

    public record Option(String selectionKey, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
