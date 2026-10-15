package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
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

	@Test
	public void testConstructorWithFailedBootstrap() {
		UserInfo admin = new UserInfo(true, 123L, "realm");
		when(mockUserManager.getUserInfo(any())).thenReturn(admin);
		doThrow(new UnauthorizedException("no CREATE on the organization"))
				.when(mockBootstrap).bootstrapAccessRequirementBaseSchema(any());

		// call under test
		assertDoesNotThrow(() -> new AccessRequirementSchemaBootstrapper(mockBootstrap, mockUserManager));

		// Throwing out of this constructor would fail the deployment of the entire stack, so the
		// failure must stay confined to the form template feature that depends on the schema.
		verify(mockBootstrap).bootstrapAccessRequirementBaseSchema(admin);
	}
}
