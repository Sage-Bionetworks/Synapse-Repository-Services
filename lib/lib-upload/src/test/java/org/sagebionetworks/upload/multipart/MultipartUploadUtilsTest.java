package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.upload.PartRange;

class MultipartUploadUtilsTest {
	@Test
	public void testCreatePartKey(){
		assertEquals("baseKey/9999", MultipartUploadUtils.createPartKey("baseKey", 9999));
	}

	@Test
	public void testCreateRangedPartKeyOfSizeOne(){
		assertEquals("baseKey/9999", MultipartUploadUtils.createPartKeyFromRange("baseKey", 9999, 9999));
	}

	@Test
	public void testCreateRangedPartKey(){
		assertEquals("baseKey/55-66", MultipartUploadUtils.createPartKeyFromRange("baseKey", 55, 66));
	}

	@Test
	public void testComputeCurrentPartSizeSingletonFirstPart() {
		assertEquals(1L, MultipartUploadUtils.computeStitchTargetSize(1, 1, 100));
	}

	@Test
	public void testComputeCurrentPartSizeSingletonLastPart() {
		assertEquals(1L, MultipartUploadUtils.computeStitchTargetSize(5, 5, 5));
	}

	@Test
	public void testComputeCurrentPartSizeIncompletePartCase() {
		assumeTrue(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE > 2);
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE, MultipartUploadUtils.computeStitchTargetSize(4, 7, 7));
	}

	@Test
	public void testComputeCurrentPartSizeCompletePartCase() {
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE,
				MultipartUploadUtils.computeStitchTargetSize(1, MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE, MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE));
	}

	@Test
	public void testComputeCurrentPartSize_NextSizeUp() {
		// The part size should square when above the max
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE * MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE,
				MultipartUploadUtils.computeStitchTargetSize(1, MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE + 1, MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE * MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(1, 32));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(32, 32));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(21, 32));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNextLevelLowerBound(33, 32));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNextLevelLowerBound(64, 32));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNextLevelLowerBound(55, 32));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(1, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(1024, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNextLevelLowerBound(650, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart_LargerMiddleChunk() {
		assertEquals(1025, MultipartUploadUtils.computeNextLevelLowerBound(1025, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart_LargerMiddleChunk() {
		assertEquals(1025, MultipartUploadUtils.computeNextLevelLowerBound(1500, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart_LargerMiddleChunk() {
		assertEquals(1025, MultipartUploadUtils.computeNextLevelLowerBound(2048, 1024));
	}


	@Test
	public void testGetRangesForPartSinglePart() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(4, 4, 100);

		// Parts will be [1, 1], [2, 2], [3, 3] ... [32, 32]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(32);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLastPart() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(32, 32, 1000);

		// Parts will be [1, 1], [2, 2], [3, 3] ... [32, 32]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(32);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForAdditionalSinglePart() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(33, 33, 33);

		// Parts will be [1, 32], [33, 33]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(33);
		expected.setNumberOfParts(2);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForPartSinglePartWithMaxPartSize() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(4, 4, 13);

		// Should get singleton parts [1, 1], [2, 2], [3, 3] ... [13, 13]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(13);
		expected.setNumberOfParts(13);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForPartSinglePartMiddleChunk() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(71, 71, 200);

		// Should get singleton parts [65, 65], [66, 66], [67, 67] ... [96, 96]
		PartRange expected = new PartRange();
		expected.setLowerBound(65);
		expected.setUpperBound(96);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerFullChunk() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(1, 32, 1024);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1024]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(1024);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(1, 32, 1000);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1000] (note this last part is not full because of the maximum part number)
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(1000);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk_OffByOne() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(1, 32, 992);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [961, 992]
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(992);
		expected.setNumberOfParts(31);

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk_TruncatedChunkIsInput() {
		PartRange actual = MultipartUploadUtils.getRangeOfPotentialStitchTargets(993, 1000, 1000);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1000] (note this last part is not full because of the maximum part number)
		PartRange expected = new PartRange();
		expected.setLowerBound(1);
		expected.setUpperBound(1000);
		expected.setNumberOfParts(32);

		assertEquals(expected, actual);
	}

	@Test
	public void testIsPowerOf32() {
		assertFalse(MultipartUploadUtils.isPowerOf32(0));
		assertTrue(MultipartUploadUtils.isPowerOf32(1));
		assertFalse(MultipartUploadUtils.isPowerOf32(2));
		assertFalse(MultipartUploadUtils.isPowerOf32(4));
		assertFalse(MultipartUploadUtils.isPowerOf32(16));
		assertFalse(MultipartUploadUtils.isPowerOf32(31));
		assertTrue(MultipartUploadUtils.isPowerOf32(32));
		assertFalse(MultipartUploadUtils.isPowerOf32(33));
		assertFalse(MultipartUploadUtils.isPowerOf32(64));
		assertTrue(MultipartUploadUtils.isPowerOf32(1024));
		assertTrue(MultipartUploadUtils.isPowerOf32(32768));
		assertTrue(MultipartUploadUtils.isPowerOf32(1048576));
		assertTrue(MultipartUploadUtils.isPowerOf32(1048576 * 32));
		assertTrue(MultipartUploadUtils.isPowerOf32(1048576 * 32 * 32));
		assertTrue(MultipartUploadUtils.isPowerOf32(1048576L * 32L * 32L * 32L));
		assertFalse(MultipartUploadUtils.isPowerOf32(1048576L * 32L * 32L * 32L + 1));
	}


}