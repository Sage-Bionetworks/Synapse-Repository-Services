package org.sagebionetworks.repo.model;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AccessRecordExtractorUtilTest {
	
	@Test
	public void testEntity(){
		FileEntity ob = new FileEntity();
		ob.setId("syn123");
		ob.setConcreteType("concreateType");
		String id = AccessRecordExtractorUtil.getObjectId(ob);
		String concreteType = AccessRecordExtractorUtil.getConcreteType(ob);
		assertEquals(ob.getId(), id);
		assertEquals(ob.getConcreteType(), concreteType);
	}

	@Test
	public void testFileHandle(){
		S3FileHandle ob = new S3FileHandle();
		ob.setId("syn123");
		String id = AccessRecordExtractorUtil.getObjectId(ob);
		String concreteType = AccessRecordExtractorUtil.getConcreteType(ob);
		assertEquals(ob.getId(), id);
		assertEquals("org.sagebionetworks.repo.model.file.S3FileHandle", concreteType);
	}

	@Test
	public void testEntityWithNullIdAndConcreteType(){
		FileEntity ob = new FileEntity();
		ob.setConcreteType(null);
		String id = AccessRecordExtractorUtil.getObjectId(ob);
		String concreteType = AccessRecordExtractorUtil.getConcreteType(ob);
		assertNull(id);
		assertNull(concreteType);
	}
}
