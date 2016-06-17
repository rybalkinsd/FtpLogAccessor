package ru.mail.ftp.singleserver.utils;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.net.ftp.FTPFile;

@ToString
public class MockFile {
    public static final MockFile ROOT = new MockFile();

    private final String path;
    @Getter
    private final String name;
    @Getter
    private final boolean directory;


    private MockFile() {
        this.name = "";
        this.path = "";
        this.directory = true;
    }

    public MockFile(FTPFile file, String directory) {
        this.name = file.getName();
        this.path = directory + "/";
        this.directory = file.isDirectory();
    }
    public String getFullPath() {
        return path + name;
    }

}