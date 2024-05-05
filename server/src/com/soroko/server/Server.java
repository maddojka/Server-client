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
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


public class Server {
    public static final String SERVER_STORAGE_LOCATION = "C:\\Users\\yuriy\\IdeaProjects\\socketLesson\\server\\src\\com\\soroko\\server\\";
    private final int port;
    private final ArrayBlockingQueue<Message> messages = new ArrayBlockingQueue<>(1000, true);
    private final List<SendReceive> connectionHandlers = new CopyOnWriteArrayList<>();
    Sender sender;
    ThreadForClient threadForClient;
    SendReceive connectionHandler;
    private int fileSize;
    private int amountOfSymbols;
    private int randomName;
    ArrayList<FileMessage> fileMessages = new ArrayList<>();

    public Server(int port) {
        this.port = port;
        fileSize = 10;
        amountOfSymbols = 200;
    }

    public synchronized void showFiles() {
        Message message = new Message("server");
        String intro = "Список доступных файлов:";
        String fileInformation = fileMessages.stream().map(FileMessage::toString).collect(Collectors.joining(", "));
        if (fileMessages.isEmpty()) {
            // message.setEmpty(true);
            message.setText("Доступных файлов не обнаружено");
        } else message.setText(intro + fileInformation);
        try {
            connectionHandler.send(message);
        } catch (IOException e) {
            connectionHandler.close();
        }
    }

    public synchronized void loadFile(FileMessage fileMessage) {
        randomName = (int) (Math.random() * 1000);
        char[] descriptionChars = fileMessage.getDescription().toCharArray();
        File fileSource = new File(fileMessage.getFilePath());
        String fileName = SERVER_STORAGE_LOCATION + fileSource.getName();
        String answer;
        File fileDestination;
        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            fileDestination = new File((SERVER_STORAGE_LOCATION + randomName + fileSource.getName()));
        } else {
            fileDestination = new File(fileName);
        }
        Message message = new Message("server");
        if (!fileSource.isDirectory() && fileSource.exists()) {
            if (fileMessage.getSize() <= fileSize && descriptionChars.length <= amountOfSymbols) {
                try {
                    copy(fileSource, fileDestination);
                    if (fileDestination.isFile()) {
                        fileMessage.setFilePath(fileDestination.getName());
                        fileMessages.add(fileMessage);
                    }
                    answer = "Файл " + fileDestination.getName() + " был успешно загружен";
                    message.setText(answer);
                    try {
                        messages.put(message);
                    } catch (InterruptedException e) {
                        messages.remove(message);
                        System.out.println(e.getMessage());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            answer = "Файл по указанному пути не найден"
                    + " или содержит слишком большой объем информации";
            message.setText(answer);
            try {
                connectionHandler.send(message);
            } catch (IOException e) {
                connectionHandler.close();
            }
        }
    }

    synchronized void saveFile(FileMessage fileMessage) {
        File fileSource = new File((SERVER_STORAGE_LOCATION + fileMessage.getDescription()));
        File fileDestination = new File(fileMessage.getFilePath() + fileMessage.getDescription());
        String answer;
        Message message = new Message("server");
        try {
            copy(fileSource, fileDestination);
            if (fileDestination.isFile()) {
                answer = "Файл " + fileDestination.getName() + " был успешно сохранен";
                message.setText(answer);
                connectionHandler.send(message);
            }
        } catch (IOException e) {
            answer = "Неверное имя файла или файла нет в списке";
            message.setText(answer);
            try {
                connectionHandler.send(message);
            } catch (IOException ex) {
                connectionHandler.close();
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
                if (fromClient != null && !fromClient.getText().equals("/files") &&
                        !fromClient.getText().equals("/loadfile") && !fromClient.getText().equals("/savefile") &&
                        !fromClient.getText().isEmpty()) {
                    Message message = new Message("server: " + fromClient.getSender());
                    message.setText(fromClient.getSentAt() + " " + fromClient.getSender() + ": " + fromClient.getText());
                    try {
                        messages.put(message);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } else if (Objects.requireNonNull(fromClient).getText().equals("/files")) {
                    showFiles();
                } else if (fromClient.getText().equals("/loadfile")) {
                    FileMessage fileMessage;
                    try {
                        fileMessage = connectionHandler.receiveFileDescription();
                    } catch (IOException e) {
                        connectionHandlers.remove(connectionHandler);
                        return;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    loadFile(fileMessage);
                } else if (fromClient.getText().equals("/savefile")) {
                    showFiles();
                    FileMessage fileMessage;
                    try {
                        fileMessage = connectionHandler.receiveFileDescription();
                    } catch (IOException e) {
                        connectionHandlers.remove(connectionHandler);
                        return;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    saveFile(fileMessage);
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
                            if (handler != null) handler.send(message);
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