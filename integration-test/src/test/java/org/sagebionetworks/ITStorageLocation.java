package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
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
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandle;
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
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.sample.sts.MigrateS3Bucket;
import org.sagebionetworks.sample.sts.MigrateSynapseProject;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;

public class ITStorageLocation {
	private static Long userToDelete;
	private static String externalS3Bucket;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static SynapseS3Client synapseS3Client;
	private static File tmpDir;

	private Folder folder;
	private Project project;
	private List<File> filesToDelete;

	@BeforeAll
	public static void beforeClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();
		externalS3Bucket = config.getExternalS3TestBucketName();

		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();

		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);

		synapseS3Client = AwsClientFactory.createAmazonS3Client();

		// Make tmp dir.
		tmpDir = Files.createTempDir();
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		filesToDelete = new ArrayList<>();

		// Create a test project which we will need.
		project = new Project();
		project = synapse.createEntity(project);

		// Create folder, which is required for STS.
		folder = createFolder(project.getId(), "folder");
	}

	@AfterEach
	public void after() throws SynapseException {
		if (project != null) {
			synapse.deleteEntity(project, true);
		}

		for (File file : filesToDelete) {
			file.delete();
		}
	}

	@AfterAll
	public static void afterClass() {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}

		tmpDir.delete();
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

	@Test
	public void sampleCode_MigrateS3Bucket() throws Exception {
		// Create external S3 storage location with STS.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = createExternalS3StorageLocation();
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting = synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
		applyStorageLocationToFolder(externalS3StorageLocationSetting);

		// Create some test data to migrate
		// baseKey/f1
		// baseKey/f2
		// baseKey/a/f3
		// baseKey/a/f4
		// baseKey/a/b/c/f5
		// baseKey/a/b/c/f6
		String baseKey = externalS3StorageLocationSetting.getBaseKey();
		uploadToExternalS3Bucket(baseKey, "f1", "sample content 1");
		uploadToExternalS3Bucket(baseKey, "f2", "sample content 2");
		uploadToExternalS3Bucket(baseKey + "/a", "f3", "sample content 3");
		uploadToExternalS3Bucket(baseKey + "/a", "f4", "sample content 4");
		uploadToExternalS3Bucket(baseKey + "/a/b/c", "f5", "sample content 5");
		uploadToExternalS3Bucket(baseKey + "/a/b/c", "f6", "sample content 6");

		// Run sample code.
		MigrateS3Bucket migration = new MigrateS3Bucket(synapseS3Client.getUSStandardAmazonClient(), synapse,
				externalS3Bucket, baseKey, folder.getId(), externalS3StorageLocationSetting.getStorageLocationId());
		migration.execute();

		// Verify results.
		// Get files in baseKey (f1 and f2).
		Map<String, EntityHeader> childrenByName = getChildren(folder.getId(), EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f1"));
		assertTrue(childrenByName.containsKey("f2"));

		// Get folders in baseKey (a).
		childrenByName = getChildren(folder.getId(), EntityType.folder);
		assertEquals(1, childrenByName.size());
		assertTrue(childrenByName.containsKey("a"));
		String aFolderId = childrenByName.get("a").getId();

		// Get files in baseKey/a (f3 and f4).
		childrenByName = getChildren(aFolderId, EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f3"));
		assertTrue(childrenByName.containsKey("f4"));

		// Get folders in baseKey/a (b).
		childrenByName = getChildren(aFolderId, EntityType.folder);
		assertEquals(1, childrenByName.size());
		assertTrue(childrenByName.containsKey("b"));
		String bFolderId = childrenByName.get("b").getId();

		// Get files in baseKey/a/b (none).
		childrenByName = getChildren(bFolderId, EntityType.file);
		assertEquals(0, childrenByName.size());

		// Get folders in baseKey/a/b (c).
		childrenByName = getChildren(bFolderId, EntityType.folder);
		assertEquals(1, childrenByName.size());
		assertTrue(childrenByName.containsKey("c"));
		String cFolderId = childrenByName.get("c").getId();

		// Get files in baseKey/a/b/c (f5 and f6).
		childrenByName = getChildren(cFolderId, EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f5"));
		assertTrue(childrenByName.containsKey("f6"));

		// Get folders in baseKey/a/c (none).
		childrenByName = getChildren(cFolderId, EntityType.folder);
		assertEquals(0, childrenByName.size());
	}

	@Test
	public void sampleCode_MigrateSynapseProject() throws Exception {
		// Create a source folder with some data to migrate.
		// sourceRootFolder/f1
		// sourceRootFolder/f2
		// sourceRootFolder/a/f3
		// sourceRootFolder/a/f4
		// sourceRootFolder/a/b/f5
		// sourceRootFolder/a/b/f6
		Folder sourceRootFolder = createFolder(project.getId(), "sourceRootFolder");
		uploadTempFileToSynapseStorage(sourceRootFolder.getId(), "f1", "sample content 1");
		uploadTempFileToSynapseStorage(sourceRootFolder.getId(), "f2", "sample content 2");

		Folder sourceAFolder = createFolder(sourceRootFolder.getId(), "a");
		uploadTempFileToSynapseStorage(sourceAFolder.getId(), "f3", "sample content 3");
		uploadTempFileToSynapseStorage(sourceAFolder.getId(), "f4", "sample content 4");

		Folder sourceBFolder = createFolder(sourceAFolder.getId(), "b");
		uploadTempFileToSynapseStorage(sourceBFolder.getId(), "f5", "sample content 5");
		uploadTempFileToSynapseStorage(sourceBFolder.getId(), "f6", "sample content 6");

		// Create a Synapse storage location with STS.
		S3StorageLocationSetting synapseS3StorageLocationSetting = new S3StorageLocationSetting();
		synapseS3StorageLocationSetting.setStsEnabled(true);
		synapseS3StorageLocationSetting = synapse.createStorageLocationSetting(synapseS3StorageLocationSetting);
		applyStorageLocationToFolder(synapseS3StorageLocationSetting);

		// Run sample code.
		MigrateSynapseProject migration = new MigrateSynapseProject(synapse, sourceRootFolder.getId(), folder.getId(),
				synapseS3StorageLocationSetting.getStorageLocationId());
		migration.execute();

		// Verify results.
		// Get files in destinationFolder (f1 and f2).
		Map<String, EntityHeader> childrenByName = getChildren(folder.getId(), EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f1"));
		assertTrue(childrenByName.containsKey("f2"));

		// Get folders in destinationFolder (a).
		childrenByName = getChildren(folder.getId(), EntityType.folder);
		assertEquals(1, childrenByName.size());
		assertTrue(childrenByName.containsKey("a"));
		String destinationAFolderId = childrenByName.get("a").getId();

		// Get files in destinationFolder/a (f3 and f4).
		childrenByName = getChildren(destinationAFolderId, EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f3"));
		assertTrue(childrenByName.containsKey("f4"));

		// Get folders in destinationFolder/a (b).
		childrenByName = getChildren(destinationAFolderId, EntityType.folder);
		assertEquals(1, childrenByName.size());
		assertTrue(childrenByName.containsKey("b"));
		String destinationBFolderId = childrenByName.get("b").getId();

		// Get files in destinationFolder/a/b (f5 and f6).
		childrenByName = getChildren(destinationBFolderId, EntityType.file);
		assertEquals(2, childrenByName.size());
		assertTrue(childrenByName.containsKey("f5"));
		assertTrue(childrenByName.containsKey("f6"));

		// Get folders in destinationFolder/a/b (none).
		childrenByName = getChildren(destinationBFolderId, EntityType.folder);
		assertEquals(0, childrenByName.size());
	}

	@Test
	public void testBucketOwnerWithMultipleUsers() throws SynapseException {
		
		List<String> ownerIdentifiers = ImmutableList.of(
				"someotherusername",
				userToDelete.toString(),
				"1234"
		);
		
		// Create and verify storage location.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = createExternalS3StorageLocation(ownerIdentifiers);
		
		synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
	}
	
	@Test
	public void testBucketOwnerWithMultipleUsersOnSameLine() throws SynapseException {
		
		List<String> ownerIdentifiers = ImmutableList.of(
				String.join(",", "someotherusername", userToDelete.toString())
		);
		
		// Create and verify storage location.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = createExternalS3StorageLocation(ownerIdentifiers);
		
		synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
	}

	private void testStsStorageLocation(StsStorageLocationSetting stsStorageLocationSetting) throws SynapseException {
		String baseKey = stsStorageLocationSetting.getBaseKey();
		assertNotNull(baseKey);
		long storageLocationId = stsStorageLocationSetting.getStorageLocationId();

		// Create and verify project settings.
		applyStorageLocationToFolder(stsStorageLocationSetting);
		UploadDestinationListSetting projectSetting = (UploadDestinationListSetting) synapse.getProjectSetting(folder.getId(),
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

	private void applyStorageLocationToFolder(StorageLocationSetting storageLocation) throws SynapseException {
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folder.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(ImmutableList.of(storageLocation.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);
	}

	// This test method assumes responses fit in a single page. Response is a map from entity name to entity header.
	private static Map<String, EntityHeader> getChildren(String parentId, EntityType type) throws SynapseException {
		EntityChildrenRequest request = new EntityChildrenRequest();
		request.setIncludeTypes(ImmutableList.of(type));
		request.setParentId(parentId);
		List<EntityHeader> entityHeaderList = synapse.getEntityChildren(request).getPage();
		return Maps.uniqueIndex(entityHeaderList, EntityHeader::getName);
	}

	private void uploadTempFileToSynapseStorage(String parentId, String filename, String content) throws Exception {
		File file = new File(tmpDir, filename);
		Files.asCharSink(file, StandardCharsets.UTF_8).write(content);
		filesToDelete.add(file);

		FileHandle fileHandle = synapse.multipartUpload(file, null, null,
				null);

		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setName(filename);
		fileEntity.setParentId(parentId);
		synapse.createEntity(fileEntity);
	}

	private static void verifyStsUploadDestination(String expectedBaseKey, UploadDestination uploadDestination) {
		assertTrue(uploadDestination instanceof StsUploadDestination);
		StsUploadDestination stsUploadDestination = (StsUploadDestination) uploadDestination;
		assertTrue(stsUploadDestination.getStsEnabled());
		assertEquals(expectedBaseKey, stsUploadDestination.getBaseKey());
	}

	private static ExternalS3StorageLocationSetting createExternalS3StorageLocation() throws SynapseException {
		String username = synapse.getUserProfile(userToDelete.toString()).getUserName();
		
		return createExternalS3StorageLocation(ImmutableList.of(username));
	}

	private static ExternalS3StorageLocationSetting createExternalS3StorageLocation(List<String> ownerIdentifiers) {
		// Ensure owner.txt is in our S3 bucket.
		String baseKey = "integration-test/ITStorageLocation-" + UUID.randomUUID().toString();
		String ownerContent = String.join("\n", ownerIdentifiers);
		uploadToExternalS3Bucket(baseKey, "owner.txt", ownerContent);

		// Create Storage Locations w/o upload type.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(externalS3Bucket);
		externalS3StorageLocationSetting.setBaseKey(baseKey);
		return externalS3StorageLocationSetting;
	}

	private static Folder createFolder(String parentId, String name) throws SynapseException {
		Folder folder = new Folder();
		folder.setName(name);
		folder.setParentId(parentId);
		return synapse.createEntity(folder);
	}

	private static void uploadToExternalS3Bucket(String prefix, String filename, String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("text/plain");
		om.setContentEncoding("UTF-8");
		om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(prefix));
		om.setContentLength(bytes.length);
		synapseS3Client.putObject(externalS3Bucket, prefix + "/" + filename, new ByteArrayInputStream(bytes), om);
	}
}
