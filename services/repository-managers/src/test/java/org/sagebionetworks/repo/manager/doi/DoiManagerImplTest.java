package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.dao.DuplicateKeyException;
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
	private AuthorizationManager mockAuthorizationManager;

	private static final String baseUrl = "https://syn.org/test/";
	private static final String stack = "notprod";
	private static final String expectedRepoEndpoint = "https://repo-" + stack + "." + stack + ".sagebase.org/repo/v1";

	private static final UserInfo adminInfo = new UserInfo(true);
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
	private static final String orcid = "123-456-0000";

	@Before
	public void before() {
		adminInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		doiManager = new DoiManagerImpl();
		ReflectionTestUtils.setField(doiManager, "stackConfiguration", mockConfig);
		when(mockConfig.getSynapseBaseUrl()).thenReturn(baseUrl);
		when(mockConfig.getStack()).thenReturn(stack);
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		ReflectionTestUtils.setField(doiManager, "dataciteClient", mockDataciteClient);
		ReflectionTestUtils.setField(doiManager, "doiAssociationDao", mockDoiDao);
		ReflectionTestUtils.setField(doiManager, "authorizationManager", mockAuthorizationManager);
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());
		inputDto = setUpDto(true);
		outputDto = setUpDto(true);
	}

	@Test
	public void testGetAssociationSuccess() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		DoiAssociation actualResponse = doiManager.getDoiAssociation(entityId, entityType, version);
		verify(mockDoiDao).getDoiAssociation(entityId, entityType, version);
		assertEquals(outputDto.getObjectId(), actualResponse.getObjectId());
		assertEquals(outputDto.getObjectType(), actualResponse.getObjectType());
		assertEquals(outputDto.getObjectVersion(), actualResponse.getObjectVersion());
		assertEquals(mockPrefix + "/" + outputDto.getObjectId() + "." + outputDto.getObjectVersion(), actualResponse.getDoiUri());
		assertEquals(doiManager.generateLocationRequestUrl(outputDto.getObjectId(), outputDto.getObjectType(), outputDto.getObjectVersion()), actualResponse.getDoiUrl());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectId() {
		// Call under test
		doiManager.getDoiAssociation(null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAssociationNoObjectType() {
		// Call under test
		doiManager.getDoiAssociation(entityId, null, version);
	}

	@Test
	public void testGetAssociationNullObjectVersion() {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, null)).thenReturn(outputDto);
		// Call under test
		doiManager.getDoiAssociation(entityId, entityType, null);
		verify(mockDoiDao).getDoiAssociation(entityId, entityType, null);
	}


	@Test
	public void testGetDoiSuccess() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		Doi metadata = setUpDto(true);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenReturn(metadata);
		// Call under test
		Doi actualResponse = doiManager.getDoi(entityId, entityType, version);
		assertEquals(mockAssociation.getObjectId(), actualResponse.getObjectId());
		assertEquals(mockAssociation.getObjectType(), actualResponse.getObjectType());
		assertEquals(mockAssociation.getObjectVersion(), actualResponse.getObjectVersion());
		assertEquals(mockPrefix + "/" + mockAssociation.getObjectId() + "." + mockAssociation.getObjectVersion(), actualResponse.getDoiUri());
		assertEquals(doiManager.generateLocationRequestUrl(mockAssociation.getObjectId(), mockAssociation.getObjectType(), mockAssociation.getObjectVersion()), actualResponse.getDoiUrl());

		assertEquals(actualResponse.getCreators(), metadata.getCreators());
		assertEquals(actualResponse.getTitles(), metadata.getTitles());
		assertEquals(actualResponse.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(actualResponse.getResourceType(), metadata.getResourceType());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetDoiNotReadyException() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenThrow(new NotReadyException(new AsynchronousJobStatus()));
		// Call under test
		doiManager.getDoi(entityId, entityType, version);
	}

	@Test
	public void testCreateOrUpdateAuthorization() throws Exception{
		UserInfo testInfo = new UserInfo(false);
		testInfo.setId(12345L); // Arbitrary user ID, doesn't matter, just need to avoid a NPE
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		// The following mocks are necessary for the rest of the method to succeed on a "create"
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);
		// Call under test
		doiManager.createOrUpdateDoi(testInfo, inputDto);
		verify(mockAuthorizationManager).canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testCreateOrUpdateUnauthorized() throws Exception {
		UserInfo testInfo = new UserInfo(false);
		// Undo the convenience authorization set in @Before
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied("mock"));
		// Call under test
		doiManager.createOrUpdateDoi(testInfo, inputDto);
	}

	@Test
	public void testCreateSuccess() throws Exception {
		// Test the entire create call
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);

		doiManager.createOrUpdateDoi(adminInfo, inputDto);
		assertEquals(inputDto.getUpdatedOn(), inputDto.getAssociatedOn());
		assertNotNull(inputDto.getEtag());
		assertEquals(adminInfo.getId().toString(), inputDto.getAssociatedBy());
		assertEquals(adminInfo.getId().toString(), inputDto.getUpdatedBy());
		verify(mockDoiDao).createDoiAssociation(inputDto);
		verify(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test
	public void testUpdateSuccess() throws Exception {
		Doi dtoToUpdate = setUpDto(true);
		String existingEtag = "an etag";
		dtoToUpdate.setAssociatedOn(new Timestamp(0));
		dtoToUpdate.setEtag(existingEtag);
		when(mockDoiDao.getDoiAssociationForUpdate(dtoToUpdate.getObjectId(), dtoToUpdate.getObjectType(), dtoToUpdate.getObjectVersion())).thenReturn(dtoToUpdate);
		// Test the entire update call
		when(mockDoiDao.updateDoiAssociation(dtoToUpdate)).thenReturn(dtoToUpdate);
		when(mockDataciteClient.get(any(String.class))).thenReturn(dtoToUpdate);
		// Call under test
		doiManager.createOrUpdateDoi(adminInfo, dtoToUpdate);
		assertNotEquals(existingEtag, dtoToUpdate.getEtag());
		assertNotEquals(dtoToUpdate.getUpdatedOn(), dtoToUpdate.getAssociatedOn());
		verify(mockDoiDao).updateDoiAssociation(dtoToUpdate);
		verify(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(mockDataciteClient).registerDoi(any(String.class), any(String.class));
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNullObjectId() throws Exception {
		inputDto.setObjectId(null);
		// Call under test
		doiManager.createOrUpdateDoi(adminInfo, inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNullObjectType() throws Exception {
		inputDto.setObjectType(null);
		// Call under test
		doiManager.createOrUpdateDoi(adminInfo, inputDto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateOrUpdateNonEntityObjectType() throws Exception {
		inputDto.setObjectType(ObjectType.PRINCIPAL);
		// Call under test
		doiManager.createOrUpdateDoi(adminInfo, inputDto);
	}

	@Test
	public void testCreateOrUpdateNullObjectVersion() throws Exception {
		inputDto.setObjectVersion(null);
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		when(mockDoiDao.createDoiAssociation(inputDto)).thenReturn(outputDto);
		when(mockDataciteClient.get(any(String.class))).thenReturn(outputDto);
		// Call under test
		doiManager.createOrUpdateDoi(adminInfo, inputDto);
		verify(mockDoiDao).createDoiAssociation(inputDto);
	}

	@Test
	public void testCreateAssociation() throws Exception {
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion());
		verify(mockDoiDao).createDoiAssociation(inputDto);
	}

	@Test
	public void testUpdateAssociation() throws Exception {
		DoiAssociation retrievedDto = setUpDto(false);
		retrievedDto.setEtag("matching etag");
		inputDto.setEtag("matching etag");

		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(retrievedDto);
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).updateDoiAssociation(inputDto);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testThrowRecoverableOnDuplicateKeyException() throws Exception {
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		when(mockDoiDao.createDoiAssociation(inputDto)).thenThrow(new DuplicateKeyException(""));
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);
	}

	@Test
	public void testCreateOrUpdateMetadata() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
		verify(mockDataciteClient).registerMetadata(inputDto, doiUri);
		verify(mockDataciteClient).registerDoi(doiUri, doiUrl);
		verify(mockDataciteClient, never()).deactivate(any(String.class));
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
		doiManager.deactivateDoi(adminInfo, entityId, entityType, version);
		verify(mockDataciteClient).deactivate(any(String.class));
	}

	@Test(expected = RecoverableMessageException.class)
	public void testDeactivateNotReady() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		doThrow(new NotReadyException(new AsynchronousJobStatus())).when(mockDataciteClient).deactivate(any(String.class));
		// Call under test
		doiManager.deactivateDoi(adminInfo, entityId, entityType, version);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testDeactivateServiceUnavailable() throws Exception {
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).deactivate(any(String.class));
		// Call under test
		doiManager.deactivateDoi(adminInfo, entityId, entityType, version);
	}

	@Test
	public void testDeactivateAuthorization() throws Exception{
		UserInfo testInfo = new UserInfo(false);
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockDoiDao.getDoiAssociation(entityId, entityType, version)).thenReturn(outputDto);
		// Call under test
		doiManager.deactivateDoi(testInfo, entityId, entityType, version);
		verify(mockAuthorizationManager).canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeactivateUnauthorized() throws Exception {
		UserInfo testInfo = new UserInfo(false);
		// Undo the convenience authorization set in @Before
		when(mockAuthorizationManager.canAccess(testInfo, entityId, entityType, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied("mock"));
		// Call under test
		doiManager.deactivateDoi(testInfo, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoUserInfo() throws Exception {
		// Call under test
		doiManager.deactivateDoi( null, entityId, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNoObjectId() throws Exception {
		// Call under test
		doiManager.deactivateDoi(adminInfo, null, entityType, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNullObjectType() throws Exception {
		// Call under test
		doiManager.deactivateDoi(adminInfo, entityId, null, version);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeactivateDoiNotEntity() throws Exception {
		// Call under test
		doiManager.deactivateDoi(adminInfo, entityId, ObjectType.PRINCIPAL, version);
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
		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
				+ "?id=" + entityId
				+ "&type=" + entityType.name()
				+ "&version=" + version;

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(entityId, entityType, version));
	}

	@Test
	public void testGenerateRequestUrlNullVersion() {
		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
				+ "?id=" + entityId
				+ "&type=" + entityType.name();

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(entityId, entityType, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGenerateRequestUrlFailOnNonentity() {
		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
				+ "?id=" + entityId
				+ "&type=" + ObjectType.TEAM.name()
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
		dto.setDoiUri(doiUri);

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
