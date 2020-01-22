package org.sagebionetworks;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.StsUploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.util.ContentDispositionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ITStorageLocation {
	private static Long userToDelete;
	private static Long secondUser;
	private static StackConfiguration config;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static SynapseS3Client synapseS3Client;

	private Folder folder;
	private Project project;

	@BeforeAll
	public static void beforeClass() throws Exception {
		config = StackConfigurationSingleton.singleton();
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();

		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);

		SynapseClient secondSynapseClient = new SynapseAdminClientImpl();
		secondUser = SynapseClientHelper.createUser(adminSynapse, secondSynapseClient);

		synapseS3Client = AwsClientFactory.createAmazonS3Client();
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();

		// Create a test project which we will need.
		project = new Project();
		project = synapse.createEntity(project);

		// Create folder, which is required for STS.
		folder = new Folder();
		folder.setParentId(project.getId());
		folder = synapse.createEntity(folder);
	}

	@AfterEach
	public void after() throws SynapseException {
		if (project != null) {
			synapse.deleteEntity(project, true);
		}
	}

	@AfterAll
	public static void afterClass() {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}

		try {
			adminSynapse.deleteUser(secondUser);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}
	}

	// PFLM-5985 - Creating storage locations with UploadType=null, associating them via project setting to a project, and
	// then calling getUploadDestinationLocations will result in a NullPointerException. This is now fixed, and this test
	// verifies the fix.
	@Test
	public void testDefaultUploadType() throws SynapseException {
		// Create Storage Locations w/o upload type.
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting =
				new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket("some-bucket");
		externalObjectStorageLocationSetting.setEndpointUrl("https://someurl.com");
		externalObjectStorageLocationSetting.setUploadType(null);
		externalObjectStorageLocationSetting = synapse.createStorageLocationSetting(
				externalObjectStorageLocationSetting);
		assertEquals(UploadType.NONE, externalObjectStorageLocationSetting.getUploadType());

		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = createExternalS3StorageLocation();
		externalS3StorageLocationSetting.setUploadType(null);
		externalS3StorageLocationSetting = synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
		assertEquals(UploadType.S3, externalS3StorageLocationSetting.getUploadType());

		ExternalStorageLocationSetting externalStorageLocationSetting = new ExternalStorageLocationSetting();
		externalStorageLocationSetting.setUrl("sftp://somewhere.com");
		externalStorageLocationSetting.setUploadType(null);
		externalStorageLocationSetting = synapse.createStorageLocationSetting(externalStorageLocationSetting);
		assertEquals(UploadType.NONE, externalStorageLocationSetting.getUploadType());

		ProxyStorageLocationSettings proxyStorageLocationSetting = new ProxyStorageLocationSettings();
		proxyStorageLocationSetting.setSecretKey("Super secret key that must be fairly long");
		proxyStorageLocationSetting.setProxyUrl("https://host.org");
		proxyStorageLocationSetting.setUploadType(null);
		proxyStorageLocationSetting = synapse.createStorageLocationSetting(proxyStorageLocationSetting);
		assertEquals(UploadType.NONE, proxyStorageLocationSetting.getUploadType());

		S3StorageLocationSetting synapseS3StorageLocationSetting = new S3StorageLocationSetting();
		synapseS3StorageLocationSetting.setUploadType(null);
		synapseS3StorageLocationSetting = synapse.createStorageLocationSetting(synapseS3StorageLocationSetting);
		assertEquals(UploadType.S3, synapseS3StorageLocationSetting.getUploadType());

		// Assign these storage locations as a project setting.
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(ImmutableList.of(
				externalObjectStorageLocationSetting.getStorageLocationId(),
				externalS3StorageLocationSetting.getStorageLocationId(),
				externalStorageLocationSetting.getStorageLocationId(),
				proxyStorageLocationSetting.getStorageLocationId(),
				synapseS3StorageLocationSetting.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);

		// Get Upload Destination Locations.
		UploadDestinationLocation[] uploadDestinationLocations = synapse.getUploadDestinationLocations(
				project.getId());
		assertEquals(5, uploadDestinationLocations.length);
		assertEquals(externalObjectStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[0]
				.getStorageLocationId());
		assertEquals(externalS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[1]
				.getStorageLocationId());
		assertEquals(externalStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[2]
				.getStorageLocationId());
		assertEquals(proxyStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[3]
				.getStorageLocationId());
		assertEquals(synapseS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[4]
				.getStorageLocationId());
	}

	@Test
	public void externalS3StorageLocationWithSts() throws SynapseException {
		// Create and verify storage location.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = createExternalS3StorageLocation();
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting = synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
		assertTrue(externalS3StorageLocationSetting.getStsEnabled());

		externalS3StorageLocationSetting = synapse.getMyStorageLocationSetting(externalS3StorageLocationSetting
				.getStorageLocationId());
		assertTrue(externalS3StorageLocationSetting.getStsEnabled());

		testStsStorageLocation(externalS3StorageLocationSetting);
	}

	@Test
	public void synapseS3StorageLocationWithSts() throws SynapseException {
		// Create and verify storage location.
		S3StorageLocationSetting synapseS3StorageLocationSetting = new S3StorageLocationSetting();
		synapseS3StorageLocationSetting.setStsEnabled(true);
		synapseS3StorageLocationSetting = synapse.createStorageLocationSetting(synapseS3StorageLocationSetting);
		assertTrue(synapseS3StorageLocationSetting.getStsEnabled());
		String baseKey = synapseS3StorageLocationSetting.getBaseKey();
		assertNotNull(baseKey);
		assertFalse(baseKey.isEmpty());

		synapseS3StorageLocationSetting = synapse.getMyStorageLocationSetting(synapseS3StorageLocationSetting
				.getStorageLocationId());
		assertTrue(synapseS3StorageLocationSetting.getStsEnabled());
		assertEquals(baseKey, synapseS3StorageLocationSetting.getBaseKey());

		testStsStorageLocation(synapseS3StorageLocationSetting);
	}

	private void testStsStorageLocation(StsStorageLocationSetting stsStorageLocationSetting) throws SynapseException {
		String baseKey = stsStorageLocationSetting.getBaseKey();
		assertNotNull(baseKey);
		long storageLocationId = stsStorageLocationSetting.getStorageLocationId();

		// Create and verify project settings.
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folder.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(ImmutableList.of(storageLocationId));
		synapse.createProjectSetting(projectSetting);

		projectSetting = (UploadDestinationListSetting) synapse.getProjectSetting(folder.getId(),
				ProjectSettingsType.upload);
		assertEquals(ImmutableList.of(storageLocationId), projectSetting.getLocations());

		// Get upload destinations.
		UploadDestination uploadDestination = synapse.getDefaultUploadDestination(folder.getId());
		verifyStsUploadDestination(baseKey, uploadDestination);

		uploadDestination = synapse.getUploadDestination(folder.getId(), storageLocationId);
		verifyStsUploadDestination(baseKey, uploadDestination);

		UploadDestinationLocation[] uploadDestinationLocationArray = synapse.getUploadDestinationLocations(
				folder.getId());
		assertEquals(1, uploadDestinationLocationArray.length);
		assertEquals(storageLocationId, uploadDestinationLocationArray[0].getStorageLocationId());
	}

	private static void verifyStsUploadDestination(String expectedBaseKey, UploadDestination uploadDestination) {
		assertTrue(uploadDestination instanceof StsUploadDestination);
		StsUploadDestination stsUploadDestination = (StsUploadDestination) uploadDestination;
		assertTrue(stsUploadDestination.getStsEnabled());
		assertEquals(expectedBaseKey, stsUploadDestination.getBaseKey());
	}

	private static ExternalS3StorageLocationSetting createExternalS3StorageLocation() throws SynapseException {
		// Ensure owner.txt is in our S3 bucket.
		String username = synapse.getUserProfile(userToDelete.toString()).getUserName();
		byte[] bytes = username.getBytes(StandardCharsets.UTF_8);

		String baseKey = "integration-test/ITStorageLocation-" + UUID.randomUUID().toString();
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("text/plain");
		om.setContentEncoding("UTF-8");
		om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(baseKey));
		om.setContentLength(bytes.length);
		synapseS3Client.putObject(config.getS3Bucket(), baseKey + "/owner.txt",
				new ByteArrayInputStream(bytes), om);

		// Create Storage Locations w/o upload type.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(config.getS3Bucket());
		externalS3StorageLocationSetting.setBaseKey(baseKey);
		return externalS3StorageLocationSetting;
	}
}
