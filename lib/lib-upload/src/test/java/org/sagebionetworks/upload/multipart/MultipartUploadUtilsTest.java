package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.ArrayList;
import java.util.List;

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
	public void testGetRangesForPartSinglePart() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(4, 4, 100);

		// Should get singleton parts [1, 1], [2, 2], [3, 3] ... [32, 32]
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i <=32; i++) { // Parts are 1-indexed
			expected.add(new PartRange(i, i));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testComputeCurrentPartSizeSingletonCase() {
		assertEquals(1L, MultipartUploadUtils.computeCurrentPartSize(1L));
	}

	@Test
	public void testComputeCurrentPartSizeIncompletePartCase() {
		assumeTrue(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE > 2);
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE, MultipartUploadUtils.computeCurrentPartSize(2L));
	}

	@Test
	public void testComputeCurrentPartSizeCompletePartCase() {
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE,
				MultipartUploadUtils.computeCurrentPartSize(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE));
	}

	@Test
	public void testComputeCurrentPartSize_NextSizeUp() {
		// The part size should square when above the max
		assertEquals(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE * MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE,
				MultipartUploadUtils.computeCurrentPartSize(MultipartUploadUtils.MAX_PARTS_IN_ONE_COMPOSE + 1));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(1, 32));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(32, 32));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(21, 32));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNewLowerBound(33, 32));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNewLowerBound(64, 32));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart_MiddleChunk() {
		assertEquals(33, MultipartUploadUtils.computeNewLowerBound(55, 32));
	}

	@Test
	public void testComputeNewLowerBoundForFirstPart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(1, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForLastPart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(1024, 1024));
	}

	@Test
	public void testComputeNewLowerBoundForMiddlePart_LargerChunk() {
		assertEquals(1, MultipartUploadUtils.computeNewLowerBound(650, 1024));
	}

	@Test
	public void testGetRangesForLastPart() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(32, 32, 1000);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1000] (note this last part is not full because of the maximum part number)
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i <= 32; i++) {
			expected.add(new PartRange(i, i));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForPartSinglePartWithMaxPartSize() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(4, 4, 13);

		// Should get singleton parts [1, 1], [2, 2], [3, 3] ... [13, 13]
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i <=13; i++) { // Parts are 1-indexed
			expected.add(new PartRange(i, i));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForPartSinglePartMiddleChunk() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(71, 71, 200);

		// Should get singleton parts [65, 65], [66, 66], [67, 67] ... [96, 96]
		List<PartRange> expected = new ArrayList<>();
		for (int i = 65; i <=96; i++) {
			expected.add(new PartRange(i, i));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerFullChunk() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(1, 32, 1024);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1024]
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i < 1024; i += 32) {
			expected.add(new PartRange(i, i + 31));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(1, 32, 1000);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1000] (note this last part is not full because of the maximum part number)
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i < 993; i += 32) {
			expected.add(new PartRange(i, i + 31));
		}
		// Add the last expected part
		expected.add(new PartRange(993, 1000));

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk_OffByOne() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(1, 32, 992);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [961, 992]
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i < 993; i += 32) {
			expected.add(new PartRange(i, i + 31));
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testGetRangesForLargerTruncatedChunk_TruncatedChunkIsInput() {
		List<PartRange> actual = MultipartUploadUtils.getListOfPartRangesToLookFor(993, 1000, 1000);

		// Should get parts [1, 32], [33, 64], [65, 96] ... [993, 1000] (note this last part is not full because of the maximum part number)
		List<PartRange> expected = new ArrayList<>();
		for (int i = 1; i < 993; i += 32) {
			expected.add(new PartRange(i, i + 31));
		}
		// Add the last expected part
		expected.add(new PartRange(993, 1000));

		assertEquals(expected, actual);
	}


}