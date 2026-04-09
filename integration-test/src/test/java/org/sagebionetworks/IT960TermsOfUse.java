package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.auth.TermsOfServiceState;
import org.sagebionetworks.repo.model.auth.TermsOfServiceStatus;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.oauth.OAuthTokenIntrospectionRequest;
import org.sagebionetworks.repo.model.oauth.OAuthTokenIntrospectionResponse;
import org.sagebionetworks.warehouse.WarehouseTestHelper;

@ExtendWith(ITTestExtension.class)
public class IT960TermsOfUse {
	private static SynapseClient rejectTOUsynapse;
	private static Long rejectTOUuserToDelete;
	
	private static Project project;
	private static FileEntity dataset;
	
	private WarehouseTestHelper warehouseHelper;
	private SynapseClient synapse;
	
	public IT960TermsOfUse(WarehouseTestHelper warehouseHelper, SynapseClient synapse) {
		this.warehouseHelper = warehouseHelper;
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		rejectTOUsynapse = new SynapseClientImpl();
		rejectTOUuserToDelete = SynapseClientHelper.createUser(adminSynapse, rejectTOUsynapse, false, false);
		
		project = new Project();
		project.setName("foo");
		project = adminSynapse.createEntity(project);
		// make the project public readable
		String publicGroupPrincipalId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString();
		AccessControlList acl = adminSynapse.getACL(project.getId());
		
		// Now add public-readable and push it back
		Set<ResourceAccess> resourceAccessSet = acl.getResourceAccess();
		Set<ACCESS_TYPE> accessTypes = new HashSet<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.READ);
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		
		ResourceAccess resourceAccess = new ResourceAccess();
		resourceAccess.setPrincipalId(Long.parseLong(publicGroupPrincipalId)); // add PUBLIC, READ access
		resourceAccess.setAccessType(accessTypes); // add PUBLIC, READ access
		resourceAccessSet.add(resourceAccess); // add it to the list
		adminSynapse.updateACL(acl); // push back to Synapse
		
		// a dataset added to the project will inherit its parent's permissions, i.e. will be public-readable
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL("http://foobar.com");
		efh.setFileName("foo.bar");
		efh = adminSynapse.createExternalFileHandle(efh);
		dataset = new FileEntity();
		dataset.setName("bar");
		dataset.setParentId(project.getId());
		dataset.setDataFileHandleId(efh.getId());
		dataset = adminSynapse.createEntity(dataset);
	}
	
	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		adminSynapse.deleteEntity(project);
		adminSynapse.deleteUser(rejectTOUuserToDelete);
	}
	
	@Test
	public void testRejectSynapseTermsOfUse() throws SynapseException, Exception {
		// I can download a data file because I have agreed to the Synapse terms of use
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
		
		// I cannot download the file because I have rejected the TOU
		assertThrows(SynapseForbiddenException.class, () -> rejectTOUsynapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
	}
	
	@Test
	public void testGetTermsOfServiceInfo() throws SynapseException {
		TermsOfServiceInfo tosInfo = synapse.getTermsOfServiceInfo();
		
		assertNotNull(tosInfo.getLatestTermsOfServiceVersion());
		assertEquals(String.format("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/%s/Terms.md", tosInfo.getLatestTermsOfServiceVersion()), tosInfo.getTermsOfServiceUrl());
		
		assertNotNull(tosInfo.getCurrentRequirements().getMinimumTermsOfServiceVersion());
		assertNotNull(tosInfo.getCurrentRequirements().getRequirementDate());
	}
	
	@Test
	public void testSignTermsOfServiceWithVersion(SynapseAdminClient adminSynapse) throws Exception {
		SynapseClient newUser = new SynapseClientImpl();
		
		Long userId = SynapseClientHelper.createUser(adminSynapse, newUser, false, false);
		
		TermsOfServiceStatus status = newUser.getUserTermsOfServiceStatus();
		
		assertEquals(TermsOfServiceState.MUST_AGREE_NOW, status.getUserCurrentTermsOfServiceState());
		
		newUser.signTermsOfUse(newUser.getAccessToken(), newUser.getTermsOfServiceInfo().getCurrentRequirements().getMinimumTermsOfServiceVersion());

		status = newUser.getUserTermsOfServiceStatus();
		
		assertEquals(TermsOfServiceState.UP_TO_DATE, status.getUserCurrentTermsOfServiceState());
		
		String latestVersion = newUser.getTermsOfServiceInfo().getLatestTermsOfServiceVersion();
		
		// This should work even though we already signed it
		newUser.signTermsOfUse(newUser.getAccessToken(), latestVersion);
		
		status = newUser.getUserTermsOfServiceStatus();
		
		assertEquals(TermsOfServiceState.UP_TO_DATE, status.getUserCurrentTermsOfServiceState());
		assertEquals(latestVersion, status.getLastAgreementVersion());
		
		Instant now = Instant.now();
		
		// Update the profile to trigger a snapshot
		newUser.updateMyProfile(newUser.getMyProfile().setFirstName("First Name").setLastName("Last Name"));		
		
		String query = String.format(
				"select count(*) from userprofilesnapshots where snapshot_date %s"
						+ " and id = %s and cardinality(tos_agreements) >= 1",
				warehouseHelper.toDateStringBetweenPlusAndMinusThirtySeconds(now),
				userId);
		
		warehouseHelper.assertWarehouseQuery(query);
		
		// Sleeping gives the snapshot worker a chance to take the snapshots before the test suite deletes the user.
		Thread.sleep(10_000);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatus() throws SynapseException {
		TermsOfServiceStatus status = rejectTOUsynapse.getUserTermsOfServiceStatus();
		
		assertEquals(TermsOfServiceState.MUST_AGREE_NOW, status.getUserCurrentTermsOfServiceState());
		
		assertNull(status.getLastAgreementDate());
		assertNull(status.getLastAgreementVersion());

		status = synapse.getUserTermsOfServiceStatus();
		
		assertEquals(TermsOfServiceState.UP_TO_DATE, status.getUserCurrentTermsOfServiceState());
		assertNotNull(status.getLastAgreementDate());
		assertNotNull(status.getLastAgreementVersion());
	}
	
	@Test
	public void testIntrospectTokenWithoutAcceptingTermsOfUse() throws SynapseException {
		// A user who has not accepted the ToS should still be able to introspect a token
		String token = rejectTOUsynapse.getAccessToken();

		OAuthTokenIntrospectionRequest request = new OAuthTokenIntrospectionRequest();
		request.setToken(token);

		OAuthTokenIntrospectionResponse response = rejectTOUsynapse.introspectToken(request);

		assertTrue(response.getActive());
		assertNotNull(response.getSub());
		assertNotNull(response.getExp());
		assertNotNull(response.getAud());
	}

	@Test
	public void testUpdateTermsOfServiceRequirments(SynapseAdminClient adminSynapse) throws SynapseException {
		TermsOfServiceInfo info = synapse.getTermsOfServiceInfo();
		String latestVersion = info.getLatestTermsOfServiceVersion();
		String minVersion = info.getCurrentRequirements().getMinimumTermsOfServiceVersion();
		
		// Note that this test assumes that the latest version and the min required is different, which should be
		// the case at the time of this implementation
		assertNotEquals(latestVersion, minVersion);

		// The test user is created using the min required version 
		assertEquals(TermsOfServiceState.UP_TO_DATE, synapse.getUserTermsOfServiceStatus().getUserCurrentTermsOfServiceState());
		
		// We set the min version requirements to latest version
		TermsOfServiceRequirements requirements = new TermsOfServiceRequirements()
			.setRequirementDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
			.setMinimumTermsOfServiceVersion(info.getLatestTermsOfServiceVersion());
		
		// A normal user cannot set the requirements
		System.out.println(assertThrows(SynapseForbiddenException.class, () -> {			
			synapse.updateTermsOfServiceRequirements(requirements);
		}).getMessage());
		
		adminSynapse.updateTermsOfServiceRequirements(requirements);
		
		assertEquals(TermsOfServiceState.MUST_AGREE_SOON, synapse.getUserTermsOfServiceStatus().getUserCurrentTermsOfServiceState());
		
		// We set the the date in the past to force the requirements
		adminSynapse.updateTermsOfServiceRequirements(requirements
			.setRequirementDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
		);
		
		assertEquals(TermsOfServiceState.MUST_AGREE_NOW, synapse.getUserTermsOfServiceStatus().getUserCurrentTermsOfServiceState());
		
		// Put back the original requirements so other tests don't brake
		adminSynapse.updateTermsOfServiceRequirements(requirements
			.setRequirementDate(info.getCurrentRequirements().getRequirementDate())
			.setMinimumTermsOfServiceVersion(info.getCurrentRequirements().getMinimumTermsOfServiceVersion())
		);
	}
	
 
}
