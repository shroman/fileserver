package org.example.files.client;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads from command line and sends requests.
 */
public class CommandLine {
    private final FileClient client;

    CommandLine(String host, int port, String fileDir) {
        this.client = new FileClient(host, port, fileDir);
    }

    public void start() {
        this.client.run();
    }

    public void message(String msg) throws Exception {
        this.client.writeMessage(msg);
    }

    public void shutdown() {
        client.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        // disable logging when cmd.
        Logger root = Logger.getLogger("");
        root.setLevel(Level.SEVERE);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.SEVERE);
        }

        Options options = new Options();

        Option host = new Option("h", "host", true, "server host address");
        host.setRequired(true);
        options.addOption(host);

        Option port = new Option("p", "port", true, "server port");
        port.setRequired(true);
        options.addOption(port);

        Option fileDir = new Option("d", "dir", true, "file directory to save files");
        fileDir.setRequired(true);
        options.addOption(fileDir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        org.apache.commons.cli.CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("CommandLine", options);

            System.exit(1);
            return;
        }

        CommandLine cl = new CommandLine(
                cmd.getOptionValue("host"),
                Integer.parseInt(cmd.getOptionValue("port")),
                cmd.getOptionValue("dir"));

        CompletableFuture.runAsync(
                () -> {
                    try {
                        cl.start();
                    } catch (Exception e) {
                        System.err.println("Leaving because of client exception");
                        cl.shutdown();
                    }
                }).whenComplete((res, ex) -> cl.shutdown());

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

        System.out.println("*** Available commands: ***");
        System.out.println("- index");
        System.out.println("- get [file_name]");
        System.out.println("- quit/q");

        Scanner scanner = new Scanner(System.in);
        System.out.print(">> ");

        while (!Thread.currentThread().isInterrupted()) {
            String input = scanner.nextLine();

            try {
                cl.message(input);
            } catch (Exception e) {
                e.printStackTrace();
                cl.shutdown();
            }

//            if (input.trim().equalsIgnoreCase("quit") || input.trim().equalsIgnoreCase("q")) {
//                Thread.currentThread().sleep(1000);
//                cl.shutdown();
//            }
        }
    }
}
