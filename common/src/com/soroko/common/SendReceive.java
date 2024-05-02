package com.soroko.common;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SendReceive implements AutoCloseable {
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Socket socket;



    public SendReceive(Socket socket) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }


    public void send(Message message) throws IOException {
        DateTimeFormatter formatTime = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime localTime = LocalTime.now();
        message.setSentAt(localTime.format(formatTime));
        outputStream.writeObject(message);
        outputStream.flush();
    }

    public void sendFileDescription(FileMessage fileMessage) throws IOException {
        outputStream.writeObject(fileMessage);
        outputStream.flush();
    }

    public Message receive() throws IOException, ClassNotFoundException {
        try {
            return (Message) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public FileMessage receiveFileDescription() throws IOException, ClassNotFoundException {
        try {
            return (FileMessage) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (!socket.isClosed()) {
                inputStream.close();
                outputStream.close();
                socket.close();
            }
        } catch (IOException ignored) {
            System.out.println("Проблема при закрытии потоков");
        }

    }
}
