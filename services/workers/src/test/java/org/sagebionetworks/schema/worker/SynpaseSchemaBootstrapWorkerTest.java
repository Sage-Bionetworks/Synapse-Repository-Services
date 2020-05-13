package org.sagebionetworks.schema.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynpaseSchemaBootstrapWorkerTest {
	
	public static final long MAX_WAIT_MS = 60*1000*2;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	
	@Test
	public void testRun() throws InterruptedException {
		long start = System.currentTimeMillis();
		while(true) {
			ListJsonSchemaInfoRequest request = new ListJsonSchemaInfoRequest();
			request.setOrganizationName("org.sagebionetworks");
			ListJsonSchemaInfoResponse response = jsonSchemaManager.listSchemas(request);
			assertNotNull(response);
			assertNotNull(response.getPage());
			if(response.getPage().size() < 4) {
				System.out.println("Waiting for SynpaseSchemaBootstrapWorker...");
				Thread.sleep(2000);
			}else {
				System.out.println("Registered schemas");
				for(JsonSchemaInfo info: response.getPage()) {
					System.out.println(info);
				}
				break;
			}
			long now = System.currentTimeMillis();
			assertTrue(now - start <  MAX_WAIT_MS, "Timed out waiting for SynpaseSchemaBootstrapWorker");
		}
	}
}
