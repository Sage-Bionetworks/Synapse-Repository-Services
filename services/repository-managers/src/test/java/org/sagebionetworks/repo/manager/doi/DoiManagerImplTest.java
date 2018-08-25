package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiManagerImplTest {

	private DoiManager doiManager;

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

	private static final String entityId = "syn584322";
	private static final ObjectType entityType = ObjectType.ENTITY;
	private static final Long version = 4L;

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

//		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetAssociationSuccess() throws Exception {
		Doi mockResponse = setUpDto(false); // Fields will be missing that the database will populate but we won't check them.
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockResponse);
		// Call under test
		DoiAssociation actualResponse = doiManager.getDoiAssociation( 12345L, entityId, entityType, version);
		assertEquals(mockResponse.getObjectId(), actualResponse.getObjectId());
		assertEquals(mockResponse.getObjectType(), actualResponse.getObjectType());
		assertEquals(mockResponse.getObjectVersion(), actualResponse.getObjectVersion());
		assertEquals(mockPrefix + "/" + mockResponse.getObjectId() + "." + mockResponse.getObjectVersion(), actualResponse.getDoiUri());
		assertEquals(doiManager.generateLocationRequestUrl(mockResponse.getObjectId(), mockResponse.getObjectType(), mockResponse.getObjectVersion()), actualResponse.getDoiUrl());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAssociationUnauthorized() {
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		// Call under test
		doiManager.getDoiAssociation( 12345L, entityId, entityType, version);
	}

	@Test(expected = NotFoundException.class)
	public void testGetAssociationNotFoundException() {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenThrow(new NotFoundException());
		// Call under test
		doiManager.getDoiAssociation( 12345L, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoUserId() {
		// Call under test
		doiManager.getDoiAssociation( null, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectId() {
		// Call under test
		doiManager.getDoiAssociation( 12345L, null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectType() {
		// Call under test
		doiManager.getDoiAssociation( 12345L, entityId, null, version);
	}

	@Test
	public void testGetDoiSuccess() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		Doi metadata = setUpDto(true);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenReturn(metadata);
		// Call under test
		Doi actualResponse = doiManager.getDoi( 12345L, entityId, entityType, version);
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

	@Test(expected = UnauthorizedException.class)
	public void testGetDoiUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		doiManager.getDoi( 12345L, entityId, entityType, version);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetDoiServiceUnavailableException() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenThrow(new ServiceUnavailableException());
		// Call under test
		doiManager.getDoi( 12345L, entityId, entityType, version);
	}

	@Test
	public void testCreateSuccess() throws Exception {
		Doi dto = setUpDto(true);
		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion())).thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(dto)).thenReturn(dto);
		doiManager.createOrUpdateDoi(12345L, dto);
		verify(mockDoiDao, times(1)).createDoiAssociation(dto);
		verify(mockDataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(1)).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, times(0)).deactivate(any(String.class));
	}

	@Test
	public void testUpdateSuccess() throws Exception {
		Doi dto = setUpDto(true);
		dto.setEtag("a matching etag");
		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()))
				.thenReturn("a matching etag");
		when(mockDoiDao.updateDoiAssociation(dto)).thenReturn(dto);
		doiManager.createOrUpdateDoi(12345L, dto);
		verify(mockDoiDao, times(1)).updateDoiAssociation(dto);
		verify(mockDataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(1)).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, times(0)).deactivate(any(String.class));

	}

	@Test
	public void testCreateAndDeactivate() throws Exception {
		Doi dto = setUpDto(true);
		dto.setStatus(DataciteRegistrationStatus.REGISTERED);
		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion())).thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(dto)).thenReturn(dto);
		doiManager.createOrUpdateDoi(12345L, dto);
		verify(mockDataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(1)).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, times(1)).deactivate(any(String.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateDoiNoUserId() throws Exception {
		Doi dto = setUpDto(true);
		// Call under test
		doiManager.createOrUpdateDoi( null, dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateDoiNoObjectId() throws Exception {
		Doi dto = setUpDto(true);
		dto.setObjectId(null);
		// Call under test
		doiManager.createOrUpdateDoi( 12345L, dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateDoiNotEntity() throws Exception {
		Doi dto = setUpDto(true);
		dto.setObjectType(ObjectType.PRINCIPAL);
		// Call under test
		doiManager.createOrUpdateDoi( 12345L, dto);
	}

	@Test
	public void testDeactivateSuccess() throws Exception {
		DoiAssociation association = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(association);
		// Call under test
		doiManager.deactivateDoi(1234L, entityId, entityType, version);
		verify(mockDataciteClient, times(1)).deactivate(any(String.class));
	}

	@Test(expected = RecoverableMessageException.class)
	public void testDeactivateRecoverableException() throws Exception {
		DoiAssociation association = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(association);
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).deactivate(any(String.class));
		// Call under test
		doiManager.deactivateDoi(1234L, entityId, entityType, version);
		verify(mockDataciteClient, times(1)).deactivate(any(String.class));
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeactivateUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(new AuthorizationStatus(false, "mock"));
		doiManager.deactivateDoi(1234L, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoUserId() throws Exception {
		// Call under test
		doiManager.deactivateDoi( null, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoObjectId() throws Exception {
		// Call under test
		doiManager.deactivateDoi( 12345L, null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNullObjectType() throws Exception {
		// Call under test
		doiManager.deactivateDoi( 12345L, entityId, null, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNotEntity() throws Exception {
		// Call under test
		doiManager.deactivateDoi( 12345L, entityId, ObjectType.PRINCIPAL, version);
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

	/*
	 * Test the expected race conditions
	 */

	/*
	 * Test the case where an object is created between the client checking for an existing object and actually creating
	 * that object. The client should retrieve an error guiding a retry. Upon retry, the object no longer exists and the
	 * client succeeds.
	 */
	@Test
	public void testDBFailedCreateWithSuccessOnRetry() throws Exception {
		Doi dto = setUpDto(false);
		DoiAssociation processedDto = setUpDto(false);
		processedDto.setAssociationId("99999");

		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()))
				.thenThrow(new NotFoundException()) // The object never gets created, so we expect this
				.thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(dto))
				.thenThrow(new DuplicateKeyException("object already created"))
				.thenReturn(processedDto); // Returning some data transfer object will indicate success.
		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (RecoverableMessageException e) {
			// As expected...now retry
		}

		// If there are no exceptions and the Datacite client calls are made once each, then the call was successful.
		doiManager.createOrUpdateDoi(1234L, dto);
		verify(mockDataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(1)).registerDoi(any(String.class), any(String.class));

	}

	/*
	 * Test the case where an object is created between the client checking for an existing object and actually creating
	 * that object. The client should retrieve an error guiding a retry. Upon retry, the client finds the new object and
	 * is thrown an exception that does not suggest a retry.
	 */
	@Test
	public void testDBFailedCreateWithTerminatingFailureOnRetry() throws Exception {
		Doi dto = setUpDto(false);

		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()))
				.thenThrow(new NotFoundException()) // The object doesn't exist the first time, so we expect this
				.thenReturn("some new etag"); // If the object exists on the retry, then an etag will actually be returned.
		when(mockDoiDao.createDoiAssociation(dto))
				.thenThrow(new DuplicateKeyException("Object has already been created"));
		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (RecoverableMessageException e) {
			// As expected...now retry
		}

		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (ConflictingUpdateException e) {
			// As expected. This should not lead to a retry
		}
	}

	/*
	 * Test the case where an object is successfully created, but the call to the external DOI provider fails.
	 * The client should retrieve an error guiding a retry. Upon retry, the new DataCite call passes, and the
	 * client observes success.
	 */
	@Test
	public void testDoiProviderFailedCreateWithSuccessOnRetry() throws Exception{
		Doi dto = setUpDto(false);
		DoiAssociation processedDto = setUpDto(false);
		processedDto.setAssociationId("99999");

		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()))
				.thenThrow(new NotFoundException()) // The object never gets created, so we expect this
				.thenThrow(new NotFoundException());
		when(mockDoiDao.createDoiAssociation(dto))
				.thenReturn(processedDto)
				.thenReturn(processedDto); // Returning some data transfer object will indicate success.

		doThrow(new ServiceUnavailableException()) // Fail the first call to DataCite, and then let it succeed
				.doNothing()
				.when(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (RecoverableMessageException e) {
			// As expected...now retry
		}

		// If there are no exceptions and the Datacite client calls are made once each, then the call was successful.
		doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
		verify(mockDataciteClient, times(2)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(2)).registerDoi(any(String.class), any(String.class));
	}

	/*
	 * Test the case where an object is successfully created, but the call to the external DOI provider fails.
	 * The client should retrieve an error guiding a retry. Upon retry, the client finds a new object and
	 * is thrown an exception that does not suggest a retry.
	 */
	@Test
	public void testDoiProviderFailedCreateWithTerminatingFailureOnRetry() throws Exception {
		Doi dto = setUpDto(false);
		DoiAssociation processedDto = setUpDto(false);
		processedDto.setAssociationId("99999");

		when(mockDoiDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion()))
				.thenThrow(new NotFoundException()) // The object never gets created, so we expect this
				.thenReturn("some etag");

		when(mockDoiDao.createDoiAssociation(dto))
				.thenReturn(processedDto); // Returning some data transfer object will indicate success.

		doThrow(new ServiceUnavailableException()) // Fail the first call to DataCite
				.when(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (RecoverableMessageException e) {
			// As expected...now retry
		}

		try {
			doiManager.createOrUpdateDoi(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dto);
			fail();
		} catch (ConflictingUpdateException e) {
			// As expected. This should not lead to a retry
		}

		verify(mockDataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient, times(1)).registerDoi(any(String.class), any(String.class));
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
