package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

public class IT049FileHandleTest {
	
	private static final String LARGE_FILE_PATH_PROP_KEY = "org.sagebionetworks.test.large.file.path";

	public static final long MAX_WAIT_MS = 1000*10; // 10 sec
	
	private static String FILE_NAME = "LittleImage.png";

	private List<FileHandle> toDelete = null;

	private static Synapse synapse = null;
	File imageFile;
	
	private static Synapse createSynapseClient(String user, String pw) throws SynapseException {
		Synapse synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
		synapse.login(user, pw);
		
		return synapse;
	}
	
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}
	
	@Before
	public void before() throws SynapseException {
		toDelete = new ArrayList<FileHandle>();
		// Get the image file from the classpath.
		URL url = IT049FileHandleTest.class.getClassLoader().getResource("images/"+FILE_NAME);
		imageFile = new File(url.getFile().replaceAll("%20", " "));
		
	}

	/**
	 * @throws Exception 
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@After
	public void after() throws Exception {
		if (toDelete != null) {
			for (FileHandle handle: toDelete) {
				try {
					synapse.deleteFileHandle(handle.getId());
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testImageFileRoundTrip() throws SynapseException, IOException, InterruptedException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// Create the image
		List<File> list = new LinkedList<File>();
		list.add(imageFile);
		FileHandleResults results = synapse.createFileHandles(list);
		assertNotNull(results);
		// We should have one image on the list
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		S3FileHandle handle = (S3FileHandle) results.getList().get(0);
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
			assertTrue("Timed out waiting for a preview image to be created.", (System.currentTimeMillis()-start) < MAX_WAIT_MS);
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		// Get the preview file handle.
		PreviewFileHandle preview = (PreviewFileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		System.out.println(preview);
		
		//clear the preview and wait for it to be recreated
		synapse.clearPreview(handle.getId());
		handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		while(handle.getPreviewId() == null){
			System.out.println("Waiting for a preview to be recreated...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for a preview image to be created.", (System.currentTimeMillis()-start) < MAX_WAIT_MS);
			handle = (S3FileHandle) synapse.getRawFileHandle(handle.getId());
		}
		preview = (PreviewFileHandle) synapse.getRawFileHandle(handle.getPreviewId());
		assertNotNull(preview);
		
		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try{
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		}catch(SynapseException e){
			// expected.
		}
		try{
			synapse.getRawFileHandle(handle.getPreviewId());
			fail("The handle should be deleted.");
		}catch(SynapseException e){
			// expected.
		}
	}
	
	@Test
	public void testSingleFileRoundTrip() throws SynapseException, IOException, InterruptedException{
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());
		String expectedMD5 = MD5ChecksumHelper.getMD5Checksum(imageFile);
		// Create the image
		String myContentType = "test/content-type";
		FileHandle result = synapse.createFileHandle(imageFile, myContentType);
		assertNotNull(result);
		S3FileHandle handle = (S3FileHandle) result;
		toDelete.add(handle);
		System.out.println(handle);
		assertEquals(myContentType, handle.getContentType());
		assertEquals(FILE_NAME, handle.getFileName());
		assertEquals(new Long(imageFile.length()), handle.getContentSize());
		assertEquals(expectedMD5, handle.getContentMd5());
		
		//preview will not be created for our test content type

		// Now delete the root file handle.
		synapse.deleteFileHandle(handle.getId());
		// The main handle and the preview should get deleted.
		try{
			synapse.getRawFileHandle(handle.getId());
			fail("The handle should be deleted.");
		}catch(SynapseException e){
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
	
	/**
	 * This test uploads files that are too large to include in the build.
	 * To run this test, set the property to point to a large file: org.sagebionetworks.test.large.file.path=<path to large file>
	 * @throws IOException 
	 * @throws SynapseException 
	 */
	@Test
	public void testLargeFileUplaod() throws SynapseException, IOException{
		String largeFileName = System.getProperty(LARGE_FILE_PATH_PROP_KEY);
		if(largeFileName != null){
			// Run the test
			File largeFile = new File(largeFileName);
			assertTrue(largeFile.exists());
			System.out.println("Attempting to upload a file of size: "+largeFile.length());
			float fileSize = largeFile.length();
			float bytesPerMB = (float) Math.pow(2, 20);
			float fileSizeMB = fileSize/bytesPerMB;
			System.out.println(String.format("Attempting to upload file: %1$s of size %2$.2f",  largeFile.getName(), fileSizeMB));
			String contentType = Synapse.guessContentTypeFromStream(largeFile);
			long start = System.currentTimeMillis();
			S3FileHandle handle = synapse.createFileHandle(largeFile, contentType);
			long elapse = System.currentTimeMillis()-start;
			float elapseSecs = elapse/1000;
			float mbPerSec = fileSizeMB/elapseSecs;
			System.out.println(String.format("Upload file: %1$s of size %2$.2f in %3$.2f secs with rate %4$.2f MB/Sec",  largeFile.getName(), fileSizeMB, elapseSecs, mbPerSec));
			assertNotNull(handle);
			toDelete.add(handle);
		}else{
			System.out.println("The property: '"+LARGE_FILE_PATH_PROP_KEY+"' was not set.  The testLargeFileUplaod() test was not run");
		}
	}
}
