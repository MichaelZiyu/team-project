package file;

import game.World;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import use_case.browse_saves.SaveCatalog;
import use_case.save.SaveId;
import use_case.save.SaveSummary;

/**
 * Filesystem gateway that implements the application-owned {@code SaveCatalog}.
 * It maps local .dat files to {@code SaveSummary} values and translates
 * listing errors into catalog read failures. The existing Load action also
 * uses this adapter, which delegates world deserialization to {@code FileManager}.
 */

public final class LocalSaveStorage implements SaveCatalog {
    private final Path saveDirectory;

    public LocalSaveStorage(Path saveDirectory) {
        this.saveDirectory = saveDirectory.toAbsolutePath().normalize();
    }

    @Override
    public List<SaveSummary> listSaves() throws ReadFailure {
        try (Stream<Path> files = Files.list(saveDirectory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FileManager.SAVE_EXTENSION)
                            && name.length() > FileManager.SAVE_EXTENSION.length())
                    .map(name -> name.substring(0, name.length() - FileManager.SAVE_EXTENSION.length()))
                    .sorted()
                    .map(name -> new SaveSummary(new SaveId(name), name))
                    .toList();
        } catch (NoSuchFileException exception) {
            return List.of();
        } catch (IOException | UncheckedIOException exception) {
            throw new ReadFailure(exception);
        }
    }

    /** Reads the selected save for the existing Load action. */
    public World loadSave(SaveId id) throws IOException {
        Path savePath = saveDirectory.resolve(id.value() + FileManager.SAVE_EXTENSION).normalize();
        if (!saveDirectory.equals(savePath.getParent())) {
            throw new IOException("Invalid save ID");
        }
        return FileManager.readSavedWorld(Files.newInputStream(savePath));
    }
}
