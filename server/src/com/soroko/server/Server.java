package com.soroko.server;

import com.soroko.common.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


public class Server {
    public static final String SERVER_STORAGE_LOCATION =
            "C:\\Users\\yuriy\\IdeaProjects\\socketLesson\\server\\src\\com\\soroko\\server\\";
    private final int port;
    private final ArrayBlockingQueue<Message> messages =
            new ArrayBlockingQueue<>(1000, true);
    private final List<SendReceive> connectionHandlers = new CopyOnWriteArrayList<>();
    Sender sender;
    ThreadForClient threadForClient;
    SendReceive connectionHandler;
    private long fileSize;
    private int amountOfSymbols;
    private int randomName;
    ArrayList<File> files = new ArrayList<>();

    public Server(int port) {
        this.port = port;
        fileSize = 10_000_000_000L;
        amountOfSymbols = 200;
    }

    public int countSymbol(File file) {
        BufferedReader reader;
        int count = 0;
        try {
            reader = new BufferedReader(new FileReader(file));
            while (reader.ready()) {
                for (char symbol : reader.readLine().toCharArray()) {
                    count++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return count;
    }

    public synchronized void showFiles() {
        Message message = new Message("server");
        String intro = "Список доступных файлов:" + "\n";
        String fileNames = files.stream().map(File::getName).collect(Collectors.joining(", "));
        if (files.isEmpty()) message.setText("Доступных файлов не обнаружено");
        else message.setText(intro + fileNames);
        try {
            connectionHandler.send(message);
        } catch (IOException e) {
            connectionHandler.close();
        }
    }

    public synchronized void loadFile(String pathname) {
        randomName = (int) (Math.random() * 1000);
        File fileSource = new File(pathname);
        String fileName = SERVER_STORAGE_LOCATION + fileSource.getName();
        String fileWasCreated;
        File fileDestination;
        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            fileDestination =
                    new File((SERVER_STORAGE_LOCATION + randomName + fileSource.getName()));
        } else {
            fileDestination = new File(fileName);
        }
        if (!fileSource.isDirectory() && fileSource.exists()) {
            long bytes = fileSource.length();
            int symbols = countSymbol(fileSource);

            if (bytes <= fileSize && symbols <= amountOfSymbols) {
                try {
                    copy(fileSource, fileDestination);
                    if (fileDestination.isFile())
                        files.add(fileDestination);
                        fileWasCreated = "Файл " + fileDestination.getName() + " был успешно загружен";
                    Message fileWasCreatedMessage = new Message("server");
                    fileWasCreatedMessage.setText(fileWasCreated);
                    try {
                        messages.put(fileWasCreatedMessage);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            String fileDoesNotExist = "Файл по указанному пути не найден" +
                    " или содержит слишком большой объем информации";
            Message fileDoesNotExistMsg = new Message("server");
            fileDoesNotExistMsg.setText(fileDoesNotExist);
            try {
                connectionHandler.send(fileDoesNotExistMsg);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

    }

     synchronized void saveFile(String fileName) {
        for (File file : files) {
            if (fileName.equals(file.getName())) {
            }
        }
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            sender = new Sender();
            sender.start();
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    // SendReceive connectionHandler = new SendReceive(socket);
                    connectionHandler = new SendReceive(socket);
                    connectionHandlers.add(connectionHandler);
                    threadForClient = new ThreadForClient(connectionHandler);
                    threadForClient.start();
                } catch (Exception e) {
                    System.out.println("Проблема с установкой нового соединения");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера");
            throw new RuntimeException(e);
        }
    }

    public static void copy(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath());
    }

    private class ThreadForClient extends Thread {
        private final SendReceive connectionHandler;
        Message fromClient;

        public ThreadForClient(SendReceive connectionHandler) {
            this.connectionHandler = connectionHandler;
        }

        public SendReceive getConnectionHandler() {
            return connectionHandler;
        }

        public Message getFromClient() {
            return fromClient;
        }

        @Override
        public void run() {
            while (true) {
                fromClient = null;
                try {
                    fromClient = connectionHandler.receive();
                } catch (IOException e) {
                    connectionHandlers.remove(connectionHandler);
                    return;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if (!fromClient.getText().equals("/files") && !fromClient.getText().equals("/loadfile")
                        && !fromClient.getText().equals("/savefile")) {
                    Message message = new Message("server: " + fromClient.getSender());
                    message.setText(fromClient.getSentAt() + " "
                            + fromClient.getSender() + ": " + fromClient.getText());
                    try {
                        messages.put(message);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } else if (fromClient.getText().equals("/files")) {
                    showFiles();
                } else if (fromClient.getText().equals("/loadfile")) {
                    Message pathMessage;
                  //  FileMessage fileMessage;
                    try {
                        pathMessage = connectionHandler.receive();
                       // fileMessage = connectionHandler.receiveFileDescription();
                    } catch (IOException e) {
                        connectionHandlers.remove(connectionHandler);
                        return;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    loadFile(pathMessage.getText());
                } else if (fromClient.getText().equals("/savefile")) {
                    saveFile("fileName");
                }
            }
        }
    }

    private class Sender extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) { // true
                try {
                    Message message = messages.take();
                    for (SendReceive handler : connectionHandlers) {
                        try {
                            handler.send(message);
                        } catch (IOException e) {
                            connectionHandlers.remove(handler);
                        }
                    }

                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}