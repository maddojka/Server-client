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

    public Client(InetSocketAddress address) {
        this.address = address;
        scanner = new Scanner(System.in);
    }

    private class Writer extends Thread {
        public void run() {
            boolean isLoadCommand = false;
            while (true) {
                if (!isLoadCommand) System.out.println("Введите текст сообщения");
                else {
                    System.out.println("Введите путь, по которому необходимо загрузить файл на сервер");
                    // isLoadCommand = false;
                }
                String text = scanner.nextLine();
                if (isLoadCommand) {
                    System.out.println("Введите описание файла:");
                    String description = scanner.nextLine();
                    System.out.println("Введите размер файла:");
                    int size = scanner.nextInt();
                  //  FileMessage fileMessage = new FileMessage(description, size);
                  //  try {
                  //      connectionHandler.sendFileDescription(fileMessage);
                  //  } catch (IOException e) {
                  //      connectionHandler.close();
                  //      System.out.println("filedescription");
                  //  }
                    isLoadCommand = false;
                }
                if (text.equalsIgnoreCase("/loadfile")) isLoadCommand = true;
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





