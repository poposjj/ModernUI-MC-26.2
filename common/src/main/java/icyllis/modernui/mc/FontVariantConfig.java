package icyllis.modernui.mc;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps each edition's client preferences independent and leaves the legacy file intact. */
public final class FontVariantConfig {
    private static boolean importedLegacy;

    private FontVariantConfig() {}

    public static synchronized Path prepare(Path directory) {
        Path target = directory.resolve(FontVariant.CLIENT_CONFIG_NAME);
        Path legacy = directory.resolve("client.toml");
        try {
            Files.createDirectories(directory);
            if (!Files.exists(target) && Files.isRegularFile(legacy)) {
                try {
                    Files.copy(legacy, target);
                    importedLegacy = true;
                } catch (FileAlreadyExistsException ignored) {
                    // Another client already created this edition's file. Never overwrite it.
                }
            }
        } catch (IOException e) {
            // A read-only, full or locked config directory must never stop the game from
            // starting: keep the in-memory defaults and let save() report the failure.
            ModernUIMod.LOGGER.warn(ModernUIMod.MARKER,
                    "Cannot prepare Modern UI {} config at {}, in-memory defaults will be used",
                    FontVariant.NAME, target, e);
        }
        return target;
    }

    public static synchronized boolean consumeLegacyImport() {
        boolean result = importedLegacy;
        importedLegacy = false;
        return result;
    }
}
