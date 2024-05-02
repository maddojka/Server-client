package com.soroko.common;

import java.io.Serializable;

public class FileMessage implements Serializable {
    private String description;
    private int size;
    private String filepath;

    public FileMessage(String description, int size) {
        this.description = description;
        this.size = size;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getDescription() {
        return description;
    }

    public int getSize() {
        return size;
    }
}
