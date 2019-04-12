package org.sagebionetworks.annotations.worker;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
class TEMPORARYAnnotationFixWorkerIntegrationTest {

	@Autowired
	NodeManager nodeManager;


	@BeforeEach
	void setUp() {
	}
}