package io.github.aref9571.jobtrackr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JobtrackrApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
