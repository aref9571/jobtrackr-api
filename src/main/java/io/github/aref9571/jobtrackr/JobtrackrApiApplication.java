package io.github.aref9571.jobtrackr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class JobtrackrApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobtrackrApiApplication.class, args);
    }

}
