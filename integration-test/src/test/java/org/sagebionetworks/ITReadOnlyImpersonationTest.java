package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorState;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

/**
 * Tests for PLFM-8415: a member of the Synapse Read-Only Impersonators team gets a read-only token that acts as
 * another user. The token can do what the target user can do, but only view and download.
 */
@ExtendWith(ITTestExtension.class)
public class ITReadOnlyImpersonationTest {

	private static final String TEAM_ENDPOINT = "https://www.synapse.org/Team:";
	private static final String UNSUBSCRIBE_ENDPOINT = "https://www.synapse.org/unsubscribe:";

	private final SynapseAdminClient adminSynapse;
	// the injected test user is the user that gets impersonated
	private final SynapseClient targetSynapse;

	private Long targetUserId;
	private SynapseClient impersonatorSynapse;
	private Long impersonatorUserId;
	private SynapseClient outsiderSynapse;
	private Long outsiderUserId;
	// a client that authenticates with the read-only token for the target user
	private SynapseClient readOnlySynapse;

	private final List<String> projectIds = new ArrayList<>();
	private final List<Long> usersToDelete = new ArrayList<>();

	public ITReadOnlyImpersonationTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.targetSynapse = synapse;
	}

	@BeforeEach
	public void before() throws Exception {
		targetUserId = Long.valueOf(targetSynapse.getMyProfile().getOwnerId());

		impersonatorSynapse = new SynapseClientImpl();
		impersonatorUserId = SynapseClientHelper.createUser(adminSynapse, impersonatorSynapse);
		usersToDelete.add(impersonatorUserId);
		// only an admin can add members to the system team
		addToImpersonatorsTeam(impersonatorUserId);
		// the impersonator must have two-factor authentication enabled to get a token
		enable2Fa(impersonatorSynapse);

		outsiderSynapse = new SynapseClientImpl();
		outsiderUserId = SynapseClientHelper.createUser(adminSynapse, outsiderSynapse);
		usersToDelete.add(outsiderUserId);

		readOnlySynapse = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(readOnlySynapse);
		readOnlySynapse.setBearerAuthorizationToken(impersonatorSynapse.getReadOnlyAccessToken(targetUserId));
	}

	@AfterEach
	public void after() {
		for (String projectId : projectIds) {
			try {
				adminSynapse.deleteEntityById(projectId);
			} catch (SynapseException e) {
				e.printStackTrace();
			}
		}
		for (Long userId : usersToDelete) {
			try {
				adminSynapse.deleteUser(userId);
			} catch (SynapseException e) {
				e.printStackTrace();
			}
		}
	}

	private void addToImpersonatorsTeam(Long userId) throws SynapseException {
		adminSynapse.addTeamMember(BOOTSTRAP_PRINCIPAL.READ_ONLY_IMPERSONATORS.getPrincipalId().toString(), userId.toString(),
				TEAM_ENDPOINT, UNSUBSCRIBE_ENDPOINT);
	}

	private static void enable2Fa(SynapseClient client) throws Exception {
		CodeGenerator codeGenerator = new DefaultCodeGenerator();
		TimeProvider timeProvider = new SystemTimeProvider();
		TotpSecret secret = client.init2Fa();
		assertEquals(TwoFactorState.ENABLED, client.enable2Fa(new TotpSecretActivationRequest()
				.setSecretId(secret.getSecretId())
				.setTotp(codeGenerator.generate(secret.getSecret(), Math.floorDiv(timeProvider.getTime(), 30)))).getStatus());
	}

	private Project createPrivateProject(SynapseClient owner, String prefix) throws SynapseException {
		Project project = new Project();
		project.setName(prefix + UUID.randomUUID());
		project = owner.createEntity(project);
		projectIds.add(project.getId());
		return project;
	}

	@Test
	public void testTokenCanReadWhatTheTargetCanRead() throws Exception {
		Project project = createPrivateProject(targetSynapse, "impersonation target ");

		// Control: on their own the impersonator has no access to the target's private project
		assertThrows(SynapseForbiddenException.class, () -> impersonatorSynapse.getEntity(project.getId(), Project.class));

		// call under test: with the read-only token the impersonator sees it as the target
		Project read = readOnlySynapse.getEntity(project.getId(), Project.class);
		assertEquals(project.getId(), read.getId());
		assertEquals(project.getName(), read.getName());
	}

	@Test
	public void testTokenCannotReadWhatTheTargetCannotRead() throws Exception {
		Project othersProject = createPrivateProject(outsiderSynapse, "impersonation other user ");

		// The target has no access to it, so neither does a token that acts as the target
		assertThrows(SynapseForbiddenException.class, () -> targetSynapse.getEntity(othersProject.getId(), Project.class));
		assertThrows(SynapseForbiddenException.class, () -> readOnlySynapse.getEntity(othersProject.getId(), Project.class));
	}

	@Test
	public void testTokenCannotModify() throws Exception {
		Project project = createPrivateProject(targetSynapse, "impersonation read only ");
		String originalName = project.getName();

		Project read = readOnlySynapse.getEntity(project.getId(), Project.class);
		read.setName("renamed by the impersonator");

		// the target has permission to do all of these, but the token only has the view and download scopes
		assertThrows(SynapseForbiddenException.class, () -> readOnlySynapse.putEntity(read));
		assertThrows(SynapseForbiddenException.class, () -> readOnlySynapse.deleteEntityById(project.getId()));
		assertThrows(SynapseForbiddenException.class, () -> readOnlySynapse.createEntity(new Project().setName("created " + UUID.randomUUID())));

		// nothing changed
		assertEquals(originalName, targetSynapse.getEntity(project.getId(), Project.class).getName());
	}

	@Test
	public void testTokenCannotBeUsedToGetAnotherToken() throws Exception {
		assertThrows(SynapseForbiddenException.class, () -> readOnlySynapse.getReadOnlyAccessToken(outsiderUserId));
	}

	@Test
	public void testNonMemberOfTheTeamCannotGetAToken() throws Exception {
		assertThrows(SynapseForbiddenException.class, () -> outsiderSynapse.getReadOnlyAccessToken(targetUserId));
	}

	@Test
	public void testMemberOfTheTeamWithout2FaCannotGetAToken() throws Exception {
		SynapseClient memberWithout2Fa = new SynapseClientImpl();
		Long memberId = SynapseClientHelper.createUser(adminSynapse, memberWithout2Fa);
		usersToDelete.add(memberId);
		addToImpersonatorsTeam(memberId);
		
		assertThrows(SynapseUnauthorizedException.class, () -> memberWithout2Fa.getReadOnlyAccessToken(targetUserId));
	}
	
	@Test
	public void testCannotImpersonateAnAdmin() throws Exception {
		Long adminId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertThrows(SynapseForbiddenException.class, () -> impersonatorSynapse.getReadOnlyAccessToken(adminId));
	}

	@Test
	public void testTokenIsIssuedForTheTargetUser() throws Exception {
		// the token acts as the target: the profile it returns is the target's, not the impersonator's
		assertNotNull(readOnlySynapse.getMyProfile());
		assertEquals(targetUserId.toString(), readOnlySynapse.getMyProfile().getOwnerId());
	}
}
