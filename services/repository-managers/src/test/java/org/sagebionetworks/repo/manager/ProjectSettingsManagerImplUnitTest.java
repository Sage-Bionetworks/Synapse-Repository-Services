package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.migration.MergeStorageLocationsResponse;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;

import com.amazonaws.services.s3.model.S3Object;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
	private SynapseGoogleCloudStorageClient synapseGoogleCloudStorageClient;

	@Mock
	private StorageLocationDAO mockStorageLocationDAO;

	@Mock
	private BufferedReader mockBufferedReader;

	@Mock
	private FileHandleDao mockFileHandleDao;

	@Mock
	private ProjectSettingsDAO mockProjectSettingDao;

	private ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	private ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, USER_ID);
		UserProfile userProfile = new UserProfile();
		userProfile.setUserName(USER_NAME);
		when(userProfileManager.getUserProfile(USER_ID.toString())).thenReturn(userProfile);

		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(bucketName);

		externalGoogleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		externalGoogleCloudStorageLocationSetting.setBucket(bucketName);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_HappyCase() throws Exception {
		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(USER_NAME.getBytes()));
		when(synapseS3Client.getObject(bucketName, "owner.txt")).thenReturn(s3Object);

		when(mockStorageLocationDAO.create(externalS3StorageLocationSetting)).thenReturn(999L);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalS3StorageLocationSetting);
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_UnsharedBucket() throws Exception {
		when(synapseS3Client.getRegionForBucket(bucketName)).thenThrow(new CannotDetermineBucketLocationException());

		assertThrows(CannotDetermineBucketLocationException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalS3StorageLocationSetting_InvalidS3BucketName() {
		externalS3StorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalS3StorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalObjectStorageLocationSetting_InvalidS3BucketName() {
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket("s3://my-bucket-name-is-wrong/");
		externalObjectStorageLocationSetting.setEndpointUrl("https://myendpoint.com");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		});
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting() throws Exception {
		when(synapseGoogleCloudStorageClient.bucketExists(bucketName)).thenReturn(true);
		when(synapseGoogleCloudStorageClient.getObject(bucketName, "owner.txt")).thenReturn(mock(Blob.class));
		when(synapseGoogleCloudStorageClient.getObjectContent(bucketName, "owner.txt")).thenReturn(mockBufferedReader);
		when(mockBufferedReader.readLine()).thenReturn("user-name");
		when(mockStorageLocationDAO.create(externalGoogleCloudStorageLocationSetting)).thenReturn(999L);

		// method under test
		projectSettingsManagerImpl.createStorageLocationSetting(userInfo, externalGoogleCloudStorageLocationSetting);

		verify(mockStorageLocationDAO).create(externalGoogleCloudStorageLocationSetting);
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting_UnsharedBucket() {
		when(synapseGoogleCloudStorageClient.bucketExists(any())).thenThrow(new StorageException(403,
				"someaccount@gserviceaccount.com does not have storage.buckets.get access to somebucket"));
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo,
					externalGoogleCloudStorageLocationSetting);
		});

		assertTrue(e.getMessage().contains("Synapse does not have access to the Google Cloud bucket " + bucketName));
	}

	@Test
	public void testCreateExternalGoogleCloudStorageLocationSetting_NonExistentBucket() {
		when(synapseGoogleCloudStorageClient.bucketExists("notabucket")).thenReturn(false);
		externalGoogleCloudStorageLocationSetting.setBucket("notabucket");

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			projectSettingsManagerImpl.createStorageLocationSetting(userInfo,
					externalGoogleCloudStorageLocationSetting);
		});
	}

	@Test
	public void testMergeStorageLocationsUnauthorized() {
		UnauthorizedException e = assertThrows(UnauthorizedException.class, () -> {
			// method under test
			projectSettingsManagerImpl.mergeDuplicateStorageLocations(userInfo);
		});
		assertEquals(e.getMessage(), "Only administrators can invoke this service");
	}

	@Test
	public void testMergeStorageLocationsNoDuplicates() {

		UserInfo adminUser = new UserInfo(true);

		when(mockStorageLocationDAO.findAllWithDuplicates()).thenReturn(Collections.emptyList());

		// Call under test
		MergeStorageLocationsResponse response = projectSettingsManagerImpl.mergeDuplicateStorageLocations(adminUser);

		verify(mockStorageLocationDAO, never()).findDuplicates(any());
		verify(mockFileHandleDao, never()).updateStorageLocationBatch(any(), any());
		verify(mockProjectSettingDao, never()).getByType(any());
		verify(mockProjectSettingDao, never()).update(any());
		verify(mockStorageLocationDAO, never()).deleteBatch(any());

		assertEquals(0l, response.getDuplicateLocationsCount());
		assertEquals(0l, response.getUpdatedProjectsCount());
	}

	@Test
	public void testMergeStorageLocationsWithDuplicatesNoProject() {

		UserInfo adminUser = new UserInfo(true);

		Long masterLocationId = 1l;

		Set<Long> duplicateLocationIds = ImmutableSet.of(2l, 3l);

		List<Long> allLocationIds = new ArrayList<>();

		allLocationIds.add(masterLocationId);
		allLocationIds.addAll(duplicateLocationIds);

		when(mockStorageLocationDAO.findAllWithDuplicates()).thenReturn(Arrays.asList(masterLocationId));
		when(mockStorageLocationDAO.findDuplicates(masterLocationId)).thenReturn(duplicateLocationIds);
		
		List<ProjectSetting> projectSettings = Collections.emptyList();
		
		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());

		// Call under test
		MergeStorageLocationsResponse response = projectSettingsManagerImpl.mergeDuplicateStorageLocations(adminUser);

		verify(mockFileHandleDao, times(1)).updateStorageLocationBatch(duplicateLocationIds, masterLocationId);
		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, never()).update(any());
		verify(mockStorageLocationDAO, times(1)).deleteBatch(duplicateLocationIds);

		assertEquals(2l, response.getDuplicateLocationsCount());
		assertEquals(0l, response.getUpdatedProjectsCount());
	}

	@Test
	public void testMergeStorageLocationsWithDuplicatesAndProjectNoUpdate() {

		UserInfo adminUser = new UserInfo(true);

		Long masterLocationId = 1l;

		Set<Long> duplicateLocationIds = ImmutableSet.of(2l, 3l);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Arrays.asList(masterLocationId));

		List<Long> allLocationIds = new ArrayList<>();

		allLocationIds.add(masterLocationId);
		allLocationIds.addAll(duplicateLocationIds);

		when(mockStorageLocationDAO.findAllWithDuplicates()).thenReturn(Arrays.asList(masterLocationId));
		when(mockStorageLocationDAO.findDuplicates(masterLocationId)).thenReturn(duplicateLocationIds);
		
		List<ProjectSetting> projectSettings = Collections.singletonList(projectSetting);
		
		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());

		// Call under test
		MergeStorageLocationsResponse response = projectSettingsManagerImpl.mergeDuplicateStorageLocations(adminUser);

		verify(mockFileHandleDao, times(1)).updateStorageLocationBatch(duplicateLocationIds, masterLocationId);
		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, never()).update(any());
		verify(mockStorageLocationDAO, times(1)).deleteBatch(duplicateLocationIds);

		assertEquals(2l, response.getDuplicateLocationsCount());
		assertEquals(0l, response.getUpdatedProjectsCount());
	}

	@Test
	public void testMergeStorageLocationsWithDuplicatesAndProjectUpdate() {

		UserInfo adminUser = new UserInfo(true);

		Long masterLocationId = 1l;

		Set<Long> duplicateLocationIds = ImmutableSet.of(2l, 3l);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Arrays.asList(masterLocationId, 2l));

		UploadDestinationListSetting projectSettingUpdated = new UploadDestinationListSetting();
		projectSettingUpdated.setLocations(Arrays.asList(masterLocationId));

		List<Long> allLocationIds = new ArrayList<>();

		allLocationIds.add(masterLocationId);
		allLocationIds.addAll(duplicateLocationIds);

		when(mockStorageLocationDAO.findAllWithDuplicates()).thenReturn(Arrays.asList(masterLocationId));
		when(mockStorageLocationDAO.findDuplicates(masterLocationId)).thenReturn(duplicateLocationIds);
		
		List<ProjectSetting> projectSettings = Collections.singletonList(projectSetting);
		
		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());
		when(mockProjectSettingDao.update(any())).thenReturn(projectSettingUpdated);

		// Call under test
		MergeStorageLocationsResponse response = projectSettingsManagerImpl.mergeDuplicateStorageLocations(adminUser);

		verify(mockFileHandleDao, times(1)).updateStorageLocationBatch(duplicateLocationIds, masterLocationId);
		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, times(1)).update(eq(projectSettingUpdated));
		verify(mockStorageLocationDAO, times(1)).deleteBatch(duplicateLocationIds);

		assertEquals(2l, response.getDuplicateLocationsCount());
		assertEquals(1l, response.getUpdatedProjectsCount());
	}

	@Test
	public void testRemoveDuplicateStorageLocationsFromProjectsWithNoUpdate() {

		Long masterLocationId = 1l;
		Long duplicateLocationId = 2l;

		Map<Long, Long> duplicatesMap = ImmutableMap.of(duplicateLocationId, masterLocationId);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Arrays.asList(masterLocationId));
		
		List<ProjectSetting> projectSettings = Collections.singletonList(projectSetting);

		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());

		// Call under test
		projectSettingsManagerImpl.removeDuplicateStorageLocationsFromProjects(duplicatesMap);

		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, never()).update(any());

	}
	
	@Test
	public void testRemoveDuplicateStorageLocationsFromProjectsWithUpdate() {

		Long masterLocationId = 1l;
		Long duplicateLocationId = 2l;

		Map<Long, Long> duplicatesMap = ImmutableMap.of(duplicateLocationId, masterLocationId);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Arrays.asList(duplicateLocationId));

		UploadDestinationListSetting projectSettingUpdated = new UploadDestinationListSetting();
		projectSettingUpdated.setLocations(Arrays.asList(masterLocationId));
		
		List<ProjectSetting> projectSettings = Collections.singletonList(projectSetting);

		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());
		when(mockProjectSettingDao.update(any())).thenReturn(projectSettingUpdated);

		// Call under test
		projectSettingsManagerImpl.removeDuplicateStorageLocationsFromProjects(duplicatesMap);

		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, times(1)).update(eq(projectSettingUpdated));

	}

	@Test
	public void testRemoveDuplicateStorageLocationsFromProjectsWithUpdateReplace() {

		Long masterLocationId = 1l;
		Long duplicateLocationId = 2l;

		Map<Long, Long> duplicatesMap = ImmutableMap.of(duplicateLocationId, masterLocationId);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Arrays.asList(masterLocationId, duplicateLocationId));

		UploadDestinationListSetting projectSettingUpdated = new UploadDestinationListSetting();
		projectSettingUpdated.setLocations(Arrays.asList(masterLocationId));

		List<ProjectSetting> projectSettings = Collections.singletonList(projectSetting);
		
		when(mockProjectSettingDao.getByType(any())).thenReturn(projectSettings.iterator());
		when(mockProjectSettingDao.update(any())).thenReturn(projectSettingUpdated);

		// Call under test
		projectSettingsManagerImpl.removeDuplicateStorageLocationsFromProjects(duplicatesMap);

		verify(mockProjectSettingDao, times(1)).getByType(any());
		verify(mockProjectSettingDao, times(1)).update(eq(projectSettingUpdated));

	}
}
