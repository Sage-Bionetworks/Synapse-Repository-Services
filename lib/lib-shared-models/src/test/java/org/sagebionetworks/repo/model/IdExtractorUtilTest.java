package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class IdExtractorUtilTest {
	
	@Test
	public void testEntity(){
		FileEntity ob = new FileEntity();
		ob.setId("syn123");
		String value = IdExtractorUtil.getObjectId(ob);
		assertEquals(ob.getId(), value);
	}

	@Test
	public void testFileHandle(){
		S3FileHandle ob = new S3FileHandle();
		ob.setId("syn123");
		String value = IdExtractorUtil.getObjectId(ob);
		assertEquals(ob.getId(), value);
	}
}
