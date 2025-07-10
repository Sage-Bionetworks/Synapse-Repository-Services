package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PatchUtilsTest {

	@Test
	public void testPlusTenPercent() {
		// call under test
		assertEquals(110L, PatchUtils.plusTenPerent(100L));
	}

	@Test
	public void testCalculateRowsPerPatchWithMax() {
		Long maxRowSizeBytes = Long.MAX_VALUE;
		// call under test
		assertEquals(1, PatchUtils.calculateRowsPerPatch(maxRowSizeBytes));
	}

	@Test
	public void testCalculateRowsPerPatchWithMaxAtLimit() {
		Long maxRowSizeBytes = PatchUtils.MAX_BYTES_PER_PATCH;
		// call under test
		assertEquals(1, PatchUtils.calculateRowsPerPatch(maxRowSizeBytes));
	}

	@Test
	public void testCalculateRowsPerPatchWithMaxOverLimit() {
		Long maxRowSizeBytes = PatchUtils.MAX_BYTES_PER_PATCH + 1L;
		// call under test
		assertEquals(1, PatchUtils.calculateRowsPerPatch(maxRowSizeBytes));
	}
	
	@Test
	public void testCalculateRowsPerPatchWithMaxUnderLimit() {
		Long maxRowSizeBytes = PatchUtils.MAX_BYTES_PER_PATCH - 1L;
		// call under test
		assertEquals(1, PatchUtils.calculateRowsPerPatch(maxRowSizeBytes));
	}
	
	@Test
	public void testCalculateRowsPerPatchWithHalf() {
		// half - 10% should be two rows.
		Long maxRowSizeBytes = (PatchUtils.MAX_BYTES_PER_PATCH/2)-Double.valueOf(PatchUtils.MAX_BYTES_PER_PATCH * 0.1).longValue();
		// call under test
		assertEquals(2, PatchUtils.calculateRowsPerPatch(maxRowSizeBytes));
	}
	
	@Test
	public void testCalculateRowsPerPatch() {
		// call under test with maxRowsSizeBytes=
		assertEquals(128000, PatchUtils.calculateRowsPerPatch(1L));
		assertEquals(11636, PatchUtils.calculateRowsPerPatch(10L));
		assertEquals(1163, PatchUtils.calculateRowsPerPatch(100L));
		assertEquals(116, PatchUtils.calculateRowsPerPatch(1_000L));
		assertEquals(11, PatchUtils.calculateRowsPerPatch(10_000L));
		assertEquals(1, PatchUtils.calculateRowsPerPatch(100_000L));
	}
}
