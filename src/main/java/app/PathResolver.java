package app;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class PathResolver {

    public static Path chooseDirectory() {
        CompletableFuture<File> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Vælg output-mappe");
            future.complete(chooser.showDialog(null));
        });
        File dir = future.join();
        return dir != null ? dir.toPath() : null;
    }

    public static Path resolveJavaRoot(Path selectedDir) throws IOException {

        // 1. Er vi allerede inde i src/main/java?
        String pathStr = selectedDir.toString().replace("\\", "/");
        if (pathStr.contains("src/main/java")) {
            return selectedDir;
        }

        // 2. Findes src/main/java under den valgte mappe?
        Path existingJavaRoot = selectedDir.resolve("src/main/java");
        if (Files.exists(existingJavaRoot)) {
            Path appDir = existingJavaRoot.resolve("app");
            Files.createDirectories(appDir);
            return appDir;
        }

        // 3. Ellers opret hele strukturen
        Path javaRoot = selectedDir.resolve("src/main/java/app");
        Files.createDirectories(javaRoot);
        return javaRoot;
    }

    public static String extractBasePackage(Path outputDir, String subPackage) {
        String pathStr = outputDir.toString().replace("\\", "/");
        int idx = pathStr.indexOf("src/main/java/");

        if (idx == -1) {
            return "package " + subPackage + ";\n\n";
        }

        String basePackage = pathStr.substring(idx + "src/main/java/".length())
                .replace("/", ".");
        return "package " + basePackage + "." + subPackage + ";\n\n";
    }
}
