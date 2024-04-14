package com.soroko.client;

import com.soroko.common.common.SendReceive;
import com.soroko.common.common.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private InetSocketAddress address;
    private String username;
    private Scanner scanner;

    public Client(InetSocketAddress address) {
        this.address = address;
        scanner = new Scanner(System.in);
    }



    public void startClient() {
        System.out.println("Введите имя");
        username = scanner.nextLine();
        while (true) {
            System.out.println("Введите текст сообщения");
            String text = scanner.nextLine();
            if (text.equals("/exit")) break;
            try (SendReceive connectionHandler
                         = new SendReceive(new Socket(
                    address.getHostName(),
                    address.getPort()
            ))) {
                Message message = new Message(username);
                message.setText(text);
                try {
                    connectionHandler.send(message);
                    Message fromServer = connectionHandler.receive();
                    System.out.println(fromServer.getText());
                } catch (IOException e) {
                }

            } catch (Exception e) {
            }
        }
    }
}



