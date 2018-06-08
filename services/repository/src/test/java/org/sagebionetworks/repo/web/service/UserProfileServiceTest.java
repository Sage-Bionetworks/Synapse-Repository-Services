package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.manager.token.TokenGeneratorSingleton;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasList;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.TypeFilter;
import org.sagebionetworks.repo.model.principal.UserGroupHeaderResponse;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class UserProfileServiceTest {
	
	private static final Long EXTRA_USER_ID = 2398475L;
	private static final Long NONEXISTENT_USER_ID = 827634L;
	private static UserProfile extraProfile;
	private static UserInfo userInfo;
	private AliasList aliasList;
	List<AliasType> typeList;
	List<UserGroupHeader> headers;
	
	private UserProfileServiceImpl userProfileService = new UserProfileServiceImpl();
	
	@Mock
	private EntityPermissionsManager mockPermissionsManager;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private VerificationDAO mockVerificationDao;
	@Mock
	private PrincipalPrefixDAO mockPrincipalPrefixDAO;
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		// Create UserGroups
		List<PrincipalAlias> groups = new LinkedList<PrincipalAlias>();
		for (int i = 0; i < 10; i++) {
			PrincipalAlias alias = new PrincipalAlias();
			alias.setPrincipalId(new Long(i));
			alias.setType(AliasType.TEAM_NAME);
			alias.setAlias("g"+i);
			groups.add(alias);
		}
		
		// Create UserProfiles
		List<UserProfile> list = new ArrayList<UserProfile>();
		for (int i = 0; i < 10; i++) {
			UserProfile p = new UserProfile();
			p.setOwnerId("" + i);
			list.add(p);
		}
		// extra profile with duplicated name
		UserProfile p = new UserProfile();
		p.setOwnerId("-100");
		list.add(p);
		List<UserProfile> profiles = list;
		
		extraProfile = new UserProfile();
		extraProfile.setOwnerId(EXTRA_USER_ID.toString());
		userInfo = new UserInfo(false, EXTRA_USER_ID);

		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getUserProfile(eq(EXTRA_USER_ID.toString()))).thenReturn(extraProfile);
		when(mockUserProfileManager.getUserProfile(eq(NONEXISTENT_USER_ID.toString()))).thenThrow(new NotFoundException());
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockPrincipalAliasDAO.listPrincipalAliases(AliasType.TEAM_NAME)).thenReturn(groups);

		ReflectionTestUtils.setField(userProfileService, "entityPermissionsManager", mockPermissionsManager);
		ReflectionTestUtils.setField(userProfileService, "userProfileManager",mockUserProfileManager);
		ReflectionTestUtils.setField(userProfileService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(userProfileService, "entityManager", mockEntityManager);
		ReflectionTestUtils.setField(userProfileService, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(userProfileService, "verificationDao", mockVerificationDao);
		ReflectionTestUtils.setField(userProfileService, "principalPrefixDAO", mockPrincipalPrefixDAO);
		ReflectionTestUtils.setField(userProfileService, "tokenGenerator", mockTokenGenerator);
		
		aliasList = new AliasList();
		aliasList.setList(Lists.newArrayList("aliasOne", "aliasTwo"));
		
		typeList = Lists.newArrayList(AliasType.TEAM_NAME, AliasType.USER_NAME);
		
		List<Long> principalIds = Lists.newArrayList(101L);
		when(mockPrincipalAliasDAO.findPrincipalsWithAliases(aliasList.getList(), typeList)).thenReturn(principalIds);
		
		headers = new LinkedList<UserGroupHeader>();
		UserGroupHeader header = new UserGroupHeader();
		header.setOwnerId("101");
		headers.add(header);
		when(mockPrincipalAliasDAO.listPrincipalHeaders(principalIds)).thenReturn(headers);
	}

	
	public void testGetUserGroupHeadersByIdDoesNotExist() throws DatastoreException, NotFoundException {
		List<Long> ids = new ArrayList<Long>();
		ids.add(0L);
		ids.add(1l);
		ids.add(2L);
		ids.add(NONEXISTENT_USER_ID); // should not exist
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(3, headers.size());
		assertTrue(headers.containsKey("g0"));
		assertTrue(headers.containsKey("g1"));
		assertTrue(headers.containsKey("g2"));
		assertFalse(headers.containsKey("g10"));
	}


	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID.toString());
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);
		
		verify(mockUserProfileManager).addFavorite(userInfo, entityId);
		verify(mockEntityManager).getEntityHeader(userInfo, entityId, null);
	}

	@Test(expected=UnauthorizedException.class)
	public void testAddFavoriteUnauthorized() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID.toString());
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);		
		fail();
	}
	
	private static IdList singletonIdList(String id) {
		IdList result = new IdList();
		result.setList(Collections.singletonList(Long.parseLong(id)));
		return result;
	}
	
	private static ListWrapper<UserProfile> wrap(UserProfile up) {
		return ListWrapper.wrap(Collections.singletonList(up), UserProfile.class);
	}
	
	@Test
	public void testPrivateFieldCleaning() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "9999";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);
		userProfile.setEmails(Collections.singletonList(email));
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(profileId)).thenReturn(userProfile);
		when(mockUserProfileManager.list(singletonIdList(ownerId))).thenReturn(wrap(userProfile));
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertNull(someOtherUserProfile.getEtag());
		assertNull(someOtherUserProfile.getEmails());
		
		ListWrapper<UserProfile> lwup = userProfileService.listUserProfiles(EXTRA_USER_ID, singletonIdList(ownerId));
		assertEquals(1, lwup.getList().size());
		someOtherUserProfile = lwup.getList().get(0);
		assertNull(someOtherUserProfile.getEtag());
		assertNull(someOtherUserProfile.getEmails());
	}

	@Test
	public void testPrivateFieldCleaningAdmin() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "9999";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);
		userProfile.setEmails(Collections.singletonList(email));

		userInfo = new UserInfo(true, EXTRA_USER_ID);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(profileId)).thenReturn(userProfile);
		when(mockUserProfileManager.list(singletonIdList(ownerId))).thenReturn(wrap(userProfile));
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertNull(someOtherUserProfile.getEtag());
		assertNotNull(someOtherUserProfile.getEmails());
		
		ListWrapper<UserProfile> lwup = userProfileService.listUserProfiles(EXTRA_USER_ID, singletonIdList(ownerId));
		assertEquals(1, lwup.getList().size());
		someOtherUserProfile = lwup.getList().get(0);
		assertNull(someOtherUserProfile.getEtag());
		assertNotNull(someOtherUserProfile.getEmails());
	}
	
	@Test
	public void testUpdateNotificationSettings() throws Exception {
		NotificationSettingsSignedToken notificationSettingsSignedToken = new NotificationSettingsSignedToken();
		Long userId = 101L;
		notificationSettingsSignedToken.setUserId(userId.toString());
		Settings settings = new Settings();
		settings.setSendEmailNotifications(false);
		notificationSettingsSignedToken.setSettings(settings);
		notificationSettingsSignedToken.setHmac("signed");
		
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(userId.toString());
		when(mockUserProfileManager.getUserProfile(userId.toString())).thenReturn(userProfile);
		
		userProfileService.updateNotificationSettings(notificationSettingsSignedToken);
		
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserProfileManager).getUserProfile(userId.toString());
		verify(mockUserProfileManager).updateUserProfile(userInfo, userProfile);
		Settings settings2 = userProfile.getNotificationSettings();
		assertNotNull(settings2);
		assertFalse(settings2.getSendEmailNotifications());
		// since this setting didn't exist before, it still does not exist
		assertNull(settings2.getMarkEmailedMessagesAsRead());
	}
	
	private VerificationSubmission mockVerificationSubmission(Long userId, VerificationStateEnum currentState) {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		VerificationState state1 = new VerificationState(); 
		state1.setState(VerificationStateEnum.SUBMITTED);
		state1.setCreatedBy("000");
		state1.setCreatedOn(new Date());
		VerificationState state2 = new VerificationState(); 
		state2.setState(currentState);
		state2.setCreatedBy("000");
		state2.setCreatedOn(new Date());
		verificationSubmission.setStateHistory(Arrays.asList(state1, state2));
		AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
		attachmentMetadata.setId("123");
		verificationSubmission.setAttachments(Collections.singletonList(attachmentMetadata));
		verificationSubmission.setEmails(Collections.singletonList("test@example.com"));
		when(mockVerificationDao.
				getCurrentVerificationSubmissionForUser(userId)).thenReturn(verificationSubmission);
		return verificationSubmission;
	}
	
	private UserProfile mockUserProfile(Long userId) {
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(userId.toString());
		userProfile.setEmails(Collections.singletonList(email));
		when(mockUserProfileManager.getUserProfile(userId.toString())).thenReturn(userProfile);
		return userProfile;
	}
	
	private void mockUserInfo(Long userId, boolean isACTMember, boolean isCertified) {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(userId);
		userInfo.setGroups(new HashSet<Long>(Arrays.asList(userId)));
		if (isACTMember) userInfo.getGroups().add(TeamConstants.ACT_TEAM_ID);
		if (isCertified) userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
	}
	
	private void mockOrcid(Long userId, String alias) {
		PrincipalAlias orcidAlias = new PrincipalAlias();
		orcidAlias.setAlias(alias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId, AliasType.USER_ORCID)).
			thenReturn(Collections.singletonList(orcidAlias));		
	}
	
	@Test
	public void testGetMyOwnUserBundle() throws Exception {
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/true, /*isCertified*/true);
		VerificationSubmission verificationSubmission = mockVerificationSubmission(EXTRA_USER_ID, VerificationStateEnum.APPROVED);
		UserProfile userProfile = mockUserProfile(EXTRA_USER_ID);
		mockOrcid(EXTRA_USER_ID, "http://orcid.org/foo");
		UserBundle result = userProfileService.getMyOwnUserBundle(EXTRA_USER_ID, 63/*everything*/);
		
		assertEquals(EXTRA_USER_ID.toString(), result.getUserId());
		assertEquals(userProfile, result.getUserProfile());
		assertEquals("test@example.com", result.getUserProfile().getEmails().get(0));
		assertTrue(result.getIsACTMember());
		assertTrue(result.getIsCertified());
		assertTrue(result.getIsVerified());
		assertEquals("http://orcid.org/foo", result.getORCID());
		assertEquals(verificationSubmission, result.getVerificationSubmission());
	}

	@Test
	public void testGetMyOwnUserBundleNoPassingRecord() throws Exception {
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/true, /*isCertified*/false);

		VerificationSubmission verificationSubmission = mockVerificationSubmission(EXTRA_USER_ID, VerificationStateEnum.APPROVED);
		UserProfile userProfile = mockUserProfile(EXTRA_USER_ID);
		mockOrcid(EXTRA_USER_ID, "http://orcid.org/foo");
		UserBundle result = userProfileService.getMyOwnUserBundle(EXTRA_USER_ID, 63/*everything*/);
		
		assertEquals(EXTRA_USER_ID.toString(), result.getUserId());
		assertEquals(userProfile, result.getUserProfile());
		assertEquals("test@example.com", result.getUserProfile().getEmails().get(0));
		assertTrue(result.getIsACTMember());
		assertFalse(result.getIsCertified());
		assertTrue(result.getIsVerified());
		assertEquals("http://orcid.org/foo", result.getORCID());
		assertEquals(verificationSubmission, result.getVerificationSubmission());
	}

	@Test
	public void testGetMyOwnUserBundleNullRecords() throws Exception {
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/false, /*isCertified*/false);
		UserProfile userProfile = mockUserProfile(EXTRA_USER_ID);
		when(mockPrincipalAliasDAO.listPrincipalAliases(EXTRA_USER_ID, AliasType.USER_ORCID)).
			thenReturn(Collections.EMPTY_LIST);

		UserBundle result = userProfileService.getMyOwnUserBundle(EXTRA_USER_ID, 63/*everything*/);
		
		assertEquals(EXTRA_USER_ID.toString(), result.getUserId());
		assertEquals(userProfile, result.getUserProfile());
		assertEquals("test@example.com", result.getUserProfile().getEmails().get(0));
		assertFalse(result.getIsACTMember());
		assertFalse(result.getIsCertified());
		assertFalse(result.getIsVerified());
		assertNull(result.getORCID());
		assertNull(result.getVerificationSubmission());
	}
	
	@Test
	public void testUserBundleMask() throws Exception {
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/true, /*isCertified*/true);
		mockVerificationSubmission(EXTRA_USER_ID, VerificationStateEnum.APPROVED);
		mockUserProfile(EXTRA_USER_ID);
		mockOrcid(EXTRA_USER_ID, "http://orcid.org/foo");
		
		UserBundle result = userProfileService.getMyOwnUserBundle(EXTRA_USER_ID, 0/*nothing*/);
		
		assertEquals(EXTRA_USER_ID.toString(), result.getUserId());
		// all fields should be null
		assertNull(result.getUserProfile());
		assertNull(result.getIsACTMember());
		assertNull(result.getIsCertified());
		assertNull(result.getIsVerified());
		assertNull(result.getORCID());
		assertNull(result.getVerificationSubmission());
	}
	
	@Test
	public void testUserBundlePublic() throws Exception {
		Long bundleOwner = 101L;
		mockUserInfo(bundleOwner, /*isInACT*/false, /*isCertified*/true);
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/false, /*isCertified*/false);
		VerificationSubmission verificationSubmission = mockVerificationSubmission(bundleOwner, VerificationStateEnum.APPROVED);
		mockUserProfile(bundleOwner);
		mockOrcid(bundleOwner, "http://orcid.org/foo");
		
		UserBundle result = userProfileService.
				getUserBundleByOwnerId(EXTRA_USER_ID, bundleOwner.toString(), 63/*get everything*/);
		
		assertEquals(bundleOwner.toString(), result.getUserId());
		assertNull(result.getUserProfile().getEmails()); // scrubbed of private info
		assertFalse(result.getIsACTMember());
		assertTrue(result.getIsCertified());
		assertTrue(result.getIsVerified());
		assertEquals("http://orcid.org/foo", result.getORCID());
		assertEquals(verificationSubmission, result.getVerificationSubmission());
		assertNull(verificationSubmission.getEmails());
		assertNull(verificationSubmission.getAttachments());
		for (VerificationState state : verificationSubmission.getStateHistory()) {
			assertNull(state.getCreatedBy());
			assertNotNull(state.getCreatedOn());
			assertNotNull(state.getState());
		}
		
		// if the verification submission was rejected then we don't get it at all
		verificationSubmission = mockVerificationSubmission(bundleOwner, VerificationStateEnum.REJECTED);
		result = userProfileService.
				getUserBundleByOwnerId(EXTRA_USER_ID, bundleOwner.toString(), 63/*get everything*/);
		assertFalse(result.getIsVerified());
		assertNull(result.getVerificationSubmission());
		
		// unless we're in the ACT
		mockUserInfo(EXTRA_USER_ID, /*isInACT*/true, /*isCertified*/false);
		result = userProfileService.
				getUserBundleByOwnerId(EXTRA_USER_ID, bundleOwner.toString(), 63/*get everything*/);
		assertFalse(result.getIsVerified());
		assertEquals(verificationSubmission, result.getVerificationSubmission());
		assertNotNull(verificationSubmission.getEmails());
		assertNotNull(verificationSubmission.getAttachments());
		for (VerificationState state : verificationSubmission.getStateHistory()) {
			assertNotNull(state.getCreatedBy());
			assertNotNull(state.getCreatedOn());
			assertNotNull(state.getState());
		}
		
	}
	
	@Test
	public void testListPrincipalsForPrefixFilterAll(){
		String prefix = "aab";
		TypeFilter filter = TypeFilter.ALL;
		long offset = 1L;
		long limit = 10L;
		List<Long> expectedResutls = Lists.newArrayList(111L,222L);
		when(mockPrincipalPrefixDAO.listPrincipalsForPrefix(prefix, limit, offset)).thenReturn(expectedResutls);
		// call under test
		List<Long> results = userProfileService.listPrincipalsForPrefix(prefix, filter, offset, limit);
		assertEquals(expectedResutls, results);
		// the filtered call should not be made.
		verify(mockPrincipalPrefixDAO, never()).listPrincipalsForPrefix(anyString(), anyBoolean(), anyLong(), anyLong());
	}
	
	@Test
	public void testListPrincipalsForPrefixFilterUsersOnly(){
		String prefix = "aab";
		TypeFilter filter = TypeFilter.USERS_ONLY;
		long offset = 1L;
		long limit = 10L;
		List<Long> expectedResutls = Lists.newArrayList(111L,222L);
		boolean isIndividual = true;
		when(mockPrincipalPrefixDAO.listPrincipalsForPrefix(prefix, isIndividual, limit, offset)).thenReturn(expectedResutls);
		// call under test
		List<Long> results = userProfileService.listPrincipalsForPrefix(prefix, filter, offset, limit);
		assertEquals(expectedResutls, results);
		// the non-filtered should not be called
		verify(mockPrincipalPrefixDAO, never()).listPrincipalsForPrefix(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testListPrincipalsForPrefixFilterTeamsOnly(){
		String prefix = "aab";
		TypeFilter filter = TypeFilter.TEAMS_ONLY;
		long offset = 1L;
		long limit = 10L;
		List<Long> expectedResutls = Lists.newArrayList(111L,222L);
		boolean isIndividual = false;
		when(mockPrincipalPrefixDAO.listPrincipalsForPrefix(prefix, isIndividual, limit, offset)).thenReturn(expectedResutls);
		// call under test
		List<Long> results = userProfileService.listPrincipalsForPrefix(prefix, filter, offset, limit);
		assertEquals(expectedResutls, results);
		// the non-filtered should not be called
		verify(mockPrincipalPrefixDAO, never()).listPrincipalsForPrefix(anyString(), anyLong(), anyLong());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testListPrincipalsForPrefixFilterNull(){
		String prefix = "aab";
		TypeFilter filter = null;
		long offset = 1L;
		long limit = 10L;
		List<Long> expectedResutls = Lists.newArrayList(111L,222L);
		boolean isIndividual = false;
		when(mockPrincipalPrefixDAO.listPrincipalsForPrefix(prefix, isIndividual, limit, offset)).thenReturn(expectedResutls);
		// call under test
		userProfileService.listPrincipalsForPrefix(prefix, filter, offset, limit);
	}
	
	@Test
	public void testGetUserGroupHeadersByAlias(){
		UserGroupHeaderResponse response = userProfileService.getUserGroupHeadersByAlias(aliasList);
		assertNotNull(response);
		assertNotNull(response.getList());
		assertEquals(headers, response.getList());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUserGroupHeadersByAliasNullRequest(){
		aliasList = null;
		// call under test
		userProfileService.getUserGroupHeadersByAlias(aliasList);
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUserGroupHeadersByAliasEmptyList(){
		aliasList.setList(new LinkedList<String>());
		// call under test
		userProfileService.getUserGroupHeadersByAlias(aliasList);
	}
	
	@Test
	public void testGetUserGroupHeadersByAliasAtLimit(){
		List<String> list = new LinkedList<String>();
		aliasList.setList(list);
		for(int i=0; i < UserProfileServiceImpl.MAX_HEADERS_PER_REQUEST; i++){
			list.add("a"+i);
		}
		assertEquals(list.size(), UserProfileServiceImpl.MAX_HEADERS_PER_REQUEST);
		// call under test
		userProfileService.getUserGroupHeadersByAlias(aliasList);
	}
	
	@Test
	public void testGetUserGroupHeadersByAliasOverLimit(){
		List<String> list = new LinkedList<String>();
		aliasList.setList(list);
		for(int i=0; i < UserProfileServiceImpl.MAX_HEADERS_PER_REQUEST+1; i++){
			list.add("a"+i);
		}
		assertEquals(list.size(), UserProfileServiceImpl.MAX_HEADERS_PER_REQUEST+1);
		// call under test
		try {
			userProfileService.getUserGroupHeadersByAlias(aliasList);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(""+UserProfileServiceImpl.MAX_HEADERS_PER_REQUEST));
		}
	}
	
	@Test
	public void testGetUserGroupHeadersByPrefixNullFilter(){
		String prefix = "aab";
		TypeFilter filter = null;
		int offset = 1;
		int limit = 10;
		UserGroupHeader one = new UserGroupHeader();
		one.setOwnerId("1");
		UserGroupHeader two = new UserGroupHeader();
		two.setOwnerId("2");
		List<UserGroupHeader> headers = Lists.newArrayList(one, two);
		when(mockPrincipalAliasDAO.listPrincipalHeaders(anyListOf(Long.class))).thenReturn(headers);
		
		// call under test
		UserGroupHeaderResponsePage page = userProfileService.getUserGroupHeadersByPrefix(prefix, filter, offset, limit);
		assertNotNull(page);
		assertEquals(headers, page.getChildren());
		assertEquals(new Long(3), page.getTotalNumberOfResults());

		// null filter should run the non-filtered query.
		verify(mockPrincipalPrefixDAO).listPrincipalsForPrefix(prefix, new Long(limit), new Long(offset));
		// filtered version should not be called
		verify(mockPrincipalPrefixDAO, never()).listPrincipalsForPrefix(anyString(), anyBoolean(), anyLong(), anyLong());
	}
	
	@Test
	public void testGetUserGroupHeadersByPrefixTeamFilter(){
		String prefix = "aab";
		TypeFilter filter = TypeFilter.TEAMS_ONLY;
		int offset = 0;
		int limit = 2;
		UserGroupHeader one = new UserGroupHeader();
		one.setOwnerId("1");
		UserGroupHeader two = new UserGroupHeader();
		two.setOwnerId("2");
		List<UserGroupHeader> headers = Lists.newArrayList(one, two);
		when(mockPrincipalAliasDAO.listPrincipalHeaders(anyListOf(Long.class))).thenReturn(headers);
		
		// call under test
		UserGroupHeaderResponsePage page = userProfileService.getUserGroupHeadersByPrefix(prefix, filter, offset, limit);
		assertNotNull(page);
		assertEquals(headers, page.getChildren());
		assertEquals(new Long(3), page.getTotalNumberOfResults());

		// filter should be applied
		verify(mockPrincipalPrefixDAO, never()).listPrincipalsForPrefix(anyString(), anyLong() , anyLong());
		boolean isIndividual = false;
		verify(mockPrincipalPrefixDAO).listPrincipalsForPrefix(prefix, isIndividual, new Long(limit), new Long(offset));
	}
}
