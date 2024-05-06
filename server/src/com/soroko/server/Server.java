package com.soroko.server;

import com.soroko.common.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


public class Server {
    public static final String SERVER_STORAGE_LOCATION
            = "C:\\Users\\yuriy\\IdeaProjects\\socketLesson\\server\\src\\com\\soroko\\server\\";
    private final int port;
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private final List<SendReceive> connectionHandlers = new CopyOnWriteArrayList<>();
    private final List<ThreadForClient> threadForClients = new CopyOnWriteArrayList<>();
    private final List<FileMessage> fileMessages = new CopyOnWriteArrayList<>();
    private final int fileSize;
    private final int amountOfSymbols;

    public Server(int port) {
        this.port = port;
        fileSize = 10;
        amountOfSymbols = 200;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    SendReceive connectionHandler = new SendReceive(socket);
                    connectionHandlers.add(connectionHandler);
                    ThreadForClient threadForClient = new ThreadForClient(connectionHandler);
                    threadForClient.start();
                    threadForClients.add(threadForClient);
                } catch (Exception e) {
                    System.out.println("Проблема с установкой нового соединения");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера");
            throw new RuntimeException(e);
        }
    }

    public static void copy(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath());
    }

    private class ThreadForClient extends Thread {
        final SendReceive connectionHandler;
        private boolean selfMessageIsActive;
        private boolean loadFileFlag;

        public ThreadForClient(SendReceive connectionHandler) {
            this.connectionHandler = connectionHandler;
        }

        public synchronized void showFiles() {
            Message message = new Message("server");
            String intro = "Список доступных файлов:";
            String fileInformation = fileMessages.stream()
                    .map(FileMessage::toString)
                    .collect(Collectors.joining(", "));
            if (fileMessages.isEmpty()) {
                message.setFilesAreEmpty(true);
                message.setText("Доступных для скачивания файлов не обнаружено");
            } else {
                message.setText(intro + fileInformation);
            }
            try {
                connectionHandler.send(message);
            } catch (IOException e) {
                connectionHandler.close();
            }
        }

        public synchronized void loadFile(FileMessage fileMessage) {
            int randomName = (int) (Math.random() * 1000);
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
                            message.setFilesAreEmpty(false);
                        }
                        answer = "Файл " + fileDestination.getName() + " был успешно загружен";
                        message.setText(answer);
                        loadFileFlag = true;
                        selfMessageIsActive = true;
                        messages.add(message);
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
            if (fileMessages.isEmpty()) message.setFilesAreEmpty(true);
            try {
                copy(fileSource, fileDestination);
                if (fileDestination.isFile()) {
                    answer = "Файл " + fileDestination.getName() + " был успешно сохранен";
                    message.setText(answer);
                    connectionHandler.send(message);
                }
            } catch (IOException e) {
                answer = "Неверное имя файла или файла нет в списке, " +
                        "либо файл с таким именем уже существует";
                message.setText(answer);
                try {
                    connectionHandler.send(message);
                } catch (IOException ex) {
                    connectionHandler.close();
                }
            }
        }

        public FileMessage createFileMessage() {
            FileMessage fileMessage;
            try {
                fileMessage = connectionHandler.receiveFileDescription();
            } catch (IOException e) {
                connectionHandlers.remove(connectionHandler);
                return null;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return fileMessage;
        }

        @Override
        public void run() {
            while (true) {
                Message fromClient;
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
                    messages.add(message);
                    System.out.println(messages);
                } else if (Objects.requireNonNull(fromClient).getText().equals("/files")) {
                    selfMessageIsActive = true;
                    showFiles();
                } else if (fromClient.getText().equals("/loadfile")) {
                    selfMessageIsActive = true;
                    FileMessage fileMessage = createFileMessage();
                    loadFile(Objects.requireNonNull(fileMessage));
                } else if (fromClient.getText().equals("/savefile")) {
                    selfMessageIsActive = true;
                    showFiles();
                    FileMessage fileMessage = createFileMessage();
                    saveFile(Objects.requireNonNull(fileMessage));
                }
                Message message = null;
                if (!messages.isEmpty()) message = messages.getLast();
                for (SendReceive handler : connectionHandlers) {
                    try {
                        if ((handler != this.connectionHandler && !selfMessageIsActive) ||
                                (handler == this.connectionHandler && this.loadFileFlag)) {
                            handler.send(Objects.requireNonNull(message));
                        }
                    } catch (IOException e) {
                        connectionHandlers.remove(handler);
                    }
                }
                selfMessageIsActive = false;
                loadFileFlag = false;
            }
        }
    }
}