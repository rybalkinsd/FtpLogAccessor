package ru.mail.ftp.singleserver.utils;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Scope("prototype")
public class ClientPool {
    private static final Logger log = LoggerFactory.getLogger(ClientPool.class);
    private static final int PORT = 21;
    private final ConcurrentLinkedQueue<FTPClient> pool = new ConcurrentLinkedQueue<>();

    private final String server;
    private final String login;
    private final String password;


    public ClientPool(String server, String login, String password) {
        this.server = server;
        this.login = login;
        this.password = password;
    }

    public FTPClient borrowClient() {
        FTPClient client;
        if ((client = pool.poll()) == null || !client.isConnected()) {
            client = createClient();
        }

        return client;
    }

    public void returnClient(FTPClient client) {
        if (client == null) {
            return;
        }

        this.pool.offer(client);
    }

    public void shutdown() {
        for (FTPClient client : pool) {
            if (client != null) {
                try {
                    client.disconnect();
                } catch (IOException ignore) { }
            }
        }
    }

    private FTPClient createClient() {
        FTPClient client = new FTPClient();
        try {
            connect(client);
        } catch (IOException e) {
            log.error("Connection to {} failed.", server);
            return null;
        }

        return client;
    }

    private void connect(FTPClient client) throws IOException {
        client.setConnectTimeout(5000);
        client.connect(server, PORT);
        client.enterLocalPassiveMode();
        client.login(login, password);
        log.info("Connected to {}.", server);
    }
}
