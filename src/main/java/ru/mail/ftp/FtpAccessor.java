package ru.mail.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by s.rybalkin on 24.05.2016.
 */
class FtpAccessor implements AutoCloseable, Runnable {
    private static final Logger log = LoggerFactory.getLogger(FtpAccessor.class);
    private static final int PORT = 21;

    private final String server;
    private final String login;
    private final String password;
    private final String destinationDir;
    private final FTPClient client;

    public FtpAccessor(String server, String login, String password) {
        this.server = server;
        this.login = login;
        this.password = password;
        this.destinationDir = System.getProperty("user.dir");
        this.client = new FTPClient();
    }

    @Override
    public void run() {
        try {
            connect();
        } catch (IOException e) {
            log.warn("Connection failed", e);
            return;
        }

        try {
            downloadAll("/");
        } catch (IOException e) {
            log.warn("Download failed", e);
        }
    }

    private void connect() throws IOException {
        log.info("Connection started");
        client.connect(server, PORT);
        client.enterLocalPassiveMode();
        client.login(login, password);


        log.info("Connected {}", server);
    }


    private void downloadAll(String dir) throws IOException {
        for (FTPFile ftpFile : client.listFiles(dir)) {
            if (ftpFile.isDirectory()) {
                downloadAll(dir + "/" + ftpFile.getName());
             } else {
                String filename = ftpFile.getName();
                if (filename.equals("FRAG.log")) {
                    String path = dir + '/' + ftpFile.getName();
                    String destination = destinationDir + '\\' + path.replaceAll("/", "@");

                    download(path, destination);
                }
            }
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    private void disconnect() {
        if (client != null) {
            try {
                log.info("Disconnected {}", server);
                client.disconnect();
            } catch (IOException ignore) { }
        }
    }

    private void download(String source, String destination) {
        File file = new File(destination);
        try {
            file.createNewFile();
        } catch (IOException e) {
            log.error("File creation failed ", e);
            return;
        }

        log.info("Downloading: " + source + " to " + destination);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destination));
             InputStream inputStream = client.retrieveFileStream(source)) {

            byte[] buff = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, bytesRead);
            }

        } catch (IOException e) {
            log.warn("Download failed ", e);
        } finally {
            try {
                client.completePendingCommand();
            } catch (IOException ignore) { }
        }
    }
}
