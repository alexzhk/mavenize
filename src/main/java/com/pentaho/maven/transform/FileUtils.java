package com.pentaho.maven.transform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class FileUtils {

    public static void copyFolder(Path moduleFolder, String from, Path where, String to) throws IOException {
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        String firstTestPackageFolder = fromRoot.getFileName().toString();
        Path mavenTestSourceFolder = Paths.get(where.toString(), to, firstTestPackageFolder);
        System.out.println("moving " + fromRoot + " to " + mavenTestSourceFolder);
        try {
            copyFile(fromRoot, mavenTestSourceFolder);
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        }

    }

    public static void copyAllInsideFolder(Path moduleFolder, String from, Path mavenFileLocation) throws IOException {
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        if (Files.exists(fromRoot)) {
            Stream<Path> list = Files.list(fromRoot);
            list.forEach(path -> {
                Path antFileLocation = Paths.get(moduleFolder.toString(), from, /*firstTestPackageFolder, */path.getFileName().toString());
                //Path mavenFileLocation = Paths.get(moduleFolder.toString(), to, /*firstTestPackageFolder, */path.getFileName().toString());
                System.out.println("moving " + antFileLocation + " to " + mavenFileLocation);
                try {
                    copyFile(antFileLocation, Paths.get(mavenFileLocation.toString(), antFileLocation.getFileName().toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public static void copyFile(Path antTestSourceFolder, Path mavenTestSourceFolder) throws IOException {
        try {
            FileUtils.copy(antTestSourceFolder, mavenTestSourceFolder);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
    }

    public static void moveFileReplace(Path antTestSourceFolder, Path mavenTestSourceFolder) throws IOException {
        try {
            FileUtils.copy(antTestSourceFolder, mavenTestSourceFolder);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
    }

    public static void copy(Path sourcePath, Path targetPath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                                                     final BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(targetPath.resolve(sourcePath
                        .relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file,
                                             final BasicFileAttributes attrs) throws IOException {
                Files.copy(file,
                        targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyFileReplace(Path antSourceFolder, Path mavenDestinationFolder) throws IOException {

        FileUtils.copy(antSourceFolder, mavenDestinationFolder);

    }

    static public void removeFile(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (FileSystemException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createFolder(Path folder) throws IOException {

        if (!Files.exists(folder)) {
            Files.createDirectory(folder);
        }
    }

    public static void createFolders(Path folders) throws IOException {

        Files.createDirectories(folders);
    }

    public static void deleteFile(Path file) throws IOException {

        Files.deleteIfExists(file);

    }

    public static void deleteFolderWithFiles(Path directory) throws IOException {

        if (Files.exists(directory)) {

            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }

    }

    public static void createFile(Path filePath) throws IOException {
        Files.createFile(filePath);
    }

    public static void writeTextToFile(Path filePath, String text) throws FileNotFoundException {

        String fileName = Paths.get(filePath.toString()).toString();

        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(text);
        }

    }


}
