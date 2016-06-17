package ru.mail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import ru.mail.ftp.singleserver.utils.ClientPool;
import ru.mail.ftp.singleserver.SingleServerAccessor;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan(basePackages="ru.mail")
@PropertySource({"classpath:server.properties", "classpath:credentials.properties"})
public class AppConfig {
    @Value("${server.mask}")
    private String serverMask;
    @Value("${server.number}")
    private int number;
    @Value("${server.login}")
    private String login;
    @Value("${server.password}")
    private String password;

    private final String destinationDir = System.getProperty("user.dir") + "\\logs";

    @Bean
    @Scope("prototype")
    public SingleServerAccessor singleServerAccessor() {
        SingleServerAccessor accessor = new SingleServerAccessor();
        accessor.setDestinationDir(destinationDir());
        return accessor;
    }

    @Bean
    @Scope("prototype")
    public ClientPool clientPool(String server, String login, String password) {
        return new ClientPool(server, login, password);
    }

    @Bean
    public List<SingleServerAccessor> serversList() {
        List<SingleServerAccessor> servers = new ArrayList<>();
        for (int i = 1; i <= number; i++) {
            String server = String.format(serverMask, i);
            ClientPool pool = clientPool(server, login, password);
            SingleServerAccessor accessor = singleServerAccessor();
            accessor.setClientPool(pool);
            accessor.setServer(server);
            servers.add(accessor);
        }
        return servers;
    }

    @Bean
    public String destinationDir() {
        return destinationDir;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
