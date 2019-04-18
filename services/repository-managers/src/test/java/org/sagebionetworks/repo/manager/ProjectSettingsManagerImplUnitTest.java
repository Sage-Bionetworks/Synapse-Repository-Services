package org.sagebionetworks.repo.manager;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;

import com.amazonaws.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectSettingsManagerImplUnitTest {
	private UserInfo userInfo;

	@InjectMocks
	private ProjectSettingsManagerImpl projectSettingsManagerImpl;
	
	private static final String USER_NAME = "user-name";
	private static final Long USER_ID = 101L;
	private static final String bucketName = "bucket.name";
	
	@Mock
	private UserProfileManager userProfileManager;
	
	@Mock
	private SynapseS3Client synapseS3Client;

	@Mock
	private StorageLocationDAO storageLocationDAO;

	ExternalS3StorageLocationSetting externalS3StorageLocationSetting;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, USER_ID);
		UserProfile userProfile = new UserProfile();
		userProfile.setUserName(USER_NAME);
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);

		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(bucketName);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_HappyCase() throws Exception {
		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(USER_NAME.getBytes()));
		when(synapseS3Client.getObject(bucketName, "owner.txt")).thenReturn(s3Object);

		when(storageLocationDAO.create(externalS3StorageLocationSetting)).thenReturn(999L);
		
		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		
		verify(storageLocationDAO).create(externalS3StorageLocationSetting);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_UnsharedBucket() throws Exception {
		when(synapseS3Client.getRegionForBucket(bucketName)).thenThrow(new CannotDetermineBucketLocationException());

		assertThrows(CannotDetermineBucketLocationException.class, ()->{
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_InvalidS3BucketName(){
		externalS3StorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");

		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_InvalidS3BucketName(){
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");

		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		});
	}

}
