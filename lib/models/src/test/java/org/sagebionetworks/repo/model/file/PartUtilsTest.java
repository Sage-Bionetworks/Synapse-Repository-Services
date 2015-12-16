package org.sagebionetworks.repo.model.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.file.PartUtils.MAX_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.file.PartUtils.MAX_NUMBER_OF_PARTS;
import static org.sagebionetworks.repo.model.file.PartUtils.MIN_PART_SIZE_BYTES;
import static org.sagebionetworks.repo.model.file.PartUtils.calculateNumberOfParts;
import static org.sagebionetworks.repo.model.file.PartUtils.choosePartSize;
import static org.sagebionetworks.repo.model.file.PartUtils.validateFileSize;

import org.junit.Test;

public class PartUtilsTest {
	
	@Test
	public void testCalculateNumberOfPartsSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(1, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsNoRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(2, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartsWithRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(3, numberOfParts);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfLessThanOne(){
		long fileSize = 0;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfPartTooSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES-1;
		//call under test
		calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test
	public void testCalculateNumberOfPartAtMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(MAX_NUMBER_OF_PARTS, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartOverMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		try {
			calculateNumberOfParts(fileSize, partSize);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("10001"));
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChoosePartSizeFileSizeLessThanOne(){
		long fileSize = 0;
		// call under test
		choosePartSize(fileSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeOne(){
		long fileSize = 1;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeMin(){
		long fileSize = MIN_PART_SIZE_BYTES;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSize50GB(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}
	
	@Test
	public void testChoosePartSizeFileSizeOver50GB(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS+1;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFileSizeUnder(){
		long fileSize = 0L;
		// call under test
		validateFileSize(fileSize);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFileSizeOver(){
		long fileSize = MAX_FILE_SIZE_BYTES+1L;
		// call under test
		validateFileSize(fileSize);
	}
	
	@Test
	public void testValidateFileSizeAtSize(){
		long fileSize = MAX_FILE_SIZE_BYTES;
		// call under test
		validateFileSize(fileSize);
	}
	
	@Test
	public void testValidateFileSizeOne(){
		long fileSize = 1;
		// call under test
		validateFileSize(fileSize);
	}

}
