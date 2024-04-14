package com.soroko.server;


import com.soroko.common.common.Message;
import com.soroko.common.common.SendReceive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Server {
    private int port;
    private int reqCounter = 0;
    private long elapsedTime;
    private int helpCounter = 0;
    private int pingCounter = 0;
    private int requestsCounter = 0;
    private int popularCounter = 0;

    private Map<String, Integer> requests = new HashMap<>();

    public Server(int port) {
        this.port = port;
    }

    public String makeRequest(String text) {
        if (text.equals("/help")) {
            reqCounter++;
            requests.put("/help", ++helpCounter);
            return "/help - список доступных запросов и их описание\n" +
                    "/ping - время ответа сервера\n" +
                    "/requests - количество успешно обработанных запросов\n" +
                    "/popular - название самого популярного запроса\n" +
                    "/exit - завершить соединение с сервером";
        } else if (text.equals("/ping")) {
            reqCounter++;
            requests.put("ping", ++pingCounter);
            return elapsedTime + " ms";
        } else if (text.equals("/requests")) {
            reqCounter++;
            requests.put("requests", ++requestsCounter);
            return String.valueOf(reqCounter);
        } else if (text.equals("/popular")) {
            reqCounter++;
            requests.put("popular", ++popularCounter);
            return requests.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .orElseGet(null)
                    .toString();
        } else {
            return "Невозможно обработать запрос";
        }
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    long startTime = System.nanoTime();
                    SendReceive connetionHandler = new SendReceive(socket);
                    Message fromClient = connetionHandler.receive();
                    String fromServer = makeRequest(fromClient.getText());
                    Message message = new Message("server");
                    message.setText(fromServer);
                    connetionHandler.send(message);
                    elapsedTime = (System.nanoTime() - startTime) / 1_000_000L;
                } catch (Exception e) {
                    System.out.println("Проблема с соединением");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка запуска сервера");
        }
    }
}
