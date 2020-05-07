package org.sagebionetworks.schema.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynpaseSchemaBootstrapWorkerTest {

	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	
	@Test
	public void testRun() {
		
	}
}
