package com.soroko.common;

import java.io.Serializable;
import java.nio.file.Paths;

public class FileMessage implements Serializable {
    private String description;
    private int size;
    private String filePath;



    public FileMessage(String description, String filepath) {
        this.description = description;
        this.filePath = filepath;
    }

    public FileMessage(String description, int size) {
        this.description = description;
        this.size = size;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        String fileName = Paths.get(filePath).getFileName().toString();
        return "\n" + "название: " + fileName +
                ", описание: " + description +
                ", размер в мб: " + size;
    }
}
