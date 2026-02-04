package com.example.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.reservation.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
@SpringBootTest
class ReservationApplicationTests {

	@Test
	void contextLoads() {
	}

}
