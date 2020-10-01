package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

public class MultipartRequestUtilsTest {
	
	private MultipartUploadRequest uploadRequest;
	private MultipartUploadCopyRequest copyRequest;
	
	@BeforeEach
	public void before() {
		uploadRequest = new MultipartUploadRequest();
		uploadRequest.setFileName("foo.txt");
		uploadRequest.setFileSizeBytes((long) (1024*1024*100+15));
		uploadRequest.setContentMD5Hex("someMD5Hex");
		uploadRequest.setPartSizeBytes((long) (1024*1024*5));
		uploadRequest.setStorageLocationId(789L);
		uploadRequest.setContentType("plain/text");
		
		FileHandleAssociation sourceFileHandleAssociation = new FileHandleAssociation();
		
		sourceFileHandleAssociation.setFileHandleId("123");
		sourceFileHandleAssociation.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		sourceFileHandleAssociation.setAssociateObjectId("456");
		
		copyRequest = new MultipartUploadCopyRequest();
		copyRequest.setFileName("bar.txt");
		copyRequest.setPartSizeBytes((long) (1024*1024*5));
		copyRequest.setStorageLocationId(789L);
		copyRequest.setSourceFileHandleAssociation(sourceFileHandleAssociation);
	}
	

	@Test
	public void testCalculateMD5AsHexForUpload() {
		// This md5 was generated from the json string of the request.
		String expected = "384da31359066d7e06e31b49b3971739";

		// Call under test
		String md5Hex = MultipartRequestUtils.calculateMD5AsHex(uploadRequest);
		
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testCalculateMD5AsHexForCopy() {
		// This md5 was generated from the json string of the request.
		String expected = "3a1fdde7e50bbf4c0e64f5bb768f4c44";
		
		// Call under test
		String md5Hex = MultipartRequestUtils.calculateMD5AsHex(copyRequest);
		
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testCreateRequestJsonForUpload() {
		String expected = "{\"concreteType\":\"org.sagebionetworks.repo.model.file.MultipartUploadRequest\",\"partSizeBytes\":5242880,\"fileName\":\"foo.txt\",\"storageLocationId\":789,\"contentMD5Hex\":\"someMD5Hex\",\"contentType\":\"plain/text\",\"fileSizeBytes\":104857615}";
		
		// Call under test
		String json =  MultipartRequestUtils.createRequestJSON(uploadRequest);
	
		assertEquals(expected, json);
	}
	
	@Test
	public void testCreateRequestJsonForCopy() {
		String expected = "{\"concreteType\":\"org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest\",\"partSizeBytes\":5242880,\"fileName\":\"bar.txt\",\"storageLocationId\":789,\"sourceFileHandleAssociation\":{\"fileHandleId\":\"123\",\"associateObjectId\":\"456\",\"associateObjectType\":\"FileEntity\"}}";
		
		// Call under test
		String json =  MultipartRequestUtils.createRequestJSON(copyRequest);
	
		assertEquals(expected, json);
	}
	
	@Test
	public void testGetRequestFromJson() {
		String json = MultipartRequestUtils.createRequestJSON(uploadRequest);
	
		MultipartUploadRequest request = MultipartRequestUtils.getRequestFromJson(json, MultipartUploadRequest.class);
	
		assertEquals(uploadRequest, request);
	}

}
