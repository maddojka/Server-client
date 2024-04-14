package com.soroko.common.common;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;

public class SendReceive implements AutoCloseable {
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Socket socket;



    public SendReceive(Socket socket) throws IOException {
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Message message) throws IOException {
        message.setSentAt(LocalDateTime.now());
        outputStream.writeObject(message);
        outputStream.flush();
    }

    public Message receive() throws IOException, ClassNotFoundException {
        try {
            return (Message) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        inputStream.close();
        outputStream.close();
        socket.close();
    }
}
