package org.sagebionetworks.file.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleUnlinkedWorkerIntegrationTest {
	
	private static final long TIMEOUT = 3 * 60 * 1000;
	
	@BeforeEach
	public void before() {
		
	}
	
	@AfterEach
	public void afterEach() {
		
	}

	@Test
	public void testRoundTrip() {
		
	}

}
