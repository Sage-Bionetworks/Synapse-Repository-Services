package org.sagebionetworks.repo.manager.doi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.datacite.DataciteClient;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.portals.PortalManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.repo.model.portals.Portal;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
public class DoiManagerImplTest {

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private DataciteClient mockDataciteClient;

	@Mock
	private DoiAssociationDao mockDoiDao;

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	
	@Mock
	private PortalManager mockPortalManager;
	
	@InjectMocks
	@Spy
	private DoiManagerImpl doiManager;

	private static final String baseUrl = "https://syn.org/test/";
	private static final String stack = "notprod";
	private static final String expectedRepoEndpoint = "https://repo-" + stack + "." + stack + ".sagebase.org/repo/v1";

	private String portalId;
	private DoiObjectType doiObjectType;
	
	private static String objectId = "syn584322";
	private static final String associationId = "4567";
	
	private static final Long version = 4L;
	
	private static UserInfo userInfo;
	private static Doi inputDto;
	private static Doi outputDto;

	private static final String mockPrefix = "10.1234";
	private static final String doiUri = "10.1234/someuri";
	private static final String doiUrl = baseUrl + objectId;

	private static final String title = "5 Easy Steps You Can Take To Become President (You Won't Believe #3!)";
	private static final String author = "Washington, George";
	private static final Long publicationYear = 1787L;
	private static final DoiResourceTypeGeneral resourceTypeGeneral = DoiResourceTypeGeneral.Dataset;

	@BeforeEach
	public void before() {
		
		portalId = DoiManagerImpl.SYNAPSE_PORTAL_ID;
		doiObjectType = DoiObjectType.ENTITY;
		
		userInfo = new UserInfo(false, 1234L, AuthorizationConstants.DEFAULT_REALM_ID);
		inputDto = setUpDto(true);
		outputDto = setUpDto(true);
	}

	@Test
	public void testGetAssociation() throws Exception {
		when(mockDoiDao.getDoiAssociation(portalId, objectId, doiObjectType, version)).thenReturn(outputDto);
		
		doReturn(doiUri).when(doiManager).generateDoiUri(outputDto);
		doReturn(doiUrl).when(doiManager).generateLocationRequestUrl(outputDto);
		
		// Call under test
		DoiAssociation result = doiManager.getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		assertEquals(outputDto, result);
		
		assertEquals(doiUri, result.getDoiUri());
		assertEquals(doiUrl, result.getDoiUrl());
	}
	
	@Test
	public void testGetAssociationWithNoPortalId() throws Exception {
		portalId = null;
		
		when(mockDoiDao.getDoiAssociation(DoiManagerImpl.SYNAPSE_PORTAL_ID, objectId, doiObjectType, version)).thenReturn(outputDto);
		
		doReturn(doiUri).when(doiManager).generateDoiUri(outputDto);
		doReturn(doiUrl).when(doiManager).generateLocationRequestUrl(outputDto);		
		
		// Call under test
		DoiAssociation result = doiManager.getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		assertEquals(outputDto, result);
		
		assertEquals(doiUri, result.getDoiUri());
		assertEquals(doiUrl, result.getDoiUrl());
	}

	@Test
	public void testGetAssociationNoObjectId() {
		assertEquals("The objectId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.getDoiAssociation(portalId, null, doiObjectType, version);
		}).getMessage());
	}

	@Test
	public void testGetAssociationNoObjectType() {
		assertEquals("The objectType is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.getDoiAssociation(portalId, objectId, null, version);
		}).getMessage());
	}

	@Test
	public void testGetAssociationNullObjectVersion() {
		when(mockDoiDao.getDoiAssociation(portalId, objectId, doiObjectType, null)).thenReturn(outputDto);
		
		// Call under test
		doiManager.getDoiAssociation(portalId, objectId, doiObjectType, null);
		
		verify(mockDoiDao).getDoiAssociation(portalId, objectId, doiObjectType, null);
	}

	@Test
	public void testGetDoi() throws Exception {
		
		DoiAssociation mockAssociation = setUpDto(false);
		Doi metadata = setUpDto(true);
		
		doReturn(mockAssociation).when(doiManager).getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		when(mockDataciteClient.get(any(String.class))).thenReturn(metadata);
		
		// Call under test
		Doi actualResponse = doiManager.getDoi(portalId, objectId, doiObjectType, version);
		
		assertEquals(DoiManagerImpl.mergeMetadataAndAssociation(metadata, mockAssociation), actualResponse);
	}

	@Test
	public void testGetDoiNotReadyException() throws Exception {
		DoiAssociation mockAssociation = setUpDto(false);
		when(mockDoiDao.getDoiAssociation(portalId, objectId, doiObjectType, version)).thenReturn(mockAssociation);
		when(mockDataciteClient.get(any(String.class))).thenThrow(new NotReadyException(new AsynchronousJobStatus()));
		
		assertThrows(ServiceUnavailableException.class, () -> {			
			// Call under test
			doiManager.getDoi(portalId, objectId, doiObjectType, version);
		});
	}

	@Test
	public void testCreateOrUpdateDoi() throws Exception{
				
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		
		doReturn(outputDto).when(doiManager).createOrUpdateAssociation(inputDto);
		
		doReturn(doiUri).when(doiManager).generateDoiUri(outputDto);
		doReturn(doiUrl).when(doiManager).generateLocationRequestUrl(outputDto);
		
		doReturn(outputDto).when(doiManager).createOrUpdateDataciteMetadata(inputDto);
		
		// Call under test
		Doi result = doiManager.createOrUpdateDoi(userInfo, inputDto);
		
		assertEquals(outputDto, result);
		
		assertEquals(inputDto.getDoiUri(), doiUri);
		assertEquals(inputDto.getDoiUrl(), doiUrl);
	}
	
	@Test
	public void testCreateOrUpdateDoiWithNoPortalId() throws Exception{

		inputDto.setPortalId(null);
		
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, DoiManagerImpl.SYNAPSE_PORTAL_ID, objectId, doiObjectType);
		
		doReturn(outputDto).when(doiManager).createOrUpdateAssociation(inputDto);
		
		doReturn(doiUri).when(doiManager).generateDoiUri(outputDto);
		doReturn(doiUrl).when(doiManager).generateLocationRequestUrl(outputDto);
		
		doReturn(outputDto).when(doiManager).createOrUpdateDataciteMetadata(inputDto);
		
		// Call under test
		Doi result = doiManager.createOrUpdateDoi(userInfo, inputDto);
		
		assertEquals(outputDto, result);
		
		assertEquals(inputDto.getDoiUri(), doiUri);
		assertEquals(inputDto.getDoiUrl(), doiUrl);
	}

	@Test
	public void testCreateOrUpdateDoiWithNullObjectId() throws Exception {
		inputDto.setObjectId(null);
		
		assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.createOrUpdateDoi(userInfo, inputDto);
		});
	}

	@Test
	public void testCreateOrUpdateDoiWithNullObjectType() throws Exception {
		inputDto.setObjectType(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.createOrUpdateDoi(userInfo, inputDto);
		});
	}

	@Test
	public void testCreateOrUpdateAssociationWithNonExisting() throws Exception {
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getPortalId(), inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).getDoiAssociationForUpdate(inputDto.getPortalId(), inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion());
		verify(mockDoiDao).createDoiAssociation(inputDto);
	}

	@Test
	public void testCreateOrUpdateAssociationWithExisting() throws Exception {
		DoiAssociation retrievedDto = setUpDto(false);
		retrievedDto.setEtag("matching etag");
		inputDto.setEtag("matching etag");

		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getPortalId(), inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(retrievedDto);
		// Call under test
		doiManager.createOrUpdateAssociation(inputDto);

		verify(mockDoiDao).updateDoiAssociation(inputDto);
	}

	@Test
	public void testCreateOrUpdateWithThrowRecoverableOnDuplicateKeyException() throws Exception {
		when(mockDoiDao.getDoiAssociationForUpdate(inputDto.getPortalId(), inputDto.getObjectId(), inputDto.getObjectType(), inputDto.getObjectVersion())).thenReturn(null);
		when(mockDoiDao.createDoiAssociation(inputDto)).thenThrow(new DuplicateKeyException(""));
		
		assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			doiManager.createOrUpdateAssociation(inputDto);
		});
	}

	@Test
	public void testCreateOrUpdateDataciteMetadata() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		inputDto.setPublisher("Bogus");
		
		when(mockPortalManager.getPortal(inputDto.getPortalId())).thenReturn(new Portal().setName("My Portal"));
		
		// Call under test
		doiManager.createOrUpdateDataciteMetadata(inputDto);
		
		assertEquals("My Portal", inputDto.getPublisher());
		
		verify(mockDataciteClient).registerMetadata(inputDto, doiUri);
		verify(mockDataciteClient).registerDoi(doiUri, doiUrl);
		
		verify(mockDataciteClient, never()).deactivate(any(String.class));
	}

	@Test
	public void testCreateOrUpdateDataciteMetadataWithNoUri() throws Exception {
		inputDto.setDoiUri(null);
		inputDto.setDoiUrl(doiUrl);
		
		assertEquals("The doiUri is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.createOrUpdateDataciteMetadata(inputDto);
		}).getMessage());
	}

	@Test
	public void testCreateOrUpdateDataciteMetadataWithNoUrl() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(null);
		
		assertEquals("The doiUrl is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.createOrUpdateDataciteMetadata(inputDto);
		}).getMessage());
	}
	
	@Test
	public void testCreateOrUpdateDataciteMetadataWithNoPortalId() throws Exception {
		inputDto.setPortalId(null);
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		
		assertEquals("The portalId is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.createOrUpdateDataciteMetadata(inputDto);
		}).getMessage());
	}

	@Test
	public void testCreateOrUpdateDataciteMetadataWithNotReady() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		
		when(mockPortalManager.getPortal(inputDto.getPortalId())).thenReturn(new Portal().setName("My Portal"));
		
		doThrow(new NotReadyException(new AsynchronousJobStatus())).when(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		
		assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			doiManager.createOrUpdateDataciteMetadata(inputDto);
		});
	}

	@Test
	public void testCreateOrUpdateDataciteMetadataWithServiceUnavailable() throws Exception {
		inputDto.setDoiUri(doiUri);
		inputDto.setDoiUrl(doiUrl);
		
		when(mockPortalManager.getPortal(inputDto.getPortalId())).thenReturn(new Portal().setName("My Portal"));
		
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).registerMetadata(any(DataciteMetadata.class), any(String.class));
		
		assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			doiManager.createOrUpdateDataciteMetadata(inputDto);
		});
	}

	@Test
	public void testDeactivateDoi() throws Exception {
		outputDto.setDoiUri(doiUri);
		
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		doReturn(outputDto).when(doiManager).getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		// Call under test
		doiManager.deactivateDoi(userInfo, portalId, objectId, doiObjectType, version);
		
		verify(mockDataciteClient).deactivate(doiUri);
	}
	
	@Test
	public void testDeactivateDoiWithNoPortalId() throws Exception {
		portalId = null;
		
		outputDto.setDoiUri(doiUri);
		
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, DoiManagerImpl.SYNAPSE_PORTAL_ID, objectId, doiObjectType);
		doReturn(outputDto).when(doiManager).getDoiAssociation(DoiManagerImpl.SYNAPSE_PORTAL_ID, objectId, doiObjectType, version);
		
		// Call under test
		doiManager.deactivateDoi(userInfo, DoiManagerImpl.SYNAPSE_PORTAL_ID, objectId, doiObjectType, version);
		
		verify(mockDataciteClient).deactivate(doiUri);
	}

	@Test
	public void testDeactivateDoiWithNotReady() throws Exception {
		outputDto.setDoiUri(doiUri);
		
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		doReturn(outputDto).when(doiManager).getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		doThrow(new NotReadyException(new AsynchronousJobStatus())).when(mockDataciteClient).deactivate(doiUri);
		
		assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			doiManager.deactivateDoi(userInfo, portalId, objectId, doiObjectType, version);
		});
	}

	@Test
	public void testDeactivateDoiWithServiceUnavailable() throws Exception {
		outputDto.setDoiUri(doiUri);
		
		doNothing().when(doiManager).verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		doReturn(outputDto).when(doiManager).getDoiAssociation(portalId, objectId, doiObjectType, version);
		
		doThrow(new ServiceUnavailableException()).when(mockDataciteClient).deactivate(doiUri);
		
		assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			doiManager.deactivateDoi(userInfo, portalId, objectId, doiObjectType, version);
		});
	}

	@Test
	public void testDeactivateDoiWithNoUserInfo() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.deactivateDoi( null, portalId, objectId, doiObjectType, version);
		});
	}

	@Test
	public void testDeactivateDoiWithNoObjectId() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.deactivateDoi(userInfo, portalId, null, doiObjectType, version);
		});
	}

	@Test
	public void testDeactivateDoiWithNullObjectType() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.deactivateDoi(userInfo, portalId, objectId, null, version);
		});
	}

	@Test
	public void testDeactivateDoiWithNotEntity() throws Exception {
		doiObjectType = DoiObjectType.PORTAL_RESOURCE;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.deactivateDoi(userInfo, portalId, objectId, doiObjectType, version);
		});
	}

	@Test
	public void testGetLocation() {
		when(mockConfig.getSynapseBaseUrl()).thenReturn(baseUrl);
		
		String expectedPortalUrl = baseUrl + DoiManagerImpl.ENTITY_URL_PREFIX + objectId + "/version/" + version;
		
		// Call under test
		String actual = doiManager.getLocation(portalId, objectId, doiObjectType, version);
		
		assertEquals(expectedPortalUrl, actual);
	}
	
	@Test
	public void testGetLocationWithNoPortalId() {
		when(mockConfig.getSynapseBaseUrl()).thenReturn(baseUrl);
		
		String expectedPortalUrl = baseUrl + DoiManagerImpl.ENTITY_URL_PREFIX + objectId + "/version/" + version;
		
		// Call under test
		String actual = doiManager.getLocation(null, objectId, doiObjectType, version);
		
		assertEquals(expectedPortalUrl, actual);
	}

	@Test
	public void testGetLocationWithNullVersion() {
		when(mockConfig.getSynapseBaseUrl()).thenReturn(baseUrl);
		
		String expectedPortalUrl = baseUrl + DoiManagerImpl.ENTITY_URL_PREFIX + objectId;
		// Call under test
		String actual = doiManager.getLocation(portalId, objectId, doiObjectType, null);
		assertEquals(expectedPortalUrl,actual);
	}

	@Test
	public void testGetLocationWithNotEntity(){
		doiObjectType = DoiObjectType.PORTAL_RESOURCE;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.getLocation(portalId, objectId, doiObjectType, version);
		});
	}
	
	@Test
	public void testGetLocationWithExternalPortal() {
		objectId = "DATASET.123.&";
		portalId = "456";

		when(mockPortalManager.getPortal(portalId)).thenReturn(new Portal().setUrl("https://myportal.synapse.org"));
		
		// Call under test
		String actual = doiManager.getLocation(portalId, objectId, doiObjectType, version);
		
		assertEquals("https://myportal.synapse.org/doi?objectId=DATASET.123.%26", actual);
	}

	@Test
	public void testGenerateLocationRequestUrl() {
		when(mockConfig.getStack()).thenReturn(stack);
		
		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
			+ "?portalId=" + portalId
			+ "&id=" + objectId
			+ "&type=" + doiObjectType.name()
			+ "&version=" + version;

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(inputDto));
	}

	@Test
	public void testGenerateLocationRequestUrlEncodedId() {
		String objectId = "{\"foo\":\"bar\"}";
		String encodedObjectId = "%7B%22foo%22%3A%22bar%22%7D";

		inputDto.setObjectId(objectId);
		when(mockConfig.getStack()).thenReturn(stack);

		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
				+ "?portalId=" + portalId
				+ "&id=" + encodedObjectId
				+ "&type=" + doiObjectType.name()
				+ "&version=" + version;

		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(inputDto));
	}

	@Test
	public void testGenerateLocationRequestUrlNullVersion() {
		when(mockConfig.getStack()).thenReturn(stack);
		
		String expected = expectedRepoEndpoint + DoiManagerImpl.LOCATE_RESOURCE_PATH
			+ "?portalId=" + portalId
			+ "&id=" + objectId
			+ "&type=" + doiObjectType.name();

		inputDto.setObjectVersion(null);
		
		// Call under test
		assertEquals(expected, doiManager.generateLocationRequestUrl(inputDto));
	}

	@Test
	public void testGenerateDoiUri() {
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		
		String expected = mockPrefix + "/" + objectId + "." + version;
		// Call under test
		String actual = doiManager.generateDoiUri(inputDto);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGenerateDoiUriWithNoObjectId() {
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		
		inputDto.setObjectId(null);
		
		assertEquals("The objectId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.generateDoiUri(inputDto);
		}).getMessage());
	}

	@Test
	public void testGenerateDoiUriWithNullVersion() {
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		
		String expected = mockPrefix + "/" + objectId;
		inputDto.setObjectVersion(null);
		// Call under test
		String actual = doiManager.generateDoiUri(inputDto);
		assertEquals(expected, actual);
	}

	@Test
	public void testGenerateDoiUriWithPortalResource() {
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		
		inputDto.setObjectType(DoiObjectType.PORTAL_RESOURCE);
		
		String expected = mockPrefix + "/" + associationId;
		
		// Call under test
		String actual = doiManager.generateDoiUri(inputDto);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGenerateDoiUriWithPortalResourceAndNoAssociationId() {
		when(mockConfig.getDoiPrefix()).thenReturn(mockPrefix);
		
		inputDto.setObjectType(DoiObjectType.PORTAL_RESOURCE);		
		inputDto.setAssociationId(null);
		
		assertEquals("The associationId is required.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			doiManager.generateDoiUri(inputDto);
		}).getMessage());
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
		doi.setPortalId(portalId);
		doi.setObjectId(objectId);
		doi.setObjectType(doiObjectType);
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
		assertEquals(doi.getPortalId(), expected.getPortalId());
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
	
	@Test
	public void testVerifyDoiMintingAuthorizationWithSynapsePortal() {
		when(mockAuthorizationManager.canAccess(userInfo, objectId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		doiManager.verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
	}
	
	@Test
	public void testVerifyDoiMintingAuthorizationWithSynapsePortalAndWrongObjectType() {
		doiObjectType = DoiObjectType.PORTAL_RESOURCE;
		
		assertEquals("Object must be an entity.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		}).getMessage());
	}
	
	@Test
	public void testVerifyDoiMintingAuthorizationWithExternalPortal() {
		portalId = "456";
		doiObjectType = DoiObjectType.PORTAL_RESOURCE;
		
		when(mockPortalManager.canMintDoi(userInfo, portalId)).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		doiManager.verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
	}
	
	@Test
	public void testVerifyDoiMintingAuthorizationWithExternalPortalAndWrongObjectType() {
		portalId = "456";
		doiObjectType = DoiObjectType.ENTITY;
		
		assertEquals("Object must be a portal resource.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			doiManager.verifyDoiMintingAuthorization(userInfo, portalId, objectId, doiObjectType);
		}).getMessage());
	}

	/**
	 * Create a DTO with all fields we expect the user to enter.
	 * @return A DTO with data in all required client-specified fields.
	 */
	private Doi setUpDto(boolean withMetadata) {
		Doi dto = new Doi();

		// Object fields
		dto.setAssociationId(associationId);
		dto.setPortalId(portalId);
		dto.setObjectId(objectId);
		dto.setObjectType(doiObjectType);
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
