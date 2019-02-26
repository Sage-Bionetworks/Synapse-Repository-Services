package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

public class MultipartUtilsTest {
	
	String userId;
	String fileName;
	StorageLocationSetting location;
	
	@Before
	public void before(){
		userId = "123";
		fileName = "foo.txt";
		location = new S3StorageLocationSetting();
	}

	@Test
	public void testGetBucketStorageLocationNull(){
		StorageLocationSetting location = null;
		//call under test
		String bucket = MultipartUtils.getBucket(location);
		assertEquals(StackConfigurationSingleton.singleton().getS3Bucket(), bucket);
	}
	
	@Test
	public void testGetBucketStorageLocationS3(){
		location = new S3StorageLocationSetting();
		//call under test
		String bucket = MultipartUtils.getBucket(location);
		assertEquals(StackConfigurationSingleton.singleton().getS3Bucket(), bucket);
	}
	
	@Test
	public void testGetBucketStorageLocationExternal(){
		ExternalS3StorageLocationSetting location = new ExternalS3StorageLocationSetting();
		//call under test
		location.setBucket("someOtherBuckt");
		String bucket = MultipartUtils.getBucket(location);
		assertEquals(location.getBucket(), bucket);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetBucketStorageLocationWrongType(){
		StorageLocationSetting location = Mockito.mock(StorageLocationSetting.class);
		//call under test
		MultipartUtils.getBucket(location);
	}
	
	@Test
	public void testCreateNewKeyStorageLocationNull(){
		location = null;
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith(userId));
		assertTrue(key.endsWith(fileName));
	}
	
	@Test
	public void testCreateNewKeyStorageLocationExternal(){
		ExternalS3StorageLocationSetting location = new ExternalS3StorageLocationSetting();
		location.setBaseKey("keyBase");
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith("keyBase/"+userId));
		assertTrue(key.endsWith(fileName));
	}
	
	@Test
	public void testCreateNewKeyStorageLocationExternalBaseEmpty(){
		ExternalS3StorageLocationSetting location = new ExternalS3StorageLocationSetting();
		location.setBaseKey("");
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith(userId));
		assertTrue(key.endsWith(fileName));
	}
	
	@Test
	public void testCreateNewKeyStorageLocationExternalBaseNull(){
		ExternalS3StorageLocationSetting location = new ExternalS3StorageLocationSetting();
		location.setBaseKey(null);
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith(userId));
		assertTrue(key.endsWith(fileName));
	}
}
