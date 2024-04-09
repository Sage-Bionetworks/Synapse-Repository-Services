package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.file.UploadType;

@ExtendWith(MockitoExtension.class)
class CloudServiceMultipartUploadDAOProviderImplTest {

	@Mock
	private S3MultipartUploadDAOImpl mockS3Dao;

	@Mock
	private GoogleCloudStorageMultipartUploadDAOImpl mockGoogleCloudDao;
	
	@Mock
	private AsyncGoogleMultipartUploadDao mockAsyncGoogleMultipartUploadDAO;

	@Mock
	private FeatureStatusDao mockFeatureStatusDao;


	@InjectMocks
	private CloudServiceMultipartUploadDAOProviderImpl provider = new CloudServiceMultipartUploadDAOProviderImpl();

	@Test
	void getS3MultipartUploadDao() {
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.S3) == mockS3Dao);
	}

	@Test
	void getGoogleCloudMultipartUploadDaoWithEmpty() {
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.empty());
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.GOOGLECLOUDSTORAGE) == mockAsyncGoogleMultipartUploadDAO);
		verify(mockFeatureStatusDao).isFeatureEnabled(Feature.USE_NEW_ASYNC_GOOGLE_MULTIPART_UPLOAD);
	}
	
	@Test
	void getGoogleCloudMultipartUploadDaoWithDisabled() {
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(false));
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.GOOGLECLOUDSTORAGE) == mockGoogleCloudDao);
		verify(mockFeatureStatusDao).isFeatureEnabled(Feature.USE_NEW_ASYNC_GOOGLE_MULTIPART_UPLOAD);
	}
	
	@Test
	void getGoogleCloudMultipartUploadDaoWithEnabled() {
		when(mockFeatureStatusDao.isFeatureEnabled(any())).thenReturn(Optional.of(true));
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.GOOGLECLOUDSTORAGE) == mockAsyncGoogleMultipartUploadDAO);
		verify(mockFeatureStatusDao).isFeatureEnabled(Feature.USE_NEW_ASYNC_GOOGLE_MULTIPART_UPLOAD);
	}

	@Test
	void getDaoForNonMultipartUploadTypes() {
		for (UploadType type : UploadType.values()) {
			if (type.equals(UploadType.S3) || type.equals(UploadType.GOOGLECLOUDSTORAGE)) {
				// Do nothing, these are valid
			} else {
				assertThrows(IllegalArgumentException.class, () -> provider.getCloudServiceMultipartUploadDao(type));
			}
		}
	}

}