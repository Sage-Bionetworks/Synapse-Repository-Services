package org.sagebionetworks.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.schema.adapter.JSONEntity;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TypedMessageDriverRunnerAdapterTest {
	
	@Mock
	private ObjectMapper mockMapper;
		
	@Mock
	private TypedMessageDrivenRunner<JSONEntity> mockRunner;
	
	@InjectMocks
	private TypedMessageDrivenRunnerAdapter<JSONEntity> adapter;
	
	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private Message mockMessage;
	
	@Mock
	private JsonNode mockJsonNode;
	
	@Mock
	private JSONEntity mockEntity;
	
	@BeforeEach
	public void before() {
		// This is automatically called by spring usually
		adapter.configure(mockMapper);
	}
	
	@Test
	public void testRun() throws Exception {
		
		when(mockRunner.getObjectClass()).thenReturn(JSONEntity.class);
		when(mockMessage.getBody()).thenReturn("message body");
		when(mockMapper.readTree(anyString())).thenReturn(mockJsonNode);
		when(mockMapper.readValue(anyString(), any(Class.class))).thenReturn(mockEntity);
		
		// Call under test
		adapter.run(mockCallback, mockMessage);
		
		verify(mockMapper).readTree("message body");
		verify(mockMapper).readValue("message body", JSONEntity.class);
		verify(mockRunner).run(mockCallback, mockEntity);
		
	}
	
	@Test
	public void testRunFromTopic() throws Exception {
		
		when(mockRunner.getObjectClass()).thenReturn(JSONEntity.class);
		when(mockMessage.getBody()).thenReturn("message body");
		when(mockMapper.readTree(anyString())).thenReturn(mockJsonNode);
		when(mockMapper.readValue(anyString(), any(Class.class))).thenReturn(mockEntity);
		when(mockJsonNode.has("Message")).thenReturn(true);
		when(mockJsonNode.has("TopicArn")).thenReturn(true);
		when(mockJsonNode.get("Message")).thenReturn(mockJsonNode);
		when(mockJsonNode.textValue()).thenReturn("message body from topic");
		
		// Call under test
		adapter.run(mockCallback, mockMessage);
		
		verify(mockMapper).readTree("message body");
		verify(mockMapper).readValue("message body from topic", JSONEntity.class);
		verify(mockRunner).run(mockCallback, mockEntity);
		
	}
}
