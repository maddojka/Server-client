package com.soroko.common;

import java.io.File;

public class FileHandler {
    private File file;
    private String filename;

    public FileHandler(File file, String filename) {
        this.filename = filename;
        this.file = file;
    }

}
