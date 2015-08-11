package org.sagebionetworks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileCopyResult;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.MD5ChecksumHelper;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.IOUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class IT054FileEntityTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_MS = 1000*10; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";

	private static AmazonS3Client s3Client;
	private File imageFile;
	private S3FileHandle fileHandle;
	private String baseKey;
	private Project project;
	private List<String> fileHandlesToDelete = Lists.newArrayList();
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		s3Client = new AmazonS3Client(new BasicAWSCredentials(StackConfiguration.getIAMUserId(), StackConfiguration.getIAMUserKey()));
		s3Client.createBucket(StackConfiguration.singleton().getExternalS3TestBucketName());
	}
	
	@Before
	public void before() throws SynapseException {
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
		// Get the image file from the classpath.
		URL url = IT054FileEntityTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		// Create the image file handle
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list, project.getId());
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		fileHandle = (S3FileHandle) results.getList().get(0);
		fileHandlesToDelete.add(fileHandle.getId());
		baseKey = UUID.randomUUID().toString() + '/';
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
		for (String handle : fileHandlesToDelete) {
			try {
				synapse.deleteFileHandle(handle);
			} catch (Exception e) {}
		}
		S3TestUtils.doDeleteAfter(s3Client);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testFileEntityRoundTrip() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException{
		// Before we start the test wait for the preview to be created
		PreviewFileHandle previewFileHandle = waitForPreviewToBeCreated(fileHandle);
		// Now create a FileEntity
		FileEntity file = new FileEntity();
		file.setName("IT054FileEntityTest.testFileEntityRoundTrip");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		// Create it
		file = synapse.createEntity(file);
		assertNotNull(file);
		// Get the file handles
		FileHandleResults fhr = synapse.getEntityFileHandlesForCurrentVersion(file.getId());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(fileHandle.getId(), fhr.getList().get(0).getId());
		assertEquals(previewFileHandle.getId(), fhr.getList().get(1).getId());
		// Repeat the test for version
		fhr = synapse.getEntityFileHandlesForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(fileHandle.getId(), fhr.getList().get(0).getId());
		assertEquals(previewFileHandle.getId(), fhr.getList().get(1).getId());

		// Make sure we can get the URLs for this file
		URL tempUrl = synapse.getFileEntityTemporaryUrlForCurrentVersion(file.getId());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(fileHandle.getKey()));
		// now check that the redirect-based download works correctly
		File tempfile = File.createTempFile("test", null);
		tempfile.deleteOnExit();
		synapse.downloadFromFileEntityCurrentVersion(file.getId(), tempfile);
		assertEquals(fileHandle.getContentMd5(),  MD5ChecksumHelper.getMD5Checksum(tempfile));

		// Get the url using the version number
		tempUrl = synapse.getFileEntityTemporaryUrlForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(fileHandle.getKey()));
		synapse.downloadFromFileEntityForVersion(file.getId(), file.getVersionNumber(), tempfile);
		assertEquals(fileHandle.getContentMd5(),  MD5ChecksumHelper.getMD5Checksum(tempfile));

		// Now get the preview URLs
		tempUrl = synapse.getFileEntityPreviewTemporaryUrlForCurrentVersion(file.getId());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(previewFileHandle.getKey()));
		synapse.downloadFromFileEntityPreviewCurrentVersion(file.getId(), tempfile);
		assertTrue(tempfile.length()>0);

		// Get the preview using the version number
		tempUrl = synapse.getFileEntityPreviewTemporaryUrlForVersion(file.getId(), file.getVersionNumber());
		assertNotNull(tempUrl);
		assertTrue("The temporary URL did not contain the expected file handle key",tempUrl.toString().contains(previewFileHandle.getKey()));
		synapse.downloadFromFileEntityPreviewForVersion(file.getId(), file.getVersionNumber(), tempfile);
		assertTrue(tempfile.length()>0);
	}

	@Test
	public void testFileEntityChangeNameAndContent() throws SynapseException, IOException, InterruptedException, JSONObjectAdapterException {
		// check download on original
		URL fileHandleTemporaryUrl = synapse.getFileHandleTemporaryUrl(fileHandle.getId());
		HttpResponse response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), fileHandleTemporaryUrl.toString(),
				"GET", null, null);
		int statusCode = response.getStatusLine().getStatusCode();
		assertTrue(statusCode >= 200 && statusCode < 300);
		for (Header header : response.getAllHeaders()) {
			if (header.getName().equals("Content-Disposition")) {
				assertEquals("attachment; filename=LittleImage.png", header.getValue());
			}
		}
		HttpEntity httpEntity = response.getEntity();
		ByteArrayOutputStream pngContent = new ByteArrayOutputStream();
		IOUtils.copy(httpEntity.getContent(), pngContent);
		assertEquals("image/png", httpEntity.getContentType().getValue());
		PreviewFileHandle previewFileHandle = waitForPreviewToBeCreated(fileHandle);
		assertEquals("image/png", previewFileHandle.getContentType());

		// create a copy of a different type
		S3FileHandle copy = synapse.createS3FileHandleCopy(fileHandle.getId(), "newname.txt", "application/text");
		fileHandlesToDelete.add(copy.getId());

		// and check download on copy
		fileHandleTemporaryUrl = synapse.getFileHandleTemporaryUrl(copy.getId());
		response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), fileHandleTemporaryUrl.toString(), "GET", null,
				null);
		statusCode = response.getStatusLine().getStatusCode();
		assertTrue(statusCode >= 200 && statusCode < 300);
		for (Header header : response.getAllHeaders()) {
			if (header.getName().equals("Content-Disposition")) {
				assertEquals("attachment; filename=newname.txt", header.getValue());
			}
		}
		httpEntity = response.getEntity();
		ByteArrayOutputStream txtContent = new ByteArrayOutputStream();
		IOUtils.copy(response.getEntity().getContent(), txtContent);
		assertEquals("application/text", httpEntity.getContentType().getValue());

		previewFileHandle = waitForPreviewToBeCreated(copy);
		assertEquals("text/plain", previewFileHandle.getContentType());

		assertArrayEquals(pngContent.toByteArray(), txtContent.toByteArray());

		// delete copy
		synapse.deleteFileHandle(copy.getId());

		// and check download on original again
		fileHandleTemporaryUrl = synapse.getFileHandleTemporaryUrl(fileHandle.getId());
		response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), fileHandleTemporaryUrl.toString(), "GET", null,
				null);
		statusCode = response.getStatusLine().getStatusCode();
		assertTrue(statusCode >= 200 && statusCode < 300);
		httpEntity = response.getEntity();
		ByteArrayOutputStream stillPngContent = new ByteArrayOutputStream();
		IOUtils.copy(response.getEntity().getContent(), stillPngContent);
		assertArrayEquals(pngContent.toByteArray(), stillPngContent.toByteArray());
	}

	@Test
	public void testGetEntityHeaderByMd5() throws Exception {

		String md5 = "548c050497fb361742b85e0835c0cc96";
		List<EntityHeader> results = synapse.getEntityHeaderByMd5(md5);
		assertNotNull(results);
		assertEquals(0, results.size());

		FileEntity file = new FileEntity();
		file.setName("IT054FileEntityTest.testGetEntityHeaderByMd5");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		file = synapse.createEntity(file);
		assertNotNull(file);

		md5 = fileHandle.getContentMd5();
		results = synapse.getEntityHeaderByMd5(md5);
		assertNotNull(results);
		assertEquals(1, results.size());
	}

	@Test
	public void testS3FileCopy() throws Exception {
		FileEntity file = new FileEntity();
		file.setName("file.nopreview");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		file = synapse.createEntity(file);

		final String asyncJobToken = synapse.s3FileCopyAsyncStart(Collections.singletonList(file.getId()), StackConfiguration
				.singleton().getExternalS3TestBucketName(), true, baseKey);

		S3FileCopyResults s3FileCopyResults = TimeUtils.waitFor(20000, 100, new Callable<Pair<Boolean, S3FileCopyResults>>() {
			@Override
			public Pair<Boolean, S3FileCopyResults> call() throws Exception {
				try {
					S3FileCopyResults s3FileCopyResults = synapse.s3FileCopyAsyncGet(asyncJobToken);
					return Pair.create(true, s3FileCopyResults);
				} catch (SynapseResultNotReadyException e) {
					return Pair.create(false, null);
				}
			}
		});

		assertEquals(1, s3FileCopyResults.getResults().size());
		S3FileCopyResult s3FileCopyResult = s3FileCopyResults.getResults().get(0);
		assertEquals(S3FileCopyResultType.COPIED, s3FileCopyResult.getResultType());
		S3TestUtils.addObjectToDelete(s3FileCopyResult.getResultBucket(), s3FileCopyResult.getResultKey());
		s3Client.getObjectMetadata(s3FileCopyResult.getResultBucket(), s3FileCopyResult.getResultKey());

		final String asyncJobToken2 = synapse.s3FileCopyAsyncStart(Collections.singletonList(file.getId()), StackConfiguration.singleton()
				.getExternalS3TestBucketName(), true, baseKey);

		s3FileCopyResults = TimeUtils.waitFor(20000, 100, new Callable<Pair<Boolean, S3FileCopyResults>>() {
			@Override
			public Pair<Boolean, S3FileCopyResults> call() throws Exception {
				try {
					S3FileCopyResults s3FileCopyResults = synapse.s3FileCopyAsyncGet(asyncJobToken2);
					return Pair.create(true, s3FileCopyResults);
				} catch (SynapseResultNotReadyException e) {
					return Pair.create(false, null);
				}
			}
		});

		assertEquals(1, s3FileCopyResults.getResults().size());
		s3FileCopyResult = s3FileCopyResults.getResults().get(0);
		assertEquals(S3FileCopyResultType.UPTODATE, s3FileCopyResult.getResultType());
	}

	@Test
	public void testS3FileCopyCancel() throws Exception {
		FileEntity file = new FileEntity();
		file.setName("file.nopreview");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		file = synapse.createEntity(file);

		final String asyncJobToken = synapse.s3FileCopyAsyncStart(Collections.singletonList(file.getId()), StackConfiguration.singleton()
				.getExternalS3TestBucketName(), true, baseKey);

		synapse.cancelAsynchJob(asyncJobToken);

		assertTrue(TimeUtils.waitFor(20000, 100, asyncJobToken, new Predicate<String>() {
			@Override
			public boolean apply(String asyncJobToken) {
				try {
					synapse.s3FileCopyAsyncGet(asyncJobToken);
					fail("Should have been canceled, maybe test is too time sensitive?");
					return true;
				} catch (SynapseBadRequestException e) {
					return true;
				} catch (SynapseResultNotReadyException e) {
					return false;
				} catch (SynapseException e) {
					fail(e.getMessage());
					return true;
				}
			}
		}));
	}

	/**
	 * Wait for a preview to be generated for the given file handle.
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	private PreviewFileHandle waitForPreviewToBeCreated(S3FileHandle fileHandle) throws InterruptedException,
			SynapseException {
		long start = System.currentTimeMillis();
		while(fileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview file to be created");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview to be created",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			fileHandle = (S3FileHandle) synapse.getRawFileHandle(fileHandle.getId());
		}
		// Fetch the preview file handle
		PreviewFileHandle previewFileHandle = (PreviewFileHandle) synapse.getRawFileHandle(fileHandle.getPreviewId());
		fileHandlesToDelete.add(previewFileHandle.getId());
		return previewFileHandle;
	}
	
}
