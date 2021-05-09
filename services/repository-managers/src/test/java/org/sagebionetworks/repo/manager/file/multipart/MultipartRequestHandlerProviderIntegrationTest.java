package org.sagebionetworks.repo.manager.file.multipart;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MultipartRequestHandlerProviderIntegrationTest {

	@Autowired
	private MultipartRequestHandlerProvider provider;
	
	@Test
	public void testHandlerForEachType() {
		for (MultiPartRequestType type : MultiPartRequestType.values()) {
			// Call under test
			MultipartRequestHandler<? extends MultipartRequest> handler = provider.getHandlerForType(type);
			assertNotNull(handler);
		}
	}
	
	@Test
	public void testHandlerForEachTypeClass() {
		for (MultiPartRequestType type : MultiPartRequestType.values()) {
			// Call under test
			MultipartRequestHandler<? extends MultipartRequest> handler = provider.getHandlerForClass(type.getRequestType());
			assertNotNull(handler);
		}
	}
	
}
