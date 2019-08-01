package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.test.util.ReflectionTestUtils;

public class MessageFileHandleAssociationProviderTest {

	@Mock
	private MessageDAO mockMessageDAO;
	@Mock
	private MessageToUser mockMessage;
	private MessageFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new MessageFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "messageDAO", mockMessageDAO);
	}

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
}
