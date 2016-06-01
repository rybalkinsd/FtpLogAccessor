package ru.mail.ftp.singleserver;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Created by s.rybalkin on 01.06.2016.
 */
class SimpleFile {
    private final String path;
    private final String name;
    private final boolean directory;

    SimpleFile() {
        this.name = "";
        this.path = "";
        this.directory = true;
    }

    SimpleFile(FTPFile file, String directory) {
        this.name = file.getName();
        this.path = directory + "/";
        this.directory = file.isDirectory();
    }
    String getFullPath() {
        return path + name;
    }

    String getName() {
        return name;
    }

    boolean isDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        return "SimpleFile{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", directory=" + directory +
                '}';
    }
}
