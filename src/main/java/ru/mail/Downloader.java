package ru.mail;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import ru.mail.config.AppConfig;
import ru.mail.ftp.MultipleServerAccessor;

public class Downloader {

    public static void main(String[] args) throws InterruptedException {
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        context.registerShutdownHook();
        MultipleServerAccessor accessor = (MultipleServerAccessor) context.getBean("multipleServerAccessor");
        accessor.download();
    }

}