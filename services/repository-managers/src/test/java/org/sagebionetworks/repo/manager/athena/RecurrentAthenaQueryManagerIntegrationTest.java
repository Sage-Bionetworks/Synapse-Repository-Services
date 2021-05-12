package org.sagebionetworks.repo.manager.athena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RecurrentAthenaQueryManagerIntegrationTest {

	@Autowired
	private RecurrentAthenaQueryManagerImpl manager;
	
	@Test
	public void testFromSqsMessage() {
		
		String messageBody = "{ \"queryName\": \"UnlinkedFileHandles\", \"functionExecutionId\": \"123\""
				+ ",\"queryExecutionId\": \"456\"}";
		
		Message message = new Message()
				.withBody(messageBody);
		
		RecurrentAthenaQueryResult expected = new RecurrentAthenaQueryResult()
				.withQueryName("UnlinkedFileHandles")
				.withFunctionExecutionId("123")
				.withQueryExecutionId("456");
		
		// Call under test
		RecurrentAthenaQueryResult result = manager.fromSqsMessage(message);
		
		assertEquals(expected, result);
	}

	@Test
	public void testFromSqsMessageWithPageToken() {
		
		String messageBody = "{ \"queryName\": \"UnlinkedFileHandles\", \"functionExecutionId\": \"123\""
				+ ",\"queryExecutionId\": \"456\""
				+ ", \"pageToken\": \"token\"}";
		
		Message message = new Message()
				.withBody(messageBody);
		
		RecurrentAthenaQueryResult expected = new RecurrentAthenaQueryResult()
				.withQueryName("UnlinkedFileHandles")
				.withFunctionExecutionId("123")
				.withQueryExecutionId("456")
				.withPageToken("token");
		
		// Call under test
		RecurrentAthenaQueryResult result = manager.fromSqsMessage(message);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testFromSqsMessageWithUnknownField() {
		
		String messageBody = "{ \"queryName\": \"UnlinkedFileHandles\", \"functionExecutionId\": \"123\""
				+ ",\"queryExecutionId\": \"456\", \"somethingNew\": \"someNewValue\""
				+ ", \"pageToken\": \"token\"}";
		
		Message message = new Message()
				.withBody(messageBody);
		
		RecurrentAthenaQueryResult expected = new RecurrentAthenaQueryResult()
				.withQueryName("UnlinkedFileHandles")
				.withFunctionExecutionId("123")
				.withQueryExecutionId("456")
				.withPageToken("token");
		
		// Call under test
		RecurrentAthenaQueryResult result = manager.fromSqsMessage(message);
		
		assertEquals(expected, result);
	}

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
