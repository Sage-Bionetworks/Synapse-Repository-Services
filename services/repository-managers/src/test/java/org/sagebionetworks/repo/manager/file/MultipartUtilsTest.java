package org.sagebionetworks.repo.manager.file;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class MultipartUtilsTest {
	
	String userId;
	String fileName;
	StorageLocationSetting location;
	
	@BeforeEach
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
	
	@Test
	public void testGetBucketStorageLocationWrongType(){
		StorageLocationSetting location = Mockito.mock(StorageLocationSetting.class);
		assertThrows(IllegalArgumentException.class, ()->{
			//call under test
			MultipartUtils.getBucket(location);
		});

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
	
	/**
	 * Added as part of PLFM-6626.
	 */
	@Test
	public void testCreateNewKeyWithInvalidName(){
		fileName = "ContainsNonÃs¢II.zip";
		//call under test
		String message = assertThrows(IllegalArgumentException.class, ()->{
			//call under test
			MultipartUtils.createNewKey(userId, fileName, location);
		}).getMessage();
		assertEquals(NameValidation.createInvalidMessage(fileName), message);
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

	@Test
	public void testCreateNewKey_ExternalGoogleStorageLocation(){
		ExternalGoogleCloudStorageLocationSetting location = new ExternalGoogleCloudStorageLocationSetting();
		location.setBaseKey("keyBase");
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith("keyBase/"+userId));
		assertTrue(key.endsWith(fileName));
	}

	@Test
	public void testCreateNewKey_SynapseStorageLocation(){
		S3StorageLocationSetting location = new S3StorageLocationSetting();
		location.setStsEnabled(true);
		location.setBaseKey("keyBase");
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith("keyBase/"+userId));
		assertTrue(key.endsWith(fileName));
	}

	// branch coverage
	@Test
	public void testCreateNewKey_NonBaseKeyLocation(){
		ProxyStorageLocationSettings location = new ProxyStorageLocationSettings();
		//call under test
		String key = MultipartUtils.createNewKey(userId, fileName, location);
		assertNotNull(key);
		assertTrue(key.startsWith(userId));
		assertTrue(key.endsWith(fileName));
	}

}
