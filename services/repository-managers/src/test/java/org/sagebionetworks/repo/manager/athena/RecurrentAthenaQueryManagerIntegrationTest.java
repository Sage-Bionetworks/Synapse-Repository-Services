package org.sagebionetworks.repo.manager.athena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RecurrentAthenaQueryManagerIntegrationTest {

	@Autowired
	private RecurrentAthenaQueryManagerImpl manager;
	
	@Test
	public void testToJsonMessage() {
		RecurrentAthenaQueryResult request = new RecurrentAthenaQueryResult()
				.withQueryName("UnlinkedFileHandles")
				.withFunctionExecutionId("123")
				.withQueryExecutionId("456")
				.withPageToken("token");

		String expected = "{\"queryName\":\"UnlinkedFileHandles\""
				+ ",\"functionExecutionId\":\"123\""
				+ ",\"queryExecutionId\":\"456\""
				+ ",\"pageToken\":\"token\"}";

		String result = manager.toJsonMessage(request);
		
		assertEquals(expected, result);
	}
	
}
