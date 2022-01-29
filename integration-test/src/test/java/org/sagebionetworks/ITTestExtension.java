package org.sagebionetworks;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginResponse;

/**
 * The extension will setup a {@link SynapseAdminClient} for the admin user, the
 * {@link StackConfiguration} and a {@link SynapseClient} for a single test user.
 * <p>
 * In order to use it annotate the integration test with @ExtendWith(ITTestExtension.class). The
 * configured elements can be declared as arguments in a test constructor or in methods annotated
 * with the @BeforeEach, @BeforeAll, @AfterEach, @AfterAll or @Test annotations and they will be
 * injected by type.
 * </p>
 * <p>
 * Note that injecting a SynpaseClient creates a test user that is deleted automatically after all
 * the tests. The SynapseClient instance will be setup with the credentials of the test user.
 * </p>
 * 
 * <pre>
 * &#64;ExtendWith(ITTestExtension.class)
 * public class ITTest {
 * 
 * 	// Can be injected by constructor
 * 	public ITTest(SynapseAdminClient adminClient, SynapseClient synapseClient, StackConfiguration config) {
 * 	}
 * 
 * 	&#64;BeforeAll
 * 	public static beforeAll(SynapseAdminClient adminClient){}
 * 
 * 	&#64;AfterAll
 * 	public static afterAll(SynapseAdminClient adminClient){}
 * 
 * 	&#64;BeforeEach
 * 	public void before(StackConfiguration config) {
 * 	}
 * 
 * 	&#64;AfterEach
 * 	public void after(StackConfiguration config) {
 * 	}
 * 
 * 	&#64;Test
 * 	public void test(SynapseClient synapseClient) {
 * 	}
 * 
 * }
 * </pre>
 */
public class ITTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

	// Admin client for the admin user
	private SynapseAdminClient adminSynapse;
	// Synpase client for a test user
	private SynapseClient synapse;
	// The id of the test user
	private Long userToDelete;
	// The stack configuration
	private StackConfiguration config;

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> paramType = parameterContext.getParameter().getType();
		return paramType == SynapseClient.class || paramType == SynapseAdminClient.class || paramType == StackConfiguration.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> paramType = parameterContext.getParameter().getType();
		if (paramType == SynapseAdminClient.class) {
			return adminSynapse;
		} else if (paramType == SynapseClient.class) {
			// We don't always need a test user so we dynamically create it when requested
			if (synapse == null) {
				synapse = new SynapseClientImpl();
				try {
					userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
				} catch (Exception e) {
					throw new ParameterResolutionException(e.getMessage(), e);
				}
			}
			return synapse;
		} else if (paramType == StackConfiguration.class) {
			return config;
		}
		throw new ParameterResolutionException("Unsupported parameter " + parameterContext);
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		// Always setup an admin client before each test
		config = StackConfigurationSingleton.singleton();

		adminSynapse = new SynapseAdminClientImpl();

		SynapseClientHelper.setEndpoints(adminSynapse);

		// Authenticate to the admin services using basic auth
		String adminServiceKey = config.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		String adminServiceSecret = config.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);

		adminSynapse.setBasicAuthorizationCredentials(adminServiceKey, adminServiceSecret);
		adminSynapse.clearAllLocks();

		// Now obtains the admin user access token through the admin service
		LoginResponse response = adminSynapse.getUserAccessToken(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		// Clear the auth header to use the bearer token instead with the access token
		adminSynapse.removeAuthorizationHeader();
		adminSynapse.setBearerAuthorizationToken(response.getAccessToken());
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		if (adminSynapse != null && userToDelete != null) {
			try {
				adminSynapse.deleteUser(userToDelete);
			} catch (SynapseException e) {

			}
		}
	}

}
