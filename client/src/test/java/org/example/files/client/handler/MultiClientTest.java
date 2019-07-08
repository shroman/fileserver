package org.example.files.client.handler;

import org.example.files.client.FileClient;
import org.example.files.server.FileServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Accessing a server with multiple clients.
 */
public class MultiClientTest {
    private static FileServer fileServer = new FileServer();

    @BeforeAll
    public static void setup() throws Exception {
        CompletableFuture.runAsync(
                () -> {
                    try {
                        fileServer.start(new String[]{System.getProperty("user.dir")});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("End");
        fileServer.stop();
    }

    @Test
    public void oneClient() throws Exception {
        FileClient fileClient = new FileClient("localhost", 49999, "/tmp");
        try {
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            fileClient.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            TimeUnit.SECONDS.sleep(1);
            for (int i = 0; i < 10; i++) {
                fileClient.writeMessage("index");
                TimeUnit.MILLISECONDS.sleep(10);
            }

            for (int i = 0; i < 10; i++) {
                fileClient.writeMessage("a");
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } finally {
            fileClient.stop();
        }
    }

    @Test
    public void multiClient() throws Exception {
        TimeUnit.SECONDS.sleep(1);
        runClients();
    }

    /**
     * Runs as many clients as files. Each client downloads a random file from the list.
     */
    private void runClients() throws Exception {
        List<Path> files = Files.walk(Paths.get(System.getProperty("user.dir") + "/../files"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(files.size());
        List<FileClient> clients = new LinkedList<>();

        try {
            for (Path file : files) {
                System.out.println(file.toString());

                FileClient c = new FileClient("localhost", 49999, "/tmp");
                clients.add(c);
                executor.submit(c);
            }
        } finally {

        }

        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 50; i++) {
            Path file = files.get(ThreadLocalRandom.current().nextInt(files.size()));
            FileClient c = clients.get(ThreadLocalRandom.current().nextInt(files.size()));
            c.writeMessage("get " + file.toString());
        }

        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        // check file integrity.
        for (Path file : files) {
            Path fName = file.getFileName();

            assertEquals(Files.size(file), Files.size(Paths.get("/tmp/" + fName)));
        }
    }
}
