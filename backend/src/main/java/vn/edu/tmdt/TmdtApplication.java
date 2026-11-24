package vn.edu.tmdt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TmdtApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmdtApplication.class, args);
    }
}
