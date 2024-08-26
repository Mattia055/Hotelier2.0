package lib.etc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TempFileUtil {

    public static Path createTempSameDir(String path, String prefix, String suffix) throws IOException,IllegalArgumentException {
        return Files.createTempFile(new File(path).getParentFile().toPath(), prefix, suffix);
    }

    public static void AtomicMove(Path tempFile, String targetPath) throws IOException {
        File target = new File(targetPath);
        Files.move(tempFile, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static void deleteTempFileIfExists(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File TempDirSamePath(String file,String child) {
        File targetDir = new File(file).getParentFile();
        File tempDir = new File(targetDir, child);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return tempDir;
    }

    public static File TempDirOpen(String file_path,String child) {
        File targetDir = new File(file_path).getParentFile();
        File tempDir = new File(targetDir,child); 

        if(tempDir.exists() && tempDir.isDirectory())
            return tempDir;
        return null;
    }

    public static void deleteDirectoryRecursively(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectoryRecursively(file);
            }
        }
        Files.delete(directory.toPath());
    }
}
