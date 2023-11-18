package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        createDirectory("/home");
        createDirectory("/dev");
        createDirectory("/test");

        try {
            Path dirToWatch = Paths.get("/home");
            Path devDir = Paths.get("/dev");
            Path testDir = Paths.get("/test");

            Integer movedToTestCount = 0;
            Integer movedToDevCount = 0;

            String countFilePath = dirToWatch + "/count.txt";


            WatchService watchService = FileSystems.getDefault().newWatchService();
            dirToWatch.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            FileWriter fileWriter = new FileWriter(countFilePath, true);
            RandomAccessFile countFile = new RandomAccessFile(countFilePath, "rw");

            while(true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path filePath = dirToWatch.resolve((Path) event.context());
                        File file = new File(filePath.toAbsolutePath().toUri());

                        String fileName = file.getName();
                        String fileExtension;
                        int lastIndex = fileName.lastIndexOf(".");

                        if (lastIndex > 0) {
                            fileExtension = fileName.substring(lastIndex + 1);
                            if (fileExtension.equals("xml")) {
                                moveFile(filePath, devDir);
                                movedToDevCount++;
                            }
                            else if (fileExtension.equals("jar")) {
                                Integer creationHour = getFileCreationHour(filePath);

                                if (creationHour % 2 == 0) {
                                    moveFile(filePath, devDir);
                                    movedToDevCount++;
                                }
                                else {
                                    moveFile(filePath, testDir);
                                    movedToTestCount++;
                                }
                            }

                        }
                        else {
                            System.out.println("Extension not found!");
                        }

                        countFile.write(("Number of files moved to /test: " + movedToTestCount + "\n").getBytes());
                        countFile.write(("Number of files moved to /dev: " + movedToDevCount + "\n").getBytes());
                        countFile.write(("Total number of moved files " + (movedToTestCount + movedToDevCount) + "\n").getBytes());
                        countFile.seek(0);
                    }
                }
                key.reset();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Integer getFileCreationHour(Path filePath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        long creationTimeMillis = attrs.creationTime().toMillis();
        Date creationDate = new Date(creationTimeMillis);
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        return Integer.parseInt(hourFormat.format(creationDate));
    }

    private static void moveFile(Path filePath, Path destinationPath) throws IOException {
        Path destDir = destinationPath.resolve(filePath.getFileName());
        Files.move(filePath, destDir, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Moved " + filePath + " to " + destDir);
    }

    private static void createDirectory(String pathToCreate) {
        Path path = Path.of(pathToCreate);

        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                System.out.println("Directory created successfully.");
            } catch (IOException e) {
                System.err.println("Failed to create the directory: " + e.getMessage());
            }
        } else {
            System.out.println("Directory already exists.");
        }
    }
}