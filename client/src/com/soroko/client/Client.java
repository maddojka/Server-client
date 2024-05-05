package com.soroko.client;

import com.soroko.common.FileMessage;
import com.soroko.common.SendReceive;
import com.soroko.common.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private InetSocketAddress address;
    private String username;
    private Scanner scanner;
    private SendReceive connectionHandler;
    private boolean isEmpty;

    public Client(InetSocketAddress address) {
        this.address = address;
        scanner = new Scanner(System.in);
    }

    public void saveCommand() {
        System.out.println("Укажите папку, в которую необходимо загрузить файл из сервера");
        String filepath = scanner.nextLine();
        System.out.println("Введите название файла из списка доступных файлов:");
        String description = scanner.nextLine();
        FileMessage fileMessage = new FileMessage(description, filepath);
        fileMessage.setFilePath(filepath);
        try {
            connectionHandler.sendFileDescription(fileMessage);
        } catch (IOException e) {
            connectionHandler.close();
        }
    }

    public void loadCommand() {
        System.out.println("Введите путь, по которому необходимо загрузить файл на сервер");
        String filepath = scanner.nextLine();
        System.out.println("Введите описание файла:");
        String description = scanner.nextLine();
        System.out.println("Введите размер файла в мегабайтах:");
        int size = scanner.nextInt();
        FileMessage fileMessage = new FileMessage(description, size);
        fileMessage.setFilePath(filepath);
        try {
            connectionHandler.sendFileDescription(fileMessage);
        } catch (IOException e) {
            connectionHandler.close();
        }
    }

    private class Writer extends Thread {
        public void run() {
            boolean isLoadCommand = false;
            boolean isSaveCommand = false;
            while (true) {
                if (isLoadCommand) {
                    loadCommand();
                    isLoadCommand = false;
                } else if (isSaveCommand) {
                    saveCommand();
                    isSaveCommand = false;
                } else System.out.println("Введите текст сообщения");
                String text = scanner.nextLine();
                if (text.equalsIgnoreCase("/loadfile")) isLoadCommand = true;
                if (text.equalsIgnoreCase("/savefile")) isSaveCommand = true;
                if (text.equalsIgnoreCase("/exit")) {
                    System.out.println("Соединение прекращено");
                    connectionHandler.close();
                    break;
                }
                Message message = new Message(username);
                message.setText(text);
                try {
                    connectionHandler.send(message);
                } catch (IOException ignored) {
                    connectionHandler.close();
                }
            }
        }
    }

    private class Reader extends Thread {
        public void run() {
            while (true) {
                Message message;
                try {
                    message = connectionHandler.receive();
                    if (message.getText().equalsIgnoreCase("/exit")) {
                        connectionHandler.close();
                        break;
                    }
                } catch (IOException ignored) {
                    connectionHandler.close();
                    break;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(message.getText());
                if (message.isEmpty()) isEmpty = true;
            }
        }
    }

    public void createConnection() throws IOException {
        connectionHandler = new SendReceive(
                new Socket(address.getHostName(), address.getPort()));
    }

    public void startClient() {
        System.out.println("Введите имя");
        username = scanner.nextLine();
        try {
            createConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Writer().start();
        new Reader().start();
    }
}





