package org.sagebionetworks.repo.manager.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;

@ExtendWith(MockitoExtension.class)
public class AgentContextValidatorImplTest {

	@Mock
	private UserInfo mockUser;

	@Mock
	private AgentContextValidatorHandler<GridAgentSessionContext> mockHandler;

	@Mock
	private SessionContext mockContext;

	private GridAgentSessionContext context;

	private AgentContextValidatorImpl validator;

	@BeforeEach
	public void before() {
		when(mockHandler.getContextType()).thenReturn((Class) GridAgentSessionContext.class);
		validator = new AgentContextValidatorImpl(List.of(mockHandler));
		context = new GridAgentSessionContext().setGridSessionId("123");
	}

	@Test
	public void testValidator() {
		// call under test
		validator.validate(mockUser, context);
		verify(mockHandler).doContextValidation(mockUser, context);
	}

	@Test
	public void testValidateWithUnknownType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(mockUser, mockContext);
		}).getMessage();
		assertTrue(message.startsWith(
				"No validator handler found for context type: org.sagebionetworks.repo.model.agent.SessionContext"));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testValidateWithNullUser() {
		mockUser = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(mockUser, mockContext);
		}).getMessage();
		assertEquals("user is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testValidateWithNullContext() {
		mockContext = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(mockUser, mockContext);
		}).getMessage();
		assertEquals("context is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}
}
