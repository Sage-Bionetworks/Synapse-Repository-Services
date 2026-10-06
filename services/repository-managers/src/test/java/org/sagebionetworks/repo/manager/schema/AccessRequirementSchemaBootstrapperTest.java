package org.sagebionetworks.repo.manager.schema;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementSchemaBootstrapperTest {

	@Mock
	private SynapseSchemaBootstrap mockBootstrap;

	@Mock
	private UserManager mockUserManager;

	@Test
	public void testConstructor() {
		UserInfo admin = new UserInfo(true, 123L, "realm");
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);

		// call under test
		new AccessRequirementSchemaBootstrapper(mockBootstrap, mockUserManager);

		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockBootstrap).bootstrapAccessRequirementBaseSchema(admin);
	}
}
