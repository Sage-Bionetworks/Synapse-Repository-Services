package org.sagebionetworks.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class MultipartUploadTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testChoosePartSizeFileSizeLessThanOne(){
		long fileSize = 0;
		// call under test
		long partSize = MultipartUpload.choosePartSize(fileSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeOne(){
		long fileSize = 1;
		// call under test
		long partSize = MultipartUpload.choosePartSize(fileSize);
		assertEquals(MultipartUpload.MINIMUM_PART_SIZE, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeMin(){
		long fileSize = MultipartUpload.MINIMUM_PART_SIZE;
		// call under test
		long partSize = MultipartUpload.choosePartSize(fileSize);
		assertEquals(MultipartUpload.MINIMUM_PART_SIZE, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSize50GB(){
		long fileSize = MultipartUpload.MINIMUM_PART_SIZE*MultipartUpload.MAX_NUMBER_OF_PARTS;
		// call under test
		long partSize = MultipartUpload.choosePartSize(fileSize);
		assertEquals(MultipartUpload.MINIMUM_PART_SIZE, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeOver50GB(){
		long fileSize = MultipartUpload.MINIMUM_PART_SIZE*MultipartUpload.MAX_NUMBER_OF_PARTS+1;
		// call under test
		long partSize = MultipartUpload.choosePartSize(fileSize);
		assertEquals(MultipartUpload.MINIMUM_PART_SIZE, partSize);
	}

}
