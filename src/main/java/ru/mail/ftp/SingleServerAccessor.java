package ru.mail.ftp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.IntStream;


/**
 * Created by s.rybalkin on 27.05.2016.
 */
public class SingleServerAccessor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SingleServerAccessor.class);
    private static final int PORT = 21;
    private static final String FILE_MASK = "FRAG.log";
    private static final int WORKERS_NUMBER = 8;

    private final String server;
    private final String login;
    private final String password;

    private static final ThreadLocal<FTPClient> client = ThreadLocal.withInitial(FTPClient::new);
    private final BlockingQueue<SimpleFile> tasks = new LinkedBlockingQueue<>();
    private final ExecutorService executor;
    private final String destinationDir;

    SingleServerAccessor(String server, String login, String password, String destinationDir) {
        this.server = server;
        this.login = login;
        this.password = password;
        this.destinationDir = destinationDir;

        executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("SingleServerAccessor-%d-")
                        .build());

    }

    @Override
    public void run() {
        tasks.add(new SimpleFile());
        IntStream.range(0, WORKERS_NUMBER).forEach(x ->
            executor.submit(new Worker())
        );
        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("Interrupted.", e);
        }
        log.info("SingleServerAccessor {} finished.", server);
        executor.shutdownNow();
    }

    private void connect() throws IOException {
        log.info("Connection started");

        client.get().connect(server, PORT);
        client.get().enterLocalPassiveMode();
        client.get().login(login, password);

        log.info("Connected {}", server);
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            FTPClient ftpClient = client.get();
            do {
                if (!ftpClient.isConnected()) {
                    try {
                        connect();
                    } catch (IOException e) {
                        log.warn("Connection failed", e);
                        return;
                    }
                }

                SimpleFile file;
                try {
                    file = tasks.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.info("Worker interruption.", e);
                    return;
                }
                if (file == null) {
                    return;
                }
                try {
                    if (file.isDirectory()) {
                        Arrays.stream(ftpClient.listFiles(file.getFullPath()))
                                .map(x -> new SimpleFile(x, file.getFullPath()))
                                .forEach(x -> {
                                    try {
                                        tasks.put(x);
                                    } catch (InterruptedException e) {
                                        log.info("Worker interruption.", e);
                                    }
                                });
                    }
                    else if (file.getName().equals(FILE_MASK)) {
                        download(file.getFullPath());
                    }
                } catch (IOException e) {
                    log.error("Download failed. Task: " + file, e);
                }
            } while (!tasks.isEmpty() || !Thread.currentThread().isInterrupted());
            log.info("Worker finished.");
        }

        private void download(String source) throws IOException {
            FTPClient ftpClient = client.get();
            String destination = source.replaceAll("/", "@");
            File file = new File(destinationDir + "/" + destination);
            file.createNewFile();

            log.info("Downloading: " + source + " to " + destination);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                 InputStream inputStream = ftpClient.retrieveFileStream(source)) {

                byte[] buff = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, bytesRead);
                }

            } finally {
                try {
                    ftpClient.completePendingCommand();
                } catch (IOException ignore) { }
            }
        }
    }

    private class SimpleFile {
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
}


