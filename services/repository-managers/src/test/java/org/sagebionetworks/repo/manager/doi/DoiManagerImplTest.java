package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.DataciteRegistrationStatus;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DoiManagerImplTest {

	private DoiManagerImpl doiManager;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private DataciteClient mockDataciteClient;

	@Mock
	private DoiAssociationDao mockDoiDao;

	@Mock
	private UserManager mockUserManager;

	@Mock
	private AuthorizationManager mockAuthorizationManager;


	private static final String baseUrl = "https://syn.org/test/";
	private static final String repoEndpoint = "https://prod-base.sagetest.gov/repo/v3";

	private static final long USER_ID = 12345L;
	private static final String entityId = "syn584322";
	private static final ObjectType entityType = ObjectType.ENTITY;
	private static final Long version = 4L;
	private static Doi inputDto = null;
	private static Doi outputDto = null;

	private static final String mockPrefix = "10.1234";
	private static final String doiUri = "10.1234/someuri";
	private static final String doiUrl = baseUrl + entityId;

	private static final String title = "5 Easy Steps You Can Take To Become President (You Won't Believe #3!)";
	private static final String author = "Washington, George";
	private static final Long publicationYear = 1787L;
	private static final DoiResourceTypeGeneral resourceTypeGeneral = DoiResourceTypeGeneral.Dataset;

	@Before
	public void before() {
		doiManager = new DoiManagerImpl();
		ReflectionTestUtils.setField(doiManager, "stackConfiguration", mockConfig);
		when(mockConfig.getSynapseBaseUrl()).thenReturn(baseUrl);
		when(mockConfig.getRepositoryServiceEndpoint()).thenReturn(repoEndpoint);
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		ReflectionTestUtils.setField(doiManager, "dataciteClient", mockDataciteClient);
		ReflectionTestUtils.setField(doiManager, "doiAssociationDao", mockDoiDao);
		ReflectionTestUtils.setField(doiManager, "userManager", mockUserManager);
		when(mockUserManager.getUserInfo(any(Long.class))).thenReturn(new UserInfo(true));
		ReflectionTestUtils.setField(doiManager, "authorizationManager", mockAuthorizationManager);
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(new AuthorizationStatus(true, "mock"));
		inputDto = setUpDto(true);
		outputDto = setUpDto(true);
	}

	@Test
	public void testGetAssociationSuccess() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		DoiAssociation actualResponse = doiManager.getDoiAssociation(USER_ID, entityId, entityType, version);
		verify(mockDoiDao).getDoiAssociation(entityId, entityType, version);
		assertEquals(outputDto.getObjectId(), actualResponse.getObjectId());
		assertEquals(outputDto.getObjectType(), actualResponse.getObjectType());
		assertEquals(outputDto.getObjectVersion(), actualResponse.getObjectVersion());
		assertEquals(mockPrefix + "/" + outputDto.getObjectId() + "." + outputDto.getObjectVersion(), actualResponse.getDoiUri());
		assertEquals(doiManager.generateLocationRequestUrl(outputDto.getObjectId(), outputDto.getObjectType(), outputDto.getObjectVersion()), actualResponse.getDoiUrl());
	}

	@Test
	public void testGetAssociationAuthorization() {
		reset(mockUserManager, mockAuthorizationManager); // Undo the convenience authorization set in @Before
		UserInfo testInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.READ))
				.thenReturn(new AuthorizationStatus(true, "mock"));
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		doiManager.getDoiAssociation(USER_ID, entityId, entityType, version);
		verify(mockUserManager).getUserInfo(USER_ID);
		verify(mockAuthorizationManager).canAccess(testInfo, entityId, entityType, ACCESS_TYPE.READ);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAssociationUnauthorized() {
		reset(mockUserManager, mockAuthorizationManager); // Undo the convenience authorization set in @Before
		UserInfo testInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.READ))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		// Call under test
		doiManager.getDoiAssociation(USER_ID, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoUserId() {
		// Call under test
		doiManager.getDoiAssociation( null, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectId() {
		// Call under test
		doiManager.getDoiAssociation(USER_ID, null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectType() {
		// Call under test
		doiManager.getDoiAssociation(USER_ID, entityId, null, version);
	}

	@Test
	public void testGetAssociationNullObjectVersion() {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, null)).thenReturn(outputDto);
		// Call under test
		doiManager.getDoiAssociation(USER_ID, entityId, entityType, null);
		verify(mockDoiDao).getDoiAssociation(entityId, entityType, null);
	}


	@Test
	public void testGetDoiSuccess() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		Doi metadata = setUpDto(true);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenReturn(metadata);
		// Call under test
		Doi actualResponse = doiManager.getDoi(USER_ID, entityId, entityType, version);
		assertEquals(mockAssociation.getObjectId(), actualResponse.getObjectId());
		assertEquals(mockAssociation.getObjectType(), actualResponse.getObjectType());
		assertEquals(mockAssociation.getObjectVersion(), actualResponse.getObjectVersion());
		assertEquals(mockPrefix + "/" + mockAssociation.getObjectId() + "." + mockAssociation.getObjectVersion(), actualResponse.getDoiUri());
		assertEquals(doiManager.generateLocationRequestUrl(mockAssociation.getObjectId(), mockAssociation.getObjectType(), mockAssociation.getObjectVersion()), actualResponse.getDoiUrl());

		assertEquals(actualResponse.getCreators(), metadata.getCreators());
		assertEquals(actualResponse.getTitles(), metadata.getTitles());
		assertEquals(actualResponse.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(actualResponse.getResourceType(), metadata.getResourceType());
		assertEquals(actualResponse.getStatus(), metadata.getStatus());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetDoiNotReadyException() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenThrow(new NotReadyException(new AsynchronousJobStatus()));
		// Call under test
		doiManager.getDoi(USER_ID, entityId, entityType, version);
	}

	@Test
	public void testCreateOrUpdateAuthorization() throws Exception{
		UserInfo testInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(new AuthorizationStatus(true, "mock"));
		// The following mocks are necessary for the rest of the method to succeed on a "create"
		when(mockDoiDao.getEtagForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
		verify(mockUserManager).getUserInfo(USER_ID);
		verify(mockAuthorizationManager).canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testCreateOrUpdateUnauthorized() throws Exception {
		UserInfo testInfo = new UserInfo(false);
		reset(mockUserManager, mockAuthorizationManager); // Undo the convenience authorization set in @Before
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
	}

	@Test
	public void testCreateSuccess() throws Exception {
		// Test the entire create call
		when(mockDoiDao.getEtagForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);

		doiManager.createOrUpdateDoi(USER_ID, inputDto);
		verify(mockDoiDao).createDoiAssociation(inputDto);
		verify(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test
	public void testUpdateSuccess() throws Exception {
		// Test the entire update call
		inputDto.setEtag("a matching etag");
		when(mockDoiDao.getEtagForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion()))
				.thenReturn("a matching etag");

		when(mockDoiDao.updateDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
		verify(mockDoiDao).updateDoiAssociation(inputDto);
		verify(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNullUserId() throws Exception {
		// Call under test
		doiManager.createOrUpdateDoi(null, inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNullObjectId() throws Exception {
		inputDto.setObjectId(null);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNullObjectType() throws Exception {
		inputDto.setObjectType(null);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNonEntityObjectType() throws Exception {
		inputDto.setObjectType(ObjectType.PRINCIPAL);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
	}

	@Test
	public void testCreateOrUpdateNullObjectVersion() throws Exception {
		inputDto.setObjectVersion(null);
		when(mockDoiDao.getEtagForUpdate(entityId, entityType, null)).thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);
		// Call under test
		doiManager.createOrUpdateDoi(USER_ID, inputDto);
		verify(mockDoiDao).createDoiAssociation(inputDto);
	}

	@Test
	public void testCreateAssociation() throws Exception {
		when(mockDoiDao.getEtagForUpdate(entityId, entityType, version)).thenThrow(new NotFoundException());
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).getEtagForUpdate(entityId, entityType, version);
		verify(mockDoiDao).createDoiAssociation(inputDto);
	}

	@Test
	public void testUpdateAssociation() throws Exception {
		inputDto.setEtag("matching etag");
		when(mockDoiDao.getEtagForUpdate(entityId, entityType, version)).thenReturn("matching etag");
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).getEtagForUpdate(entityId, entityType, version);
		verify(mockDoiDao).updateDoiAssociation(inputDto);
	}

	@Test(expected = ConflictingUpdateException.class)
	public void testUpdateAssociationMismatchedEtag() throws Exception {
		inputDto.setEtag("nonmatching etag 1");
		when(mockDoiDao.getEtagForUpdate(entityId, entityType, version)).thenReturn("nonmatching etag2");
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testCreateAssociationFailureOnIllegalArgumentException() throws Exception {
		when(mockDoiDao.getEtagForUpdate(entityId, entityType, version)).thenThrow(new NotFoundException());

		when(mockDoiDao.createDoiAssociation(inputDto)).thenThrow(new IllegalArgumentException());
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);
	}

	@Test
	public void testCreateOrUpdateMetadata() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		inputDto.setStatus(DataciteRegistrationStatus.FINDABLE);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
		verify(mockDataciteClient).registerMetadata(inputDto, doiUri);
		verify(mockDataciteClient).registerDoi(doiUri, doiUrl);
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test
	public void testCreateOrUpdateMetadataNullStatus() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		inputDto.setStatus(null);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
		verify(mockDataciteClient).registerMetadata(inputDto, doiUri);
		verify(mockDataciteClient).registerDoi(doiUri, doiUrl);
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test
	public void testCreateMetadataAndDeactivate() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		inputDto.setStatus(DataciteRegistrationStatus.REGISTERED);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
		verify(mockDataciteClient).registerMetadata(inputDto, doiUri);
		verify(mockDataciteClient).registerDoi(doiUri, doiUrl);
		verify(mockDataciteClient).deactivate(doiUri);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateMetadataNoUri() throws Exception {
		inputDto.setDoiUri(null);
		inputDto.setDoiUrl(doiUrl);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateMetadataNoUrl() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(null);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testCreateMetadataNotReady() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		doThrow(new NotReadyException(new AsynchronousJobStatus())).when(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testCreateMetadataServiceUnavailable() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
	}


	@Test
	public void testDeactivateSuccess() throws Exception {
		DoiAssociation association = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(association);
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, entityType, version);
		verify(mockDataciteClient).deactivate(any(String.class));
	}

	@Test(expected = RecoverableMessageException.class)
	public void testDeactivateNotReady() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		doThrow(new NotReadyException(new AsynchronousJobStatus())).when(mockDataciteClient).deactivate(any(String.class));
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, entityType, version);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testDeactivateServiceUnavailable() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).deactivate(any(String.class));
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, entityType, version);
	}

	@Test
	public void testDeactivateAuthorization() throws Exception{
		UserInfo testInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(new AuthorizationStatus(true, "mock"));
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, entityType, version);
		verify(mockUserManager).getUserInfo(USER_ID);
		verify(mockAuthorizationManager).canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeactivateUnauthorized() throws Exception {
		UserInfo testInfo = new UserInfo(false);
		reset(mockUserManager, mockAuthorizationManager); // Undo the convenience authorization set in @Before
		when(mockUserManager.getUserInfo(USER_ID)).thenReturn(testInfo);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoUserId() throws Exception {
		// Call under test
		doiManager.deactivateDoi( null, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoObjectId() throws Exception {
		// Call under test
		doiManager.deactivateDoi(USER_ID, null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNullObjectType() throws Exception {
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, null, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNotEntity() throws Exception {
		// Call under test
		doiManager.deactivateDoi(USER_ID, entityId, ObjectType.PRINCIPAL, version);
	}

	@Test
	public void testGetPortalUrl(){
		String expectedPortalUrl = baseUrl + DoiManagerImpl.ENTITY_URL_PREFIX + entityId + "/version/" + version;
		// Call under test
		String actual = doiManager.getLocation(entityId, entityType, version);
		assertEquals(expectedPortalUrl,actual);
	}

	@Test
	public void testGetPortalUrlNullVersion(){
		String expectedPortalUrl = baseUrl + DoiManagerImpl.ENTITY_URL_PREFIX + entityId;
		// Call under test
		String actual = doiManager.getLocation(entityId, entityType, null);
		assertEquals(expectedPortalUrl,actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPortalUrlNotEntity(){
		doiManager.getLocation(entityId, ObjectType.PRINCIPAL, version);
	}

	@Test
	public void testGenerateRequestUrl() {
		String expected = repoEndpoint + DoiManagerImpl.RESOURCE_PATH
				+ "?id=" + entityId
				+ "&objectType=" + entityType.name()
				+ "&version=" + version;

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(entityId, entityType, version));
	}

	@Test
	public void testGenerateRequestUrlNullVersion() {
		String expected = repoEndpoint + DoiManagerImpl.RESOURCE_PATH
				+ "?id=" + entityId
				+ "&objectType=" + entityType.name();

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(entityId, entityType, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGenerateRequestUrlFailOnNonentity() {
		String expected = repoEndpoint + DoiManagerImpl.RESOURCE_PATH
				+ "?id=" + entityId
				+ "&objectType=" + ObjectType.TEAM.name()
				+ "&version=" + version;

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(entityId, ObjectType.TEAM, version));
	}

	@Test
	public void testGenerateUri() {
		String expected = mockPrefix + "/" + entityId + "." + version;
		// Call under test
		String actual = doiManager.generateDoiUri(entityId, entityType, version);
		assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGenerateUriNullId() {
		// Call under test
		doiManager.generateDoiUri(null, entityType, version);
	}

	@Test
	public void testGenerateUriNullVersion() {
		String expected = mockPrefix + "/" + entityId;
		// Call under test
		String actual = doiManager.generateDoiUri(entityId, entityType, null);
		assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGenerateUriNonEntity() {
		String expected = mockPrefix + "/" + entityId;
		// Call under test
		String actual = doiManager.generateDoiUri(entityId, ObjectType.PRINCIPAL, version);
		assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGenerateUriNullType() {
		String expected = mockPrefix + "/" + entityId;
		// Call under test
		String actual = doiManager.generateDoiUri(entityId, null, version);
		assertEquals(expected, actual);
	}

	@Test
	public void testMergeMetadataAndAssociation() {
		DataciteMetadata metadata = new Doi();

		DoiCreator doiCreator = new DoiCreator();
		doiCreator.setCreatorName(author);
		metadata.setCreators(Collections.singletonList(doiCreator));
		DoiTitle doiTitle = new DoiTitle();
		doiTitle.setTitle(title);
		metadata.setTitles(Collections.singletonList(doiTitle));
		metadata.setPublicationYear(publicationYear);
		DoiResourceType doiResourceType = new DoiResourceType();
		doiResourceType.setResourceTypeGeneral(resourceTypeGeneral);
		metadata.setResourceType(doiResourceType);
		metadata.setStatus(DataciteRegistrationStatus.FINDABLE);

		DoiAssociation doi = new Doi();
		doi.setAssociationId("43210");
		doi.setObjectId(entityId);
		doi.setObjectType(entityType);
		doi.setObjectVersion(version);
		doi.setAssociatedOn(new Date());
		doi.setAssociatedBy("492123");
		doi.setUpdatedOn(new Date());
		doi.setUpdatedBy("321294");
		doi.setEtag("etag");
		doi.setDoiUri(doiUri);
		doi.setDoiUrl(doiUrl);

		//Call under test
		Doi expected = DoiManagerImpl.mergeMetadataAndAssociation(metadata, doi);
		assertEquals(doi.getAssociationId(), expected.getAssociationId());
		assertEquals(doi.getObjectId(), expected.getObjectId());
		assertEquals(doi.getObjectType(), expected.getObjectType());
		assertEquals(doi.getObjectVersion(), expected.getObjectVersion());
		assertEquals(doi.getAssociatedOn(), expected.getAssociatedOn());
		assertEquals(doi.getAssociatedBy(), expected.getAssociatedBy());
		assertEquals(doi.getUpdatedOn(), expected.getUpdatedOn());
		assertEquals(doi.getUpdatedBy(), expected.getUpdatedBy());
		assertEquals(doi.getEtag(), expected.getEtag());
		assertEquals(doi.getDoiUri(), expected.getDoiUri());
		assertEquals(doi.getDoiUrl(), expected.getDoiUrl());

		assertEquals(metadata.getCreators(), expected.getCreators());
		assertEquals(metadata.getTitles(), expected.getTitles());
		assertEquals(metadata.getResourceType(), expected.getResourceType());
		assertEquals(metadata.getPublicationYear(), expected.getPublicationYear());
		assertEquals(metadata.getStatus(), expected.getStatus());

	}

	/**
	 * Create a DTO with all fields we expect the user to enter.
	 * @return A DTO with data in all required client-specified fields.
	 */
	private Doi setUpDto(boolean withMetadata) {
		Doi dto = new Doi();

		// Object fields
		dto.setObjectId(entityId);
		dto.setObjectType(entityType);
		dto.setObjectVersion(version);

		if (withMetadata) {
			// Required metadata fields
			DoiCreator doiCreator = new DoiCreator();
			doiCreator.setCreatorName(author);
			dto.setCreators(Collections.singletonList(doiCreator));

			DoiTitle doiTitle = new DoiTitle();
			doiTitle.setTitle(title);
			dto.setTitles(Collections.singletonList(doiTitle));

			dto.setPublicationYear(publicationYear);

			DoiResourceType doiResourceType = new DoiResourceType();
			doiResourceType.setResourceTypeGeneral(resourceTypeGeneral);
			dto.setResourceType(doiResourceType);
		}
		return dto;
	}
}
