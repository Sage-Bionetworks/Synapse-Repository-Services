package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FileHandleArchivalManagerTest {
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private ObjectMapper mockMapper;
	
	@Mock
	private AmazonSQS mockSqs;
	
	@Mock
	private FileHandleDao mockFileDao;
	
	@InjectMocks
	private FileHandleArchivalManagerImpl manager;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleArchivalRequest mockRequest;
	
	private String queueUrl = "queueUrl";
	
	@BeforeEach
	public void setup() {
		when(mockConfig.getQueueName(anyString())).thenReturn("queueName");
		when(mockSqs.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		// This is invoked automatically by spring
		manager.configureQueue(mockConfig);
	}
	
	@Test
	public void testProcessArchivalRequest() {
		
		when(mockUser.isAdmin()).thenReturn(true);
		
		FileHandleArchivalResponse expectedResponse = new FileHandleArchivalResponse().setCount(0L);
		
		// Call under test
		FileHandleArchivalResponse response = manager.processFileHandleArchivalRequest(mockUser, mockRequest);
		
		assertEquals(expectedResponse, response);
		
	}
	
	@Test
	public void testProcessArchivalRequestWithNotAdmin() {
		
		when(mockUser.isAdmin()).thenReturn(false);
		
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, mockRequest);			
		});
		
		assertEquals("Only administrators can access this service.", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithNoUser() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(null, mockRequest);			
		});
		
		assertEquals("The user is required.", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithNoRequest() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, null);			
		});
		
		assertEquals("The request is required.", ex.getMessage());
				
	}
}
