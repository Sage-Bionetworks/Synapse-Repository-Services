package org.sagebionetworks.repo.model;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResponseDataExtractorUtilTest {
	
	@Test
	public void testEntity(){
		FileEntity ob = new FileEntity();
		ob.setId("syn123");
		ob.setConcreteType("concreateType");
		ResponseData responseData = ResponseDataExtractorUtil.getResponseData(ob);
		assertEquals(ob.getId(), responseData.getId());
		assertEquals(ob.getConcreteType(), responseData.getConcreteType());
	}

	@Test
	public void testFileHandle(){
		S3FileHandle ob = new S3FileHandle();
		ob.setId("syn123");
		ResponseData responseData = ResponseDataExtractorUtil.getResponseData(ob);
		assertEquals(ob.getId(), responseData.getId());
		assertEquals("org.sagebionetworks.repo.model.file.S3FileHandle", responseData.getConcreteType());
	}

	@Test
	public void testEntityWithNullIdAndConcreteType(){
		FileEntity ob = new FileEntity();
		ob.setConcreteType(null);
		ResponseData responseData = ResponseDataExtractorUtil.getResponseData(ob);
		assertNull(responseData.getId());
		assertNull(responseData.getConcreteType());
	}
}
