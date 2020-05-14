package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynapseSchemaBootstrapImplAutowireTest {
	
	@Autowired
	private SynapseSchemaBootstrap bootstrap;
	
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	
	@BeforeEach
	public void before() {
		jsonSchemaManager.truncateAll();
	}
	
	@Test
	public void testMultipletimes() throws RecoverableMessageException {
		ListJsonSchemaInfoRequest listRequest = new ListJsonSchemaInfoRequest();
		listRequest.setOrganizationName("org.sagebionetworks");
		ListJsonSchemaInfoResponse response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertEquals(0, response.getPage().size());
		
		// call under test
		bootstrap.bootstrapSynapseSchemas();
		
		response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 3);
		for(JsonSchemaInfo schemaInfo: response.getPage()) {
			// there should only be one version of each schema
			assertEquals(1, getVersionCount(schemaInfo));
		}
		
		
		// A second call to the bootstrap must not create additional versions.s
		bootstrap.bootstrapSynapseSchemas();
		response = jsonSchemaManager.listSchemas(listRequest);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 3);
		for(JsonSchemaInfo schemaInfo: response.getPage()) {
			// there should only be one version of each schema
			assertEquals(1, getVersionCount(schemaInfo));
		}
	}
	
	/**
	 * Helper to get the number of version for a given schema.s
	 * @param info
	 * @return
	 */
	int getVersionCount(JsonSchemaInfo info){
		ListJsonSchemaVersionInfoRequest request = new ListJsonSchemaVersionInfoRequest();
		request.setOrganizationName(info.getOrganizationName());
		request.setSchemaName(info.getSchemaName());
		ListJsonSchemaVersionInfoResponse response = jsonSchemaManager.listSchemaVersions(request);
		assertNotNull(response.getPage());
		return response.getPage().size();
	}

}
