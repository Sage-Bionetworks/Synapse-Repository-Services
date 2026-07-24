package org.sagebionetworks.repo.manager.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.docusign.DocuSignClient;
import org.sagebionetworks.docusign.EnvelopeStatusResult;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.educ.EDucFileHandleId;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.docusign.RoleLabelKey;
import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatus;
import org.sagebionetworks.repo.model.educ.EDucSignerStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.model.educ.EDucTemplateValidationResult;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucQuotaDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.educ.EDucTemplate;
import org.sagebionetworks.repo.model.educ.EDucTemplateListRequest;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;
import org.sagebionetworks.repo.model.educ.EDucSignatureQuota;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.util.Clock;


@ExtendWith(MockitoExtension.class)
public class EDucManagerTest {

	@Mock
	private DocuSignClient mockDocuSignClient;
	@Mock
	private RequestDAO mockRequestDao;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	@Mock
	private UserProfileDAO mockUserProfileDao;
	@Mock
	private EDucQuotaDao mockEDucQuotaDao;
	@Mock
	private Clock mockClock;
	@Mock
	private FileHandleManager mockFileHandleManager;

	private EDucManager eDucManager;

	private UserInfo adminUser;
	private UserInfo actUser;
	private UserInfo regularUser;

	// Mid-July 2026 in epoch ms
	private static final long JULY_15_2026_MS = 1784188800000L;

	@BeforeEach
	public void before() {
		eDucManager = new EDucManager(mockDocuSignClient, mockRequestDao, mockAccessRequirementDao,
				mockPrincipalAliasDao, mockNotificationEmailDao, mockUserProfileDao,
				mockEDucQuotaDao, mockClock, mockFileHandleManager);

		adminUser = new UserInfo(true, 1L, DEFAULT_REALM_ID);

		actUser = new UserInfo(false, 2L, DEFAULT_REALM_ID);
		actUser.setGroups(new HashSet<>(Collections.singleton(TeamConstants.ACT_TEAM_ID)));

		regularUser = new UserInfo(false, 3L, DEFAULT_REALM_ID);
		regularUser.setGroups(new HashSet<>());
	}

	private EDucTemplate template(String id) {
		EDucTemplate t = new EDucTemplate();
		t.setTemplateId(id);
		t.setName("template-" + id);
		return t;
	}

	// --- listTemplates tests ---

	@Test
	public void testListTemplatesWithNullUserInfo() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> eDucManager.listTemplates(null, new EDucTemplateListRequest()));
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testListTemplatesWithNullRequest() {
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> eDucManager.listTemplates(adminUser, null));
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testListTemplatesWithUnauthorizedUser() {
		// call under test
		assertThrows(UnauthorizedException.class,
				() -> eDucManager.listTemplates(regularUser, new EDucTemplateListRequest()));
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testListTemplatesAsAdminFirstPageWithNoNextPage() throws Exception {
		EDucTemplatePage clientPage = new EDucTemplatePage();
		clientPage.setResults(new ArrayList<>(Arrays.asList(template("a"), template("b"))));
		when(mockDocuSignClient.listTemplates(0, 51)).thenReturn(clientPage);

		// call under test
		EDucTemplatePage page = eDucManager.listTemplates(adminUser, new EDucTemplateListRequest());

		assertNotNull(page);
		assertEquals(2, page.getResults().size());
		assertNull(page.getNextPageToken());
		verify(mockDocuSignClient).listTemplates(0, 51);
	}

	@Test
	public void testListTemplatesAsACTFirstPageWithNextPage() throws Exception {
		List<EDucTemplate> results = new ArrayList<>();
		for (int i = 0; i < 51; i++) {
			results.add(template(String.valueOf(i)));
		}
		EDucTemplatePage clientPage = new EDucTemplatePage();
		clientPage.setResults(results);
		when(mockDocuSignClient.listTemplates(0, 51)).thenReturn(clientPage);

		// call under test
		EDucTemplatePage page = eDucManager.listTemplates(actUser, new EDucTemplateListRequest());

		assertNotNull(page);
		assertEquals(50, page.getResults().size());
		assertNotNull(page.getNextPageToken());
	}

	@Test
	public void testListTemplatesHonorsIncomingNextPageToken() throws Exception {
		EDucTemplatePage clientPage = new EDucTemplatePage();
		clientPage.setResults(new ArrayList<>(Arrays.asList(template("x"))));
		when(mockDocuSignClient.listTemplates(50, 51)).thenReturn(clientPage);

		EDucTemplateListRequest request = new EDucTemplateListRequest();
		request.setNextPageToken("50a50");

		// call under test
		EDucTemplatePage page = eDucManager.listTemplates(adminUser, request);

		assertEquals(1, page.getResults().size());
		assertNull(page.getNextPageToken());
		verify(mockDocuSignClient).listTemplates(50, 51);
	}

	@Test
	public void testListTemplatesWithEmptyClientResponse() throws Exception {
		EDucTemplatePage clientPage = new EDucTemplatePage();
		clientPage.setResults(new ArrayList<>());
		when(mockDocuSignClient.listTemplates(anyInt(), anyInt())).thenReturn(clientPage);

		// call under test
		EDucTemplatePage page = eDucManager.listTemplates(adminUser, new EDucTemplateListRequest());

		assertEquals(0, page.getResults().size());
		assertNull(page.getNextPageToken());
	}

	// --- routeForSignature tests ---

	private Request buildValidRequest() {
		PrincipalInvestigator pi = new PrincipalInvestigator();
		pi.setUserId("200");
		pi.setName("Dr. Jones");
		pi.setTitle("Professor");
		pi.setInstitutionalEmail("pi@university.edu");

		SigningOfficial so = new SigningOfficial();
		so.setName("Jane Admin");
		so.setTitle("VP Research");
		so.setInstitutionalEmail("so@university.edu");

		AccessorChange collab1 = new AccessorChange();
		collab1.setUserId("301");
		collab1.setType(AccessType.GAIN_ACCESS);

		AccessorChange collab2 = new AccessorChange();
		collab2.setUserId("302");
		collab2.setType(AccessType.RENEW_ACCESS);

		AccessorChange revoked = new AccessorChange();
		revoked.setUserId("303");
		revoked.setType(AccessType.REVOKE_ACCESS);

		Request request = new Request();
		request.setId("req-1");
		request.setCreatedBy("100");
		request.setAccessRequirementId("456");
		request.setInstitution("MIT");
		request.setPrincipalInvestigator(pi);
		request.setSigningOfficial(so);
		request.setAccessorChanges(List.of(collab1, collab2, revoked));
		return request;
	}

	private ManagedACTAccessRequirement buildValidAccessRequirement() {
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setId(1L);
		ar.setIsDUCRequired(true);
		ar.setEDucTemplateId("tpl-abc");
		return ar;
	}

	@Test
	public void testRouteForSignatureSuccess() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(0L);
		when(mockPrincipalAliasDao.getUserName(200L)).thenReturn("drjones");
		when(mockPrincipalAliasDao.getUserName(100L)).thenReturn("creatoruser");
		when(mockPrincipalAliasDao.getUserName(301L)).thenReturn("collab1user");
		when(mockPrincipalAliasDao.getUserName(302L)).thenReturn("collab2user");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(200L)).thenReturn("pi@synapse.org");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(100L)).thenReturn("creator@example.com");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(301L)).thenReturn("c1@example.com");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(302L)).thenReturn("c2@example.com");
		UserProfile creatorProfile = new UserProfile();
		creatorProfile.setFirstName("Creator");
		creatorProfile.setLastName("User");
		when(mockUserProfileDao.get("100")).thenReturn(creatorProfile);
		UserProfile profile1 = new UserProfile();
		profile1.setFirstName("Alice");
		profile1.setLastName("Smith");
		when(mockUserProfileDao.get("301")).thenReturn(profile1);
		UserProfile profile2 = new UserProfile();
		profile2.setFirstName("Bob");
		profile2.setLastName("Brown");
		when(mockUserProfileDao.get("302")).thenReturn(profile2);
		when(mockDocuSignClient.createEnvelope(eq("tpl-abc"), any(), any())).thenReturn("env-xyz");
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));

		// call under test
		EDucSignatureQuota result = eDucManager.routeForSignature(user, "req-1");

		assertEquals(Long.valueOf(10), result.getQuota());
		assertEquals(Long.valueOf(9), result.getRemaining());

		verify(mockEDucQuotaDao).create(eq(100L), anyLong(), eq("env-xyz"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> emailsCaptor = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<RoleLabelKey, String>> tabsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockDocuSignClient).createEnvelope(eq("tpl-abc"), emailsCaptor.capture(), tabsCaptor.capture());

		// Collaborators: createdBy=100 first, then 301, 302 from accessorChanges (PI 200 excluded)
		Map<String, String> roleEmails = emailsCaptor.getValue();
		assertEquals("pi@synapse.org", roleEmails.get("principal_investigator"));
		assertEquals("so@university.edu", roleEmails.get("signing_official"));
		assertEquals("creator@example.com", roleEmails.get("collaborator_1"));
		assertEquals("c1@example.com", roleEmails.get("collaborator_2"));
		assertEquals("c2@example.com", roleEmails.get("collaborator_3"));
		assertEquals(5, roleEmails.size());

		Map<RoleLabelKey, String> tabValues = tabsCaptor.getValue();
		assertEquals("Dr. Jones", tabValues.get(new RoleLabelKey("principal_investigator", "principal_investigator_name")));
		assertEquals("Professor", tabValues.get(new RoleLabelKey("principal_investigator", "principal_investigator_title")));
		assertEquals("pi@university.edu", tabValues.get(new RoleLabelKey("principal_investigator", "principal_investigator_email")));
		assertEquals("MIT", tabValues.get(new RoleLabelKey("principal_investigator", "principal_investigator_institution")));
		assertEquals("drjones", tabValues.get(new RoleLabelKey("principal_investigator", "principal_investigator_user_name")));
		assertEquals("Jane Admin", tabValues.get(new RoleLabelKey("signing_official", "signing_official_name")));
		assertEquals("VP Research", tabValues.get(new RoleLabelKey("signing_official", "signing_official_title")));
		assertEquals("so@university.edu", tabValues.get(new RoleLabelKey("signing_official", "signing_official_email")));
		assertEquals("creatoruser", tabValues.get(new RoleLabelKey("collaborator_1", "collaborator_1_user_name")));
		assertEquals("Creator User", tabValues.get(new RoleLabelKey("collaborator_1", "collaborator_1_name")));
		assertEquals("collab1user", tabValues.get(new RoleLabelKey("collaborator_2", "collaborator_2_user_name")));
		assertEquals("Alice Smith", tabValues.get(new RoleLabelKey("collaborator_2", "collaborator_2_name")));
		assertEquals("collab2user", tabValues.get(new RoleLabelKey("collaborator_3", "collaborator_3_user_name")));
		assertEquals("Bob Brown", tabValues.get(new RoleLabelKey("collaborator_3", "collaborator_3_name")));
	}

	@Test
	public void testRouteForSignatureWithUnauthorizedUser() {
		Request request = buildValidRequest();
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.routeForSignature(regularUser, "req-1"));

		assertEquals("Only the request creator or an administrator can route for signature.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithAdminUser() {
		Request request = buildValidRequest();
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(1L), anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(0L);
		when(mockPrincipalAliasDao.getUserName(any(Long.class))).thenReturn("someuser");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(any(Long.class))).thenReturn("x@y.com");
		UserProfile profile = new UserProfile();
		profile.setFirstName("A");
		profile.setLastName("B");
		when(mockUserProfileDao.get(any(String.class))).thenReturn(profile);
		when(mockDocuSignClient.createEnvelope(any(), any(), any())).thenReturn("env-1");
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));

		// call under test
		EDucSignatureQuota result = eDucManager.routeForSignature(adminUser, "req-1");

		assertEquals(Long.valueOf(10), result.getQuota());
		assertEquals(Long.valueOf(9), result.getRemaining());
	}

	@Test
	public void testRouteForSignatureWithExistingDraft() {
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("existing-env");
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(0L);

		// call under test — sends the existing draft
		EDucSignatureQuota result = eDucManager.routeForSignature(user, "req-1");

		verify(mockDocuSignClient).sendEnvelope("existing-env");
		assertEquals(Long.valueOf(10), result.getQuota());
		assertEquals(Long.valueOf(9), result.getRemaining());
	}

	@Test
	public void testRouteForSignatureWithNonManagedACTRequirement() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(new TermsOfUseAccessRequirement());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("The access requirement is not a ManagedACTAccessRequirement.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithDUCNotRequired() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		ManagedACTAccessRequirement ar = buildValidAccessRequirement();
		ar.setIsDUCRequired(false);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(ar);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("The access requirement does not require a DUC.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoTemplateId() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		ManagedACTAccessRequirement ar = buildValidAccessRequirement();
		ar.setEDucTemplateId(null);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(ar);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("The access requirement does not have an eDUC template ID configured.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoPrincipalInvestigator() {
		Request request = buildValidRequest();
		request.setPrincipalInvestigator(null);
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("principalInvestigator is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoPIUserId() {
		Request request = buildValidRequest();
		request.getPrincipalInvestigator().setUserId(null);
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("principalInvestigator.userId is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoSigningOfficial() {
		Request request = buildValidRequest();
		request.setSigningOfficial(null);
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("signingOfficial is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoSOEmail() {
		Request request = buildValidRequest();
		request.getSigningOfficial().setInstitutionalEmail(null);
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("signingOfficial.institutionalEmail is required.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithNoAccessorChanges() {
		Request request = buildValidRequest();
		request.setAccessorChanges(Collections.emptyList());
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(0L);
		when(mockPrincipalAliasDao.getUserName(200L)).thenReturn("drjones");
		when(mockPrincipalAliasDao.getUserName(100L)).thenReturn("creatoruser");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(200L)).thenReturn("pi@synapse.org");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(100L)).thenReturn("creator@example.com");
		UserProfile creatorProfile = new UserProfile();
		creatorProfile.setFirstName("Creator");
		creatorProfile.setLastName("User");
		when(mockUserProfileDao.get("100")).thenReturn(creatorProfile);
		when(mockDocuSignClient.createEnvelope(eq("tpl-abc"), any(), any())).thenReturn("env-no-collabs");
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));

		// call under test
		EDucSignatureQuota result = eDucManager.routeForSignature(user, "req-1");

		assertEquals(Long.valueOf(10), result.getQuota());
		assertEquals(Long.valueOf(9), result.getRemaining());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> emailsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockDocuSignClient).createEnvelope(eq("tpl-abc"), emailsCaptor.capture(), any());
		// PI + SO + createdBy as collaborator_1
		assertEquals(3, emailsCaptor.getValue().size());
		assertEquals("creator@example.com", emailsCaptor.getValue().get("collaborator_1"));
	}

	@Test
	public void testRouteForSignatureWithQuotaExceeded() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(10L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("User has exceeded their eDUC routing quota for the requested access requirement.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithGlobalLimitExceeded() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(0L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(100L);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.routeForSignature(user, "req-1"));

		assertEquals("The global daily eDUC routing limit has been reached. Please try again later.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testRouteForSignatureWithQuotaReturnsCorrectRemaining() {
		Request request = buildValidRequest();
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());
		when(mockClock.currentTimeMillis()).thenReturn(JULY_15_2026_MS);
		when(mockEDucQuotaDao.getCount(eq(100L), anyLong(), anyLong(), anyLong())).thenReturn(5L);
		when(mockEDucQuotaDao.getGlobalCount(anyLong(), anyLong())).thenReturn(0L);
		when(mockPrincipalAliasDao.getUserName(any(Long.class))).thenReturn("someuser");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(any(Long.class))).thenReturn("x@y.com");
		UserProfile profile = new UserProfile();
		profile.setFirstName("A");
		profile.setLastName("B");
		when(mockUserProfileDao.get(any(String.class))).thenReturn(profile);
		when(mockDocuSignClient.createEnvelope(any(), any(), any())).thenReturn("env-partial");
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));

		// call under test
		EDucSignatureQuota result = eDucManager.routeForSignature(user, "req-1");

		assertEquals(Long.valueOf(10), result.getQuota());
		assertEquals(Long.valueOf(4), result.getRemaining());
		verify(mockEDucQuotaDao).create(eq(100L), anyLong(), eq("env-partial"));
	}

	@Test
	public void testBuildCollaboratorUserIdsWithPIExcluded() {
		Request request = buildValidRequest();
		// Add PI (userId=200) as an accessor — should be excluded
		AccessorChange piChange = new AccessorChange();
		piChange.setUserId("200");
		piChange.setType(AccessType.GAIN_ACCESS);
		request.setAccessorChanges(List.of(piChange));

		// call under test
		List<String> result = eDucManager.buildCollaboratorUserIds(request);

		// createdBy=100 is included, PI=200 is excluded
		assertEquals(List.of("100"), result);
	}

	@Test
	public void testBuildCollaboratorUserIdsWithCreatedByInAccessorChanges() {
		Request request = buildValidRequest();
		// createdBy=100 also appears in accessorChanges — should not be duplicated
		AccessorChange creatorChange = new AccessorChange();
		creatorChange.setUserId("100");
		creatorChange.setType(AccessType.GAIN_ACCESS);
		AccessorChange otherChange = new AccessorChange();
		otherChange.setUserId("301");
		otherChange.setType(AccessType.GAIN_ACCESS);
		request.setAccessorChanges(List.of(creatorChange, otherChange));

		// call under test
		List<String> result = eDucManager.buildCollaboratorUserIds(request);

		// 100 appears only once (from createdBy, deduplicated), then 301
		assertEquals(List.of("100", "301"), result);
	}

	@Test
	public void testBuildCollaboratorUserIdsWithDuplicateAccessors() {
		Request request = buildValidRequest();
		AccessorChange change1 = new AccessorChange();
		change1.setUserId("301");
		change1.setType(AccessType.GAIN_ACCESS);
		AccessorChange change2 = new AccessorChange();
		change2.setUserId("301");
		change2.setType(AccessType.RENEW_ACCESS);
		request.setAccessorChanges(List.of(change1, change2));

		// call under test
		List<String> result = eDucManager.buildCollaboratorUserIds(request);

		// createdBy=100, then 301 (deduplicated)
		assertEquals(List.of("100", "301"), result);
	}

	// --- getSignatureStatus tests ---

	@Test
	public void testGetSignatureStatusSuccess() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-123");
		when(mockRequestDao.get("req-1")).thenReturn(request);

		EDucSignerStatus signer1Status = new EDucSignerStatus();
		signer1Status.setName("Dr. Jones");
		signer1Status.setStatus(EDucSignerStatusEnum.done);
		EDucSignerStatus signer2Status = new EDucSignerStatus();
		signer2Status.setName("Jane Admin");
		signer2Status.setStatus(EDucSignerStatusEnum.pending);

		EDucSignatureStatus envelopeStatus = new EDucSignatureStatus();
		envelopeStatus.setDucStatus(EDucStatusEnum.sent);
		envelopeStatus.setCreatedOn(new java.util.Date());
		envelopeStatus.setModifiedOn(new java.util.Date());
		envelopeStatus.setSignerStatus(List.of(signer1Status, signer2Status));

		EnvelopeStatusResult envelopeResult = new EnvelopeStatusResult(envelopeStatus,
				List.of("pi@university.edu", "so@university.edu"));
		when(mockDocuSignClient.getEnvelopeStatus("env-123")).thenReturn(envelopeResult);

		PrincipalAlias alias1 = new PrincipalAlias();
		alias1.setPrincipalId(200L);
		when(mockPrincipalAliasDao.findPrincipalWithAlias("pi@university.edu", AliasType.USER_EMAIL)).thenReturn(alias1);
		when(mockPrincipalAliasDao.findPrincipalWithAlias("so@university.edu", AliasType.USER_EMAIL)).thenReturn(null);

		// call under test
		EDucSignatureStatus result = eDucManager.getSignatureStatus(user, "req-1");

		assertEquals("req-1", result.getDataAccessRequestId());
		assertEquals(EDucStatusEnum.sent, result.getDucStatus());
		assertNotNull(result.getCreatedOn());
		assertNotNull(result.getModifiedOn());
		assertEquals(2, result.getSignerStatus().size());
		assertEquals("Dr. Jones", result.getSignerStatus().get(0).getName());
		assertEquals(EDucSignerStatusEnum.done, result.getSignerStatus().get(0).getStatus());
		assertEquals("200", result.getSignerStatus().get(0).getUserId());
		assertEquals("Jane Admin", result.getSignerStatus().get(1).getName());
		assertEquals(EDucSignerStatusEnum.pending, result.getSignerStatus().get(1).getStatus());
		assertNull(result.getSignerStatus().get(1).getUserId());
	}

	@Test
	public void testGetSignatureStatusWithUnauthorizedUser() {
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-123");
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.getSignatureStatus(regularUser, "req-1"));

		assertEquals("Only the request creator or an administrator can view signature status.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testGetSignatureStatusWithNoEnvelope() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId(null);
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.getSignatureStatus(user, "req-1"));

		assertEquals("This request does not have a routed DUC.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	// --- cancelSignature tests ---

	@Test
	public void testCancelSignatureSuccess() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-cancel");
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));

		// call under test
		eDucManager.cancelSignature(user, "req-1");

		verify(mockDocuSignClient).voidEnvelope("env-cancel", "Cancelled by user.");
		verify(mockRequestDao).update(request);
		assertNull(request.getEDucSignatureEnvelopeId());
	}

	@Test
	public void testCancelSignatureWithUnauthorizedUser() {
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-cancel");
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.cancelSignature(regularUser, "req-1"));

		assertEquals("Only the request creator or an administrator can cancel a signature.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testCancelSignatureWithNoEnvelope() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId(null);
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.cancelSignature(user, "req-1"));

		assertEquals("This request does not have a routed DUC.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	// --- getSignedDocumentFileHandle tests ---

	@Test
	public void testGetSignedDocumentFileHandleSuccess() throws Exception {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-signed");
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockDocuSignClient.getSignedDocument("env-signed")).thenReturn(new byte[]{1, 2, 3});
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("fh-999");
		when(mockFileHandleManager.createFileFromByteArray(any(), any(), any(), any(), any(), any()))
				.thenReturn(fileHandle);

		// call under test
		EDucFileHandleId result = eDucManager.getSignedDocumentFileHandle(user, "req-1");

		assertEquals("fh-999", result.getFileHandleId());
		verify(mockDocuSignClient).getSignedDocument("env-signed");
	}

	@Test
	public void testGetSignedDocumentFileHandleWithUnauthorizedUser() {
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-signed");
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.getSignedDocumentFileHandle(regularUser, "req-1"));

		assertEquals("Only the request creator or an administrator can retrieve the signed document.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testGetSignedDocumentFileHandleWithNoEnvelope() {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId(null);
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucManager.getSignedDocumentFileHandle(user, "req-1"));

		assertEquals("This request does not have a routed DUC.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	@Test
	public void testBuildFullName() {
		assertEquals("Alice Smith", EDucManager.buildFullName("Alice", "Smith"));
		assertEquals("Alice", EDucManager.buildFullName("Alice", null));
		assertEquals("Smith", EDucManager.buildFullName(null, "Smith"));
		assertNull(EDucManager.buildFullName(null, null));
	}

	// --- previewEDuc tests ---

	@Test
	public void testPreviewEDucSuccess() throws Exception {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockAccessRequirementDao.get("456")).thenReturn(buildValidAccessRequirement());
		when(mockPrincipalAliasDao.getUserName(any(Long.class))).thenReturn("someuser");
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(any(Long.class))).thenReturn("x@y.com");
		UserProfile profile = new UserProfile();
		profile.setFirstName("A");
		profile.setLastName("B");
		when(mockUserProfileDao.get(any(String.class))).thenReturn(profile);
		when(mockDocuSignClient.createEnvelope(any(), any(), any())).thenReturn("env-draft");
		when(mockRequestDao.update(any())).thenAnswer(i -> i.getArgument(0));
		when(mockDocuSignClient.getDocument("env-draft")).thenReturn(new byte[]{1, 2, 3});
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("fh-preview");
		when(mockFileHandleManager.createFileFromByteArray(any(), any(), any(), any(), any(), any()))
				.thenReturn(fileHandle);

		// call under test
		EDucFileHandleId result = eDucManager.previewEDuc(user, "req-1");

		assertEquals("fh-preview", result.getFileHandleId());
		verify(mockDocuSignClient).getDocument("env-draft");
	}

	@Test
	public void testPreviewEDucWithExistingDraft() throws Exception {
		UserInfo user = new UserInfo(false, 100L, DEFAULT_REALM_ID);
		Request request = buildValidRequest();
		request.setEDucSignatureEnvelopeId("env-existing");
		when(mockRequestDao.get("req-1")).thenReturn(request);
		when(mockDocuSignClient.getDocument("env-existing")).thenReturn(new byte[]{4, 5});
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("fh-existing");
		when(mockFileHandleManager.createFileFromByteArray(any(), any(), any(), any(), any(), any()))
				.thenReturn(fileHandle);

		// call under test
		EDucFileHandleId result = eDucManager.previewEDuc(user, "req-1");

		assertEquals("fh-existing", result.getFileHandleId());
		verify(mockDocuSignClient).getDocument("env-existing");
	}

	@Test
	public void testPreviewEDucWithUnauthorizedUser() {
		Request request = buildValidRequest();
		when(mockRequestDao.get("req-1")).thenReturn(request);

		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.previewEDuc(regularUser, "req-1"));

		assertEquals("Only the request creator or an administrator can preview the eDUC.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}

	// --- validateTemplate tests ---

	@Test
	public void testValidateTemplateWithValidTemplate() {
		// call under test
		EDucTemplateValidationResult result = eDucManager.validateTemplate(actUser, "tpl-1");

		assertEquals(true, result.getIsValid());
		assertNull(result.getReason());
		verify(mockDocuSignClient).validateTemplate("tpl-1");
	}

	@Test
	public void testValidateTemplateWithInvalidTemplate() {
		doThrow(new IllegalArgumentException("Template is missing required role: signing_official"))
				.when(mockDocuSignClient).validateTemplate("tpl-bad");

		// call under test
		EDucTemplateValidationResult result = eDucManager.validateTemplate(actUser, "tpl-bad");

		assertEquals(false, result.getIsValid());
		assertEquals("Template is missing required role: signing_official", result.getReason());
	}

	@Test
	public void testValidateTemplateWithUnauthorizedUser() {
		// call under test
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> eDucManager.validateTemplate(regularUser, "tpl-1"));

		assertEquals("Only ACT member can perform this action.", ex.getMessage());
		verifyNoInteractions(mockDocuSignClient);
	}
}
