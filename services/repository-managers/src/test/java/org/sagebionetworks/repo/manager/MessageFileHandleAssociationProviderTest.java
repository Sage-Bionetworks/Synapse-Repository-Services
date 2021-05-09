package org.sagebionetworks.repo.manager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class MessageFileHandleAssociationProviderTest {

	@Mock
	private MessageDAO mockMessageDAO;
	
	@Mock
	private MessageToUser mockMessage;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private MessageFileHandleAssociationProvider provider;
	

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String messageId = "1";
		String fileHandleId = "2";
		when(mockMessageDAO.getMessage(messageId)).thenReturn(mockMessage);
		when(mockMessage.getFileHandleId()).thenReturn(fileHandleId);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(fileHandleId, "4"), messageId);
		assertEquals(Collections.singleton(fileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.MESSAGE, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.MessageAttachment, provider.getAssociateType());
	}
	
}
