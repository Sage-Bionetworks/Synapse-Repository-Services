package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BucketObjectReaderProviderAutowireTest {
	
	@Autowired
	private BucketObjectReaderProvider provider;

	@ParameterizedTest
	@ValueSource(classes = { S3FileHandle.class, GoogleCloudFileHandle.class } )
	public void testGetBucketObjectReader(Class<? extends CloudProviderFileHandleInterface> clazz) {
		assertNotNull(provider.getBucketObjectReader(clazz));
	}
	
	@Test
	public void testGetBucketObjectReaderWithUnsupportedClazz() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			provider.getBucketObjectReader(UnsupportedFileHandle.class);
		}).getMessage();
		
		assertEquals("Unsupported file handle type " + UnsupportedFileHandle.class.getName(), errorMessage);
	}
	
	private static interface UnsupportedFileHandle extends CloudProviderFileHandleInterface { }

}
