package ru.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.IntStream;

/**
 * Created by s.rybalkin on 24.05.2016.
 */
public class LogUploader {
    private static final Logger log = LoggerFactory.getLogger(LogUploader.class);
    private static String serverMask;
    private static int serverNumber;

    static {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream("config.properties")) {
            prop.load(stream);
            serverMask = prop.getProperty("serverMask");
            serverNumber = Integer.parseInt(prop.getProperty("serverNumber"));
        } catch (IOException e) {
            log.error("Incorrect properties", e);
            System.exit(1);
        }
    }
    public static void main(String[] args) {
        if (args.length < 2)
        {
            log.error("args[0] = login, args[1] = password.");
            return;
        }

        IntStream.rangeClosed(1, serverNumber).forEach(x -> {
            FtpAccessor accessor = null;
            try {
                accessor = new FtpAccessor(String.format(serverMask, x), args[0], args[1]);
                accessor.connect();
                accessor.downloadAll("/");
            } catch (IOException e) {
                log.error("Download failed: ", e);
            } finally {
                if (accessor != null) {
                    accessor.disconnect();
                }
            }
        });
    }
}
