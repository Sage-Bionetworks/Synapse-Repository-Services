package org.sagebionetworks.repo.model.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.file.PartUtils.MAX_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.file.PartUtils.MAX_NUMBER_OF_PARTS;
import static org.sagebionetworks.repo.model.file.PartUtils.MIN_PART_SIZE_BYTES;
import static org.sagebionetworks.repo.model.file.PartUtils.calculateNumberOfParts;
import static org.sagebionetworks.repo.model.file.PartUtils.choosePartSize;
import static org.sagebionetworks.repo.model.file.PartUtils.validateFileSize;

import org.junit.jupiter.api.Test;

public class PartUtilsTest {

	@Test
	public void testCalculateNumberOfPartsSmall() {
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES;
		// call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(1, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsNoRemainder() {
		long fileSize = MIN_PART_SIZE_BYTES * 2;
		long partSize = MIN_PART_SIZE_BYTES;
		// call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(2, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsWithRemainder() {
		long fileSize = MIN_PART_SIZE_BYTES * 2 + 1;
		long partSize = MIN_PART_SIZE_BYTES;
		// call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(3, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfLessThanOne() {
		long fileSize = 0;
		long partSize = MIN_PART_SIZE_BYTES;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			calculateNumberOfParts(fileSize, partSize);
		}).getMessage();

		assertEquals("File size must be at least one byte", errorMessage);
	}

	@Test
	public void testCalculateNumberOfPartTooSmall() {
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES - 1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			calculateNumberOfParts(fileSize, partSize);
		}).getMessage();

		assertEquals("The part size must be at least 5242880 bytes.", errorMessage);
	}

	@Test
	public void testCalculateNumberOfPartAtMax() {
		long fileSize = MIN_PART_SIZE_BYTES * MAX_NUMBER_OF_PARTS;
		long partSize = MIN_PART_SIZE_BYTES;
		// call under test
		int numberOfParts = calculateNumberOfParts(fileSize, partSize);
		assertEquals(MAX_NUMBER_OF_PARTS, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartOverMax() {
		long fileSize = MIN_PART_SIZE_BYTES * MAX_NUMBER_OF_PARTS + 1;
		long partSize = MIN_PART_SIZE_BYTES;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			calculateNumberOfParts(fileSize, partSize);
		}).getMessage();

		assertEquals(
				"File Upload would require 10001 parts, which exceeds the maximum number of allowed parts of: 10000. Please choose a larger part size.",
				errorMessage);
	}

	@Test
	public void testChoosePartSizeFileSizeLessThanOne() {
		long fileSize = 0;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			choosePartSize(fileSize);
		}).getMessage();

		assertEquals("File size must be at least one byte", errorMessage);
	}

	@Test
	public void testChoosePartSizeFileSizeOne() {
		long fileSize = 1;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}

	@Test
	public void testChoosePartSizeFileSizeMin() {
		long fileSize = MIN_PART_SIZE_BYTES;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}

	@Test
	public void testChoosePartSizeFileSize50GB() {
		long fileSize = MIN_PART_SIZE_BYTES * MAX_NUMBER_OF_PARTS;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertEquals(MIN_PART_SIZE_BYTES, partSize);
	}

	@Test
	public void testChoosePartSizeFileSizeOver50GB() {
		long fileSize = MIN_PART_SIZE_BYTES * MAX_NUMBER_OF_PARTS + 1;
		// call under test
		long partSize = choosePartSize(fileSize);
		assertTrue(partSize > MIN_PART_SIZE_BYTES);
	}

	@Test
	public void testValidateFileSizeUnder() {
		long fileSize = 0L;
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validateFileSize(fileSize);
		}).getMessage();

		assertEquals("File size must be at least one byte", errorMessage);
	}

	@Test
	public void testValidateFileSizeOver() {
		long fileSize = MAX_FILE_SIZE_BYTES + 1L;
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validateFileSize(fileSize);
		}).getMessage();

		assertEquals("The maximum file size is 5 TB", errorMessage);
	}

	@Test
	public void testValidateFileSizeAtSize() {
		long fileSize = MAX_FILE_SIZE_BYTES;
		// call under test
		validateFileSize(fileSize);
	}

	@Test
	public void testValidateFileSizeOne() {
		long fileSize = 1;
		// call under test
		validateFileSize(fileSize);
	}

	@Test
	public void testValidatePartSize() {
		long partSize = PartUtils.MIN_PART_SIZE_BYTES;

		PartUtils.validatePartSize(partSize);

	}

	@Test
	public void testValidatePartSizeLessThanMin() {
		long partSize = PartUtils.MIN_PART_SIZE_BYTES - 1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			PartUtils.validatePartSize(partSize);
		}).getMessage();

		assertEquals("The part size must be at least 5242880 bytes.", errorMessage);
	}

	@Test
	public void testValidatePartSizeNegative() {
		long partSize = -1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			PartUtils.validatePartSize(partSize);
		}).getMessage();

		assertEquals("The part size must be at least 5242880 bytes.", errorMessage);
	}

	@Test
	public void testValidatePartSizeLargerThanMax() {
		long partSize = PartUtils.MAX_PART_SIZE_BYTES + 1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			PartUtils.validatePartSize(partSize);
		}).getMessage();

		assertEquals("The part size must not exceed 5368709120 bytes.", errorMessage);
	}

	@Test
	public void testGetPartRange() {
		final long partNumber = 1;
		final long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		final long fileSizeBytes = PartUtils.MAX_PART_SIZE_BYTES;

		long[] expected = new long[] { 0, partSizeBytes - 1 };

		assertArrayEquals(expected, PartUtils.getPartRange(partNumber, partSizeBytes, fileSizeBytes));
	}

	@Test
	public void testGetPartRangeLastPart() {
		final long partNumber = 2;
		final long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		// Make the file 2 parts, with the second being half a part size
		final long fileSizeBytes = partSizeBytes + partSizeBytes / 2;

		long[] expected = new long[] { partSizeBytes, (partSizeBytes + partSizeBytes / 2 - 1) };

		assertArrayEquals(expected, PartUtils.getPartRange(partNumber, partSizeBytes, fileSizeBytes));
	}

	@Test
	public void testGetPartRangeMultiple() {
		final long partSizeBytes = PartUtils.MIN_PART_SIZE_BYTES;
		final int numberOfParts = 5;
		// The file fit exactly all the parts
		final long fileSizeBytes = numberOfParts * partSizeBytes;

		long firstByte = 0;
		long lastByte = partSizeBytes - 1;
		
		for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {

			long[] expected = new long[] { firstByte, lastByte };

			assertArrayEquals(expected, PartUtils.getPartRange(partNumber, partSizeBytes, fileSizeBytes));

			firstByte += partSizeBytes;
			lastByte += partSizeBytes;
		}
	}

}
