package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudClientFactory;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.ContentDispositionUtils;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.Lists;

public class IT049FileHandleTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_MS = 1000*10; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";
	private static final String LARGE_IMAGE_FILE_NAME = "LargeImage.jpg";

	private List<FileHandle> toDelete = null;
	private File imageFile;
	private File largeImageFile;
	private Project project;

	// Hard-coded dev test bucket
	private String googleCloudBucket = "dev.test.gcp-storage.sagebase.org";

	private static StackConfiguration config;
	private static SynapseGoogleCloudStorageClient googleCloudStorageClient;

	private static SynapseS3Client synapseS3Client;

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

		synapseS3Client = AwsClientFactory.createAmazonS3Client();
		if (config.getGoogleCloudEnabled()) {
			googleCloudStorageClient = SynapseGoogleCloudClientFactory.createGoogleCloudStorageClient();
		}
	}
	
	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<>();
		// Get the image file from the classpath.
		URL url = IT049FileHandleTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		URL largeImageUrl = IT049FileHandleTest.class.getClassLoader().getResource("images/"+LARGE_IMAGE_FILE_NAME);
		largeImageFile = new File(largeImageUrl.getFile().replaceAll("%20", " "));
		project = new Project();
		project = synapse.createEntity(project);
	}

	@AfterEach
	public void after() throws Exception {
		for (FileHandle handle: toDelete) {
			try {
				synapse.deleteFileHandle(handle.getId());
			} catch (SynapseNotFoundException | SynapseClientException e) {
				// Ignore exceptions
			}
		}

		synapse.deleteEntity(project, true);
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testImageFileRoundTrip() throws SynapseException, IOException, InterruptedException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);

		CloudProviderFileHandleInterface handle = synapse.multipartUpload(imageFile, null, true, false);
		toDelete.add(handle);
		System.out.println(handle);
		assertEquals("image/png", handle.getContentType());
		assertEquals(FILE_NAME, handle.getFileName());
		assertEquals(new Long(imageFile.length()), handle.getContentSize());
		assertEquals(expectedMD5, handle.getContentMd5());
		
		// Now wait for the preview to be created.
		long start = System.currentTimeMillis();
		while(handle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be created...");
			Thread.sleep(1000);
			assertTrue((System.currentTimeMillis()-start) < MAX_WAIT_MS, "Timed out waiting for a preview image to be created.");
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		// Get the preview file handle.
		S3FileHandle preview = (S3FileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		System.out.println(preview);
		toDelete.add(preview);
		
		//clear the preview and wait for it to be recreated
		synapse.clearPreview(handle.getId());
		handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		while(handle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be recreated...");
			Thread.sleep(1000);
			assertTrue((System.currentTimeMillis()-start) < MAX_WAIT_MS, "Timed out waiting for a preview image to be created.");
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		preview = (S3FileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		toDelete.add(preview);
		
		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try{
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		}catch(SynapseNotFoundException e){
			// expected.
		}
		try{
			synapse.getRawFileHandle(handle.getPreviewId());
			fail("The handle should be deleted.");
		}catch(SynapseNotFoundException e){
			// expected.
		}
	}
	
	@Test
	public void testExternalRoundTrip() throws JSONObjectAdapterException, SynapseException{
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType("text/plain");
		efh.setFileName("foo.bar");
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		assertNotNull(clone);
		toDelete.add(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getCreatedBy());
		assertNotNull(clone.getCreatedOn());
		assertNotNull(clone.getEtag());
		assertEquals(efh.getFileName(), clone.getFileName());
		assertEquals(efh.getExternalURL(), clone.getExternalURL());
		assertEquals(efh.getContentType(), clone.getContentType());
	}
	
	@Test
	public void testExternalRoundTripWithNulls() throws JSONObjectAdapterException, SynapseException{
		// Us a null name and content type
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType(null);
		efh.setFileName(null);
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		assertNotNull(clone);
		toDelete.add(clone);
		assertNotNull(clone.getId());
		assertNotNull(clone.getCreatedBy());
		assertNotNull(clone.getCreatedOn());
		assertNotNull(clone.getEtag());
		assertEquals("NOT_SET", clone.getFileName());
		assertEquals(efh.getExternalURL(), clone.getExternalURL());
		assertEquals("NOT_SET", clone.getContentType());
	}
	
	@Test
	public void testProjectSettingsCrud() throws SynapseException, IOException, InterruptedException {
		// create an project setting
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);

		ExternalStorageLocationSetting externalDestination = new ExternalStorageLocationSetting();
		externalDestination.setUploadType(UploadType.HTTPS);
		externalDestination.setUrl("https://notvalid.com");
		externalDestination.setBanner("warning, at institute");
		externalDestination.setDescription("not in synapse, this is");

		List<StorageLocationSetting> settings = synapse.getMyStorageLocationSettings();
		assertFalse(settings.contains(externalDestination));

		externalDestination = synapse.createStorageLocationSetting(externalDestination);

		settings = synapse.getMyStorageLocationSettings();
		assertTrue(settings.contains(externalDestination));
		StorageLocationSetting settingsClone = synapse.getMyStorageLocationSetting(externalDestination.getStorageLocationId());
		assertEquals(externalDestination, settingsClone);

		projectSetting.setLocations(Lists.newArrayList(externalDestination.getStorageLocationId()));
		ProjectSetting created = synapse.createProjectSetting(projectSetting);
		assertEquals(project.getId(), created.getProjectId());
		assertEquals(ProjectSettingsType.upload, created.getSettingsType());
		assertEquals(UploadDestinationListSetting.class, created.getClass());
		assertEquals(projectSetting.getLocations(), ((UploadDestinationListSetting) created).getLocations());

		ProjectSetting clone = synapse.getProjectSetting(project.getId(), ProjectSettingsType.upload);
		assertEquals(created, clone);

		UploadDestination uploadDestination = synapse.getUploadDestination(project.getId(), externalDestination.getStorageLocationId());
		assertEquals(externalDestination.getUploadType(), uploadDestination.getUploadType());
		assertEquals(externalDestination.getBanner(), uploadDestination.getBanner());
		assertEquals(externalDestination.getStorageLocationId(), uploadDestination.getStorageLocationId());

		synapse.deleteProjectSetting(created.getId());

		assertNull(synapse.getProjectSetting(project.getId(), ProjectSettingsType.upload));
	}

	@Test
	public void testProxyFileHandleRoundTrip() throws SynapseException, JSONObjectAdapterException, IOException{
		ProxyStorageLocationSettings storageLocation = new ProxyStorageLocationSettings();
		storageLocation.setSecretKey("Super secret key that must be fairly long");
		storageLocation.setProxyUrl("https://host.org");
		storageLocation.setUploadType(UploadType.SFTP);
		// create the storage location
		storageLocation = synapse.createStorageLocationSetting(storageLocation);
		
		// Create a ProxyFileHandle
		ProxyFileHandle handle = new ProxyFileHandle();
		handle.setContentMd5("0123456789abcdef0123456789abcdef");
		handle.setContentSize(123L);
		handle.setContentType("text/plain");
		handle.setFileName("barFoo.txt");
		handle.setFilePath("pathParent/pathChild");
		handle.setStorageLocationId(storageLocation.getStorageLocationId());
		handle = synapse.createExternalProxyFileHandle(handle);
		
		// get a pre-signed url for this object
		URL preSigned = synapse.getFileHandleTemporaryUrl(handle.getId());
		assertNotNull(preSigned);
		assertEquals("host.org", preSigned.getHost());
		String expectedPath = "/sftp/"+handle.getFilePath();
		assertEquals(expectedPath, preSigned.getPath());
	}
	
	@Test
	public void testProxyLocalFileHandleRoundTrip() throws SynapseException, JSONObjectAdapterException, IOException{
		ProxyStorageLocationSettings storageLocation = new ProxyStorageLocationSettings();
		storageLocation.setSecretKey("Super secret key that must be fairly long");
		storageLocation.setProxyUrl("https://host.org");
		storageLocation.setUploadType(UploadType.PROXYLOCAL);
		// create the storage location
		storageLocation = synapse.createStorageLocationSetting(storageLocation);
		
		// Create a ProxyFileHandle
		ProxyFileHandle handle = new ProxyFileHandle();
		handle.setContentMd5("0123456789abcdef0123456789abcdef");
		handle.setContentSize(123L);
		handle.setContentType("text/plain");
		handle.setFileName("barFoo.txt");
		handle.setFilePath("pathParent/pathChild");
		handle.setStorageLocationId(storageLocation.getStorageLocationId());
		handle = synapse.createExternalProxyFileHandle(handle);
		
		// get a pre-signed url for this object
		URL preSigned = synapse.getFileHandleTemporaryUrl(handle.getId());
		assertNotNull(preSigned);
		assertEquals("host.org", preSigned.getHost());
		String expectedPath = "/proxylocal/"+handle.getFilePath();
		assertEquals(expectedPath, preSigned.getPath());
	}

	@Test
	public void testExternalObjectStoreFileHandleRoundTrip() throws SynapseException {
		//create a new StorageLocationSetting
		ExternalObjectStorageLocationSetting storageLocationSetting = new ExternalObjectStorageLocationSetting();
		String bucket = "some-bucket";
		String endpoint = "https://someurl.com";
		storageLocationSetting.setBucket(bucket);
		storageLocationSetting.setEndpointUrl(endpoint);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		//make sure the StorageLocationSetting exists
		List<StorageLocationSetting> settings = synapse.getMyStorageLocationSettings();
		assertTrue(settings.contains(storageLocationSetting));

		//change the project setting to use the newly created storage location
		UploadDestinationListSetting projectUploadSetting = new UploadDestinationListSetting();
		projectUploadSetting.setProjectId(project.getId());
		projectUploadSetting.setSettingsType(ProjectSettingsType.upload);
		projectUploadSetting.setLocations(Lists.newArrayList(storageLocationSetting.getStorageLocationId()));
		UploadDestinationListSetting createdUploadSetting = (UploadDestinationListSetting) synapse.createProjectSetting(projectUploadSetting);
		assertEquals(project.getId(), createdUploadSetting.getProjectId());
		assertEquals(ProjectSettingsType.upload, createdUploadSetting.getSettingsType());
		assertEquals(projectUploadSetting.getLocations(), createdUploadSetting.getLocations());

		//retrieve a upload destination for that project
		ExternalObjectStoreUploadDestination uploadDestination = (ExternalObjectStoreUploadDestination) synapse.getDefaultUploadDestination(project.getId());
		assertNotNull(uploadDestination.getKeyPrefixUUID());
		assertEquals(endpoint, uploadDestination.getEndpointUrl());
		assertEquals(bucket, uploadDestination.getBucket());

		//create the filehandle based off of the upload destination information
		ExternalObjectStoreFileHandle fileHandle = new ExternalObjectStoreFileHandle();
		fileHandle.setFileKey(uploadDestination.getKeyPrefixUUID() + "/asdf.txt");
		fileHandle.setStorageLocationId(storageLocationSetting.getStorageLocationId());
		fileHandle.setContentMd5("0123456789abcdef0123456789abcdef");
		fileHandle.setContentSize(1234L);
		fileHandle.setContentType("text/plain");
		ExternalObjectStoreFileHandle createdFileHandle = synapse.createExternalObjectStoreFileHandle(fileHandle);

		//Assert file handle has mirrored information about the bucket and endpointUrl from the storageLocationSetting
		assertEquals(endpoint, createdFileHandle.getEndpointUrl());
		assertEquals(bucket, createdFileHandle.getBucket());

		//Assert created file handle has same metadata as the file handle given as argument
		assertEquals(fileHandle.getFileKey(), createdFileHandle.getFileKey());
		assertEquals(fileHandle.getStorageLocationId(), createdFileHandle.getStorageLocationId());
		assertEquals(fileHandle.getContentMd5(), createdFileHandle.getContentMd5());
		assertEquals(fileHandle.getContentSize(), createdFileHandle.getContentSize());
		assertEquals(fileHandle.getContentType(), createdFileHandle.getContentType());

		//make sure created file handle is same as the ones retrieved by id
		ExternalObjectStoreFileHandle retrievedByIdFileHandle = (ExternalObjectStoreFileHandle) synapse.getRawFileHandle(createdFileHandle.getId());
		assertEquals(createdFileHandle, retrievedByIdFileHandle);
	}

	@Test
	public void testExternalUploadDestinationChoice() throws SynapseException, IOException, InterruptedException {
		// create an project setting
		ExternalStorageLocationSetting externalDestination = new ExternalStorageLocationSetting();
		externalDestination.setUploadType(UploadType.SFTP);
		externalDestination.setUrl("sftp://somewhere.com");
		externalDestination.setBanner("warning, at institute");
		externalDestination.setDescription("not in synapse, this is");
		externalDestination = synapse.createStorageLocationSetting(externalDestination);

		S3StorageLocationSetting internalS3Destination = new S3StorageLocationSetting();
		internalS3Destination.setUploadType(UploadType.S3);
		internalS3Destination.setBanner("warning, not at institute");
		internalS3Destination = synapse.createStorageLocationSetting(internalS3Destination);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(Lists.newArrayList(externalDestination.getStorageLocationId(),
				internalS3Destination.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);

		UploadDestinationLocation[] uploadDestinationLocations = synapse.getUploadDestinationLocations(project.getId());
		assertEquals(2, uploadDestinationLocations.length);
		assertEquals(externalDestination.getStorageLocationId(), uploadDestinationLocations[0].getStorageLocationId());
		assertEquals(internalS3Destination.getStorageLocationId(), uploadDestinationLocations[1].getStorageLocationId());

		UploadDestination uploadDestination = synapse.getUploadDestination(project.getId(),
				uploadDestinationLocations[0].getStorageLocationId());
		assertEquals(UploadType.SFTP, uploadDestination.getUploadType());
		assertEquals(externalDestination.getStorageLocationId(), uploadDestination.getStorageLocationId());
		assertEquals(ExternalUploadDestination.class, uploadDestination.getClass());

		uploadDestination = synapse.getUploadDestination(project.getId(), uploadDestinationLocations[1].getStorageLocationId());
		assertEquals(UploadType.S3, uploadDestination.getUploadType());
		assertEquals(internalS3Destination.getStorageLocationId(), uploadDestination.getStorageLocationId());
		assertEquals(S3UploadDestination.class, uploadDestination.getClass());
	}

	@Test
	public void testCreateSFTPExternalFile() throws Exception {
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType(null);
		efh.setFileName(null);
		efh.setExternalURL("sftp://somewhere.com");
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		toDelete.add(clone);
	}

	/**
	 * This test just ensures the web-services are setup correctly.
	 * @throws SynapseException 
	 * @throws JSONObjectAdapterException 
	 */
	@Test
	public void testBulkFileDownload() throws Exception {
		
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("BulkDownloadTest");
		table = adminSynapse.createEntity(table);
		
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType("text/plain");
		efh.setFileName("foo.bar");
		efh.setExternalURL("http://google.com");
		// Save it
		ExternalFileHandle clone = synapse.createExternalFileHandle(efh);
		this.toDelete.add(clone);
		
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setFileHandleId(clone.getId());
		fha.setAssociateObjectId(table.getId());
		fha.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		
		BulkFileDownloadRequest request = new BulkFileDownloadRequest();
		request.setRequestedFiles(Arrays.asList(fha));
		
		String jobId = adminSynapse.startBulkFileDownload(request);
		BulkFileDownloadResponse respones = waitForJob(jobId);
		assertNotNull(respones);
		assertNotNull(respones.getFileSummary());
		assertEquals(1, respones.getFileSummary().size());
		FileDownloadSummary summary = respones.getFileSummary().get(0);
		// should fail since we attempted to include an external file handle.
		assertEquals(FileDownloadStatus.FAILURE, summary.getStatus());
		// Result file handle should be null since it failed to do the one file
		assertEquals(null, respones.getResultZipFileHandleId());
	}
	
	/**
	 * Wait for a bulk download job to finish.
	 * @param jobId
	 * @return
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	private BulkFileDownloadResponse waitForJob(String jobId) throws SynapseException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				return adminSynapse.getBulkFileDownloadResults(jobId);
			} catch (SynapseResultNotReadyException e) {
				System.out.println("Waiting for job: "+e.getJobStatus());
			}
			assertTrue(System.currentTimeMillis() - start < MAX_WAIT_MS, "Timed out waiting for bulk download job.");
			Thread.sleep(2000);
		}
	}
	
	@Test
	public void testMultipartUploadV2() throws FileNotFoundException, SynapseException, IOException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// upload the little image using multi-part upload
		Long storageLocationId = null;
		Boolean generatePreview = false;
		Boolean forceRestart = null;
		CloudProviderFileHandleInterface result = synapse.multipartUpload(this.imageFile, storageLocationId, generatePreview, forceRestart);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
		assertEquals(expectedMD5, result.getContentMd5());
	}
	
	/**
	 * Validate that a forceRestart=true actually restarts the upload.s
	 * @throws FileNotFoundException
	 * @throws SynapseException
	 * @throws IOException
	 */
	@Test
	public void testMultipartUploadV2Reset() throws FileNotFoundException, SynapseException, IOException{
		Boolean forceRestart = false;
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setContentMD5Hex("47f208f98d738d5ff3330f4a0b358788");
		request.setContentType("plain/text");
		request.setFileName("foo.txt");
		request.setFileSizeBytes(1L);
		request.setGeneratePreview(false);
		request.setStorageLocationId(null);
		request.setPartSizeBytes((long) (5*1024*1024));
		// start this job once.
		MultipartUploadStatus startStatus = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(startStatus);
		assertNotNull(startStatus.getUploadId());
		// Starting again should yield the same job
		MultipartUploadStatus statusAgain = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(statusAgain);
		assertEquals(startStatus.getUploadId(), statusAgain.getUploadId());
		// now a force restart should yield a new id.
		forceRestart = true;
		statusAgain = synapse.startMultipartUpload(request, forceRestart);
		assertNotNull(statusAgain);
		assertFalse(startStatus.getUploadId().equals(statusAgain.getUploadId()));
	}

	@Test
	public void testCreateExternalS3FileHandleFromExistingFile() throws Exception {
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String md5 = MD5ChecksumHelper.getMD5Checksum(imageFile);

		// Upload the owner.txt to S3 so we can create the external storage location
		String baseKey = "integration-test/IT049FileHandleTest/testCreateExternalS3FileHandleFromExistingFile/" + UUID.randomUUID().toString();
		uploadOwnerTxtToS3(config.getS3Bucket(), baseKey, synapse.getUserProfile(userToDelete.toString()).getUserName());
		String key = baseKey + "/" + FILE_NAME;

		// upload the little image to S3, but not through Synapse
		putToS3WaitForConsistency(config.getS3Bucket(), key, imageFile);

		// Create the storage location setting
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(config.getS3Bucket());
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		S3FileHandle fh = new S3FileHandle();
		fh.setStorageLocationId(storageLocationSetting.getStorageLocationId());
		fh.setBucketName(config.getS3Bucket());
		fh.setKey(key);
		fh.setContentMd5(md5);

		// Method under test
		S3FileHandle result = synapse.createExternalS3FileHandle(fh);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
	}

	// See PLFM-5769
	@Test
	public void testMultipartUploadToExternalS3() throws SynapseException, IOException, InterruptedException {
		assertNotNull(largeImageFile);
		assertTrue(largeImageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(largeImageFile);

		// Upload the owner.txt to S3 so we can create the external storage location
		String baseKey = "integration-test/IT049FileHandleTest/testMultipartUploadToExternalS3/" + UUID.randomUUID().toString();

		uploadOwnerTxtToS3(config.getS3Bucket(), baseKey, synapse.getUserProfile(userToDelete.toString()).getUserName());

		// upload the little image using multi-part upload
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(config.getS3Bucket());
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		Boolean generatePreview = false;
		Boolean forceRestart = null;
		S3FileHandle result = (S3FileHandle) synapse.multipartUpload(this.largeImageFile, storageLocationSetting.getStorageLocationId(), generatePreview, forceRestart);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
		assertEquals(expectedMD5, result.getContentMd5());
	}

	@Test
	public void testMultipartUploadV2ToGoogleCloud() throws SynapseException, IOException, InterruptedException {
		// Only run this test if Google Cloud is enabled.
		Assumptions.assumeTrue(config.getGoogleCloudEnabled());
		// We use the large image file to force an upload with more than one part. required to test part deletion
		assertNotNull(largeImageFile);
		assertTrue(largeImageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(largeImageFile);

		// Upload the owner.txt to Google Cloud so we can create the storage location
		String baseKey = "integration-test/IT049FileHandleTest/testMultipartUploadV2ToGoogleCloud/" + UUID.randomUUID().toString();

		uploadOwnerTxtToGoogleCloud(googleCloudBucket, baseKey, synapse.getUserProfile(userToDelete.toString()).getUserName());

		// upload the little image using multi-part upload
		ExternalGoogleCloudStorageLocationSetting storageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		storageLocationSetting.setBucket(googleCloudBucket);
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		Boolean generatePreview = false;
		Boolean forceRestart = null;
		GoogleCloudFileHandle result = (GoogleCloudFileHandle) synapse.multipartUpload(this.largeImageFile, storageLocationSetting.getStorageLocationId(), generatePreview, forceRestart);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
		assertEquals(expectedMD5, result.getContentMd5());

		// Verify that all parts have been deleted
		assertTrue(IterableUtils.isEmpty(googleCloudStorageClient.getObjects(result.getBucketName(), result.getKey() + "/")));
	}

	public void testCreateExternalGoogleCloudFileHandleFromExistingFile() throws Exception {
		// Only run this test if Google Cloud is enabled.
		Assumptions.assumeTrue(config.getGoogleCloudEnabled());

		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String md5 = MD5ChecksumHelper.getMD5Checksum(imageFile);

		// Upload the owner.txt to Google Cloud so we can create the external storage location
		String baseKey = "integration-test/IT049FileHandleTest/testCreateExternalGoogleCloudFileHandleFromExistingFile/" + UUID.randomUUID().toString();
		uploadOwnerTxtToGoogleCloud(googleCloudBucket, baseKey, synapse.getUserProfile(userToDelete.toString()).getUserName());

		String key = baseKey + "/" + FILE_NAME;
		// upload the little image to Google Cloud, but not through Synapse
		putToGoogleCloudWaitForConsistency(googleCloudBucket, key, imageFile);

		// Create the storage location setting
		ExternalGoogleCloudStorageLocationSetting storageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		storageLocationSetting.setBucket(googleCloudBucket);
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		GoogleCloudFileHandle fh = new GoogleCloudFileHandle();
		fh.setStorageLocationId(storageLocationSetting.getStorageLocationId());
		fh.setBucketName(googleCloudBucket);
		fh.setKey(key);
		fh.setContentMd5(md5);

		// Method under test
		GoogleCloudFileHandle result = synapse.createExternalGoogleCloudFileHandle(fh);
		assertNotNull(result);
		toDelete.add(result);
		assertNotNull(result.getFileName());
	}

	private static void putToS3WaitForConsistency(String bucket, String key, File file) throws InterruptedException {
		synapseS3Client.putObject(bucket, key, file);
		long start = System.currentTimeMillis();
		long waitTimeMillis = 100;
		while (!synapseS3Client.doesObjectExist(bucket, key) && (System.currentTimeMillis()-start) < MAX_WAIT_MS) {
			Thread.sleep(waitTimeMillis);
			waitTimeMillis *= 2;
		}
		assertTrue(synapseS3Client.doesObjectExist(bucket, key), "Failed to create " + key + " in bucket " + bucket);
	}

	private static void putToS3WaitForConsistency(String bucket, String key, InputStream contents, ObjectMetadata metadata) throws InterruptedException {
		synapseS3Client.putObject(bucket, key, contents, metadata);
		long start = System.currentTimeMillis();
		long waitTimeMillis = 100;
		while (!synapseS3Client.doesObjectExist(bucket, key) && (System.currentTimeMillis()-start) < MAX_WAIT_MS) {
			Thread.sleep(waitTimeMillis);
			waitTimeMillis *= 2;
		}
		assertTrue(synapseS3Client.doesObjectExist(bucket, key), "Failed to create " + key + " in S3 bucket " + bucket);
	}

	private static void putToGoogleCloudWaitForConsistency(String bucket, String key, File contents) throws InterruptedException, IOException {
		googleCloudStorageClient.putObject(bucket, key, contents);
		long start = System.currentTimeMillis();
		long waitTimeMillis = 100;
		boolean objectExists = false;
		while (!objectExists && (System.currentTimeMillis()-start) < MAX_WAIT_MS) {
			try {
				googleCloudStorageClient.getObject(bucket, key);
				objectExists = true;
			} catch (StorageException e) {
				assertEquals(404, e.getCode(), "Unknown Google Cloud Storage error: " + e.getMessage());
			}
			Thread.sleep(waitTimeMillis);
			waitTimeMillis *= 2;
		}
		assertTrue(objectExists, "Failed to create " + key + " in Google Cloud bucket " + bucket);
	}

	private static void putToGoogleCloudWaitForConsistency(String bucket, String key, InputStream contents) throws InterruptedException, IOException {
		googleCloudStorageClient.putObject(bucket, key, contents);
		long start = System.currentTimeMillis();
		long waitTimeMillis = 100;
		boolean objectExists = false;
		while (!objectExists && (System.currentTimeMillis()-start) < MAX_WAIT_MS) {
			try {
				googleCloudStorageClient.getObject(bucket, key);
				objectExists = true;
			} catch (StorageException e) {
				assertEquals(404, e.getCode(), "Unknown Google Cloud Storage error: " + e.getMessage());
			}
			Thread.sleep(waitTimeMillis);
			waitTimeMillis *= 2;
		}
		assertTrue(objectExists, "Failed to create " + key + " in Google Cloud bucket " + bucket);
	}

	private static void uploadOwnerTxtToS3(String bucket, String baseKey, String username) throws InterruptedException {
		byte[] bytes = username.getBytes(StandardCharsets.UTF_8);

		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("text/plain");
		om.setContentEncoding("UTF-8");
		om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(baseKey));
		om.setContentLength(bytes.length);
		putToS3WaitForConsistency(bucket, baseKey + "/owner.txt", new ByteArrayInputStream(bytes), om);
	}

	private static void uploadOwnerTxtToGoogleCloud(String bucket, String baseKey, String username) throws InterruptedException, IOException {
		// The Google Cloud service account must have write access to the bucket for this call to succeed
		putToGoogleCloudWaitForConsistency(bucket, baseKey + "/owner.txt", new ByteArrayInputStream(username.getBytes(StandardCharsets.UTF_8)));
	}
}
