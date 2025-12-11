package org.sagebionetworks.repo.manager.agent.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.service.UserProfileService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class UserProfileHandlerTest {

	private static final String ACTION_GROUP = "org_sage_zero";
	private static final String FUNCTION = "org_sage_zero_get_user_profile";
	private static final String PROFILE_ID = "12345";
	private static final long RUN_AS_USER_ID = 999L;
	private static final Parameter USER_ID_PARAM = new Parameter("userId", "string", PROFILE_ID);

	@Mock
	private UserProfileService userProfileService;

	@InjectMocks
	private UserProfileHandler userProfileHandler;

	@Test
	public void testGetActionGroup() {
		assertEquals(ACTION_GROUP, userProfileHandler.getActionGroup());
	}

	@Test
	public void testGetFunction() {
		assertEquals(FUNCTION, userProfileHandler.getFunction());
	}

	@Test
	public void testNeedsWriteAccess() {
		assertFalse(userProfileHandler.needsWriteAccess());
	}

	@Test
	public void testHandleEventWithoutUserId() {
		ReturnControlEvent event = new ReturnControlEvent(RUN_AS_USER_ID, ACTION_GROUP, FUNCTION, Collections.emptyList());

		String resultMessage = assertThrows(IllegalArgumentException.class, () -> {
			userProfileHandler.handleEvent(event);
		}).getMessage();

		assertEquals("Parameter 'userId' of type string is required", resultMessage);
	}

	@Test
	public void testHandleEvent() throws Exception {
		ReturnControlEvent event = new ReturnControlEvent(RUN_AS_USER_ID, ACTION_GROUP, FUNCTION, List.of(USER_ID_PARAM));

		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(PROFILE_ID);
		userProfile.setUserName("testuser");
		userProfile.setFirstName("Test");
		userProfile.setLastName("User");

		when(userProfileService.getUserProfileByOwnerId(RUN_AS_USER_ID, PROFILE_ID)).thenReturn(userProfile);

		String result = userProfileHandler.handleEvent(event);

		verify(userProfileService).getUserProfileByOwnerId(RUN_AS_USER_ID, PROFILE_ID);
		assertEquals(EntityFactory.createJSONStringForEntity(userProfile), result);
	}
}
