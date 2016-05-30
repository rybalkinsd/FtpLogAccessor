package ru.mail.ftp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Created by s.rybalkin on 25.05.2016.
 */
public class MultipleAccessor {
    private static final Logger log = LoggerFactory.getLogger(MultipleAccessor.class);

    private final String serverMask;
    private final int serverNumber;
    private final String login;
    private final String password;
    private final String destinationDirectory;

    public MultipleAccessor(String serverMask, int serverNumber, String login, String password) {
        this.serverMask = serverMask;
        this.serverNumber = serverNumber;
        this.login = login;
        this.password = password;
        this.destinationDirectory = System.getProperty("user.dir") + "\\logs";

    }

    public void download() throws InterruptedException {
        File file = new File(destinationDirectory);
        if (!file.exists()) {
            file.mkdir();
        }

        ExecutorService executor = Executors.newFixedThreadPool(serverNumber,
                new ThreadFactoryBuilder()
                        .setNameFormat("MultipleAccessor-%d-")
                        .build());
        long startTime = System.currentTimeMillis();


        IntStream.rangeClosed(1, serverNumber).forEach(x ->
            executor.execute(new SingleServerAccessor(String.format(serverMask, x), login, password,
                    destinationDirectory))
        );

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        long downloadTime = System.currentTimeMillis() - startTime;
        log.info("Time {}", downloadTime);
    }
}
