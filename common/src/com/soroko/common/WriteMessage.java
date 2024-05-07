package com.soroko.common;

import java.io.IOException;
import java.util.Scanner;

public class WriteMessage extends Thread {
    private SendReceive connectionHandler;
    private String username;
    private Scanner scanner;
    private int counter;

    public WriteMessage(SendReceive sendReceive, String username, Scanner scanner) {
        this.connectionHandler = sendReceive;
        this.username = username;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        System.out.println("Введите имя");
        username = scanner.nextLine();

        while (true) {
            System.out.println("Введите текст сообщения");
            String text = scanner.nextLine();
            if (text.equals("/exit")) break;
            Message message = new Message(username);
            message.setText(text);
            try {
                Message fromServer;
                try {
                    fromServer = connectionHandler.receive();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(fromServer.getText());
            } catch (IOException e) {
            }
        }
    }
}
