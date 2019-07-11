package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.file.UploadType;

@ExtendWith(MockitoExtension.class)
class CloudServiceMultipartUploadDAOProviderImplTest {

	@Mock
	S3MultipartUploadDAOImpl mockS3Dao;

	@Mock
	GoogleCloudStorageMultipartUploadDAOImpl mockGoogleCloudDao;

	@InjectMocks
	private CloudServiceMultipartUploadDAOProviderImpl provider = new CloudServiceMultipartUploadDAOProviderImpl();

	@Test
	void getS3MultipartUploadDao() {
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.S3) == mockS3Dao);
	}

	@Test
	void getGoogleCloudMultipartUploadDao() {
		assertTrue(provider.getCloudServiceMultipartUploadDao(UploadType.GOOGLECLOUDSTORAGE) == mockGoogleCloudDao);
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