package org.sagebionetworks.repo.manager;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.model.S3Object;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSettingsManagerImplUnitlTest {
	private UserInfo userInfo;
	private ProjectSettingsManagerImpl projectSettingsManagerImpl;
	
	private static final String USER_NAME = "user-name";
	private static final Long USER_ID = 101L;
	private static final String bucketName = "bucketName";
	
	@Mock
	private UserProfileManager userProfileManager;
	
	@Mock
	private SynapseS3Client synapseS3Client;

	@Mock
	private StorageLocationDAO storageLocationDAO;

	@Before
	public void before() {
		projectSettingsManagerImpl = new ProjectSettingsManagerImpl();
	
		ReflectionTestUtils.setField(projectSettingsManagerImpl, "userProfileManager", userProfileManager);
		ReflectionTestUtils.setField(projectSettingsManagerImpl, "s3client", synapseS3Client);
		ReflectionTestUtils.setField(projectSettingsManagerImpl, "storageLocationDAO", storageLocationDAO);
		
		userInfo = new UserInfo(false, USER_ID);
		UserProfile userProfile = new UserProfile();
		userProfile.setUserName(USER_NAME);
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);
		
		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(USER_NAME.getBytes()));
		when(synapseS3Client.getObject(bucketName, "owner.txt")).thenReturn(s3Object);
		
	}

	@Test
	public void testCreateExternalS3ProjectSetting_HappyCase() throws Exception {
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(bucketName);		
		when(storageLocationDAO.create(storageLocationSetting)).thenReturn(999L);
		
		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, storageLocationSetting);
		
		verify(storageLocationDAO).create(storageLocationSetting);
	}

	@Test(expected=CannotDetermineBucketLocationException.class)
	public void testCreateExternalS3ProjectSetting_UnsharedBucket() throws Exception {
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(bucketName);		
		
		when(synapseS3Client.getRegionForBucket(bucketName)).thenThrow(new CannotDetermineBucketLocationException());
		
		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, storageLocationSetting);
	}

}
