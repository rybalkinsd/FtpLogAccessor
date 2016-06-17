package ru.mail.ftp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.mail.ftp.singleserver.SingleServerAccessor;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

@Component
public class MultipleServerAccessor {
    private static final Logger log = LoggerFactory.getLogger(MultipleServerAccessor.class);

    @Resource(name = "serversList")
    private List<SingleServerAccessor> servers;

    @Autowired
    @Qualifier("destinationDir")
    private String destinationDir;


    public void download() throws InterruptedException {
        File file = new File(destinationDir);
        if (!file.exists()) {
            file.mkdir();
        }

        long startTime = System.currentTimeMillis();

        servers.forEach(SingleServerAccessor::process);

        long downloadTime = System.currentTimeMillis() - startTime;
        log.info("Time {}", downloadTime);
    }
}
