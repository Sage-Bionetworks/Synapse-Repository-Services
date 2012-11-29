package org.sagebionetworks.repo.model.dbo;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;

public class FileMetadataUtilsTest {
	
	@Test
	public void testExternalFileMetadataRoundTrip(){
		// External
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setContentMd5("md5");
		meta.setContentSize(123l);
		meta.setContentType("text/plain");
		meta.setCreatedByPrincipalId(456l);
		meta.setExternalURL("http://google.com");
		meta.setId("987");
		meta.setImplementationType(ExternalFileMetadata.class.getName());
		System.out.println(meta);
		meta.setPreviewId("456");
		
	}

}
