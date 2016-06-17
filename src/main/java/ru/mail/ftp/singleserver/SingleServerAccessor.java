package ru.mail.ftp.singleserver;


import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import ru.mail.ftp.singleserver.utils.ClientPool;
import ru.mail.ftp.singleserver.utils.MockFile;

import java.io.*;
import java.util.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SingleServerAccessor {
    private static final Logger log = LoggerFactory.getLogger(SingleServerAccessor.class);
    private static final String FILE_MASK = "FRAG.log";
    private static final int THREAD_NUMBER = 24;
    private static final ForkJoinPool forkJoinProcessingPool = new ForkJoinPool(THREAD_NUMBER);

    @Setter
    private ClientPool clientPool;
    @Setter
    private String destinationDir;
    @Setter
    private String server;


    public void process() {
        try {
            forkJoinProcessingPool.submit(() ->
                listFiles(MockFile.ROOT).stream().parallel()
                        .filter(x -> x.getName().matches(FILE_MASK))
                        .map(MockFile::getFullPath)
                        .forEach(this::download))
                .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Processing {} failed.", server);
        }

        clientPool.shutdown();
    }

    private List<MockFile> listFiles(MockFile file) {
        if (!file.isDirectory()) {
            return Collections.singletonList(file);
        }
        Stream<FTPFile> stream;
        FTPClient ftp = clientPool.borrowClient();
        try {
            stream = Arrays.stream(ftp.listFiles(file.getFullPath()));
        } catch (IOException e) {
            log.error("Listing failed: {}, server {}.", file.getFullPath(), server);
            return Collections.emptyList();
        } finally {
            clientPool.returnClient(ftp);
        }

        return stream.parallel()
                    .map(x -> new MockFile(x, file.getFullPath()))
                    .map(this::listFiles)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    private void download(String source) {
        String destination = source.replaceAll("/", "@");
        File file = new File(destinationDir + "/" + destination);
        try {
            file.createNewFile();
        } catch (IOException e) {
            log.error("Local file creation failed {}.", file.getAbsolutePath());
            return;
        }

        FTPClient ftpClient = clientPool.borrowClient();
        if (ftpClient == null) {
            return;
        }

        log.info("Downloading: {} - {}", server, source);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
             InputStream inputStream = ftpClient.retrieveFileStream(source)) {

            byte[] buff = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, bytesRead);
            }

            if (ftpClient.isConnected()) {
                ftpClient.completePendingCommand();
            }
        } catch (IOException | NullPointerException e) {
            // Do not get client reply
            log.error("File transfer failed: {}.", source);
        } finally {
            clientPool.returnClient(ftpClient);
        }
    }
}
