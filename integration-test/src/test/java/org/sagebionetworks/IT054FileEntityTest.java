package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class IT054FileEntityTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static final long MAX_WAIT_MS = 1000*10; // 10 sec
	private static final String FILE_NAME = "LittleImage.png";

	private File imageFile;
	private S3FileHandle fileHandle;
	private PreviewFileHandle previewFileHandle;
	private Project project;
	
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
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
		if(fileHandle != null){
			try {
				synapse.deleteFileHandle(fileHandle.getId());
			} catch (Exception e) {}
		}
		if(previewFileHandle != null){
			try {
				synapse.deleteFileHandle(previewFileHandle.getId());
			} catch (Exception e) {}
		}
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
		waitForPreviewToBeCreated(fileHandle);
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

	/**
	 * Wait for a preview to be generated for the given file handle.
	 * @throws InterruptedException
	 * @throws SynapseException
	 */
	private void waitForPreviewToBeCreated(S3FileHandle fileHandle) throws InterruptedException,
			SynapseException {
		long start = System.currentTimeMillis();
		while(fileHandle.getPreviewId() == null){
			System.out.println("Waiting for a preview file to be created");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview to be created",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			fileHandle = (S3FileHandle) synapse.getRawFileHandle(fileHandle.getId());
		}
		// Fetch the preview file handle
		previewFileHandle = (PreviewFileHandle) synapse.getRawFileHandle(fileHandle.getPreviewId());
	}
	
}
