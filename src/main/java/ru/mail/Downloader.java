package ru.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.ftp.MultiServerAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by s.rybalkin on 01.06.2016.
 */
public class Downloader {
    private static final Logger log = LoggerFactory.getLogger(Downloader.class);

    public static void main(String[] args) {
        if (args.length < 2)
        {
            log.error("args[0] = login, args[1] = password.");
            return;
        }
        String serverMask;
        int serverNumber;
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream("config.properties")) {
            prop.load(stream);
            serverMask = prop.getProperty("serverMask");
            serverNumber = Integer.parseInt(prop.getProperty("serverNumber"));
        } catch (IOException e) {
            log.error("Incorrect properties", e);
            throw new IllegalArgumentException(e);
        }

        try {
            new MultiServerAccessor(serverMask, serverNumber, args[0], args[1])
                    .download();
        } catch (InterruptedException e) {
            log.error("Interrupted ", e);
        }
    }
}
