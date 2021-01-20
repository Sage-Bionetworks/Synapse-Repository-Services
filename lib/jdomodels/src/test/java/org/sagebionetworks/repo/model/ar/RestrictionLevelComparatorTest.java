package org.sagebionetworks.repo.model.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RestrictionLevel;

import static org.sagebionetworks.repo.model.RestrictionLevel.*;

import com.google.common.collect.Lists;

public class RestrictionLevelComparatorTest {

	@Test
	public void testCompareAsRank() {
		List<RestrictionLevel> list = Lists.newArrayList(OPEN, RESTRICTED_BY_TERMS_OF_USE, CONTROLLED_BY_ACT, OPEN,
				RESTRICTED_BY_TERMS_OF_USE, CONTROLLED_BY_ACT);
		// call under test
		Collections.sort(list, RestrictionLevelComparator.SINGLETON);
		List<RestrictionLevel> expected = Lists.newArrayList(OPEN, OPEN, RESTRICTED_BY_TERMS_OF_USE,
				RESTRICTED_BY_TERMS_OF_USE, CONTROLLED_BY_ACT, CONTROLLED_BY_ACT);
		assertEquals(expected, list);
	}

	@Test
	public void testCompareAllCombinations() {
		RestrictionLevelComparator comparator = new RestrictionLevelComparator();
		assertEquals(0, comparator.compare(CONTROLLED_BY_ACT, CONTROLLED_BY_ACT));
		assertEquals(1, comparator.compare(CONTROLLED_BY_ACT, RESTRICTED_BY_TERMS_OF_USE));
		assertEquals(1, comparator.compare(CONTROLLED_BY_ACT, OPEN));

		assertEquals(-1, comparator.compare(RESTRICTED_BY_TERMS_OF_USE, CONTROLLED_BY_ACT));
		assertEquals(0, comparator.compare(RESTRICTED_BY_TERMS_OF_USE, RESTRICTED_BY_TERMS_OF_USE));
		assertEquals(1, comparator.compare(RESTRICTED_BY_TERMS_OF_USE, OPEN));

		assertEquals(-1, comparator.compare(OPEN, CONTROLLED_BY_ACT));
		assertEquals(-1, comparator.compare(OPEN, RESTRICTED_BY_TERMS_OF_USE));
		assertEquals(0, comparator.compare(OPEN, OPEN));
	}

	@Test
	public void testCompareWithNullLeft() {
		RestrictionLevelComparator comparator = new RestrictionLevelComparator();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			comparator.compare(null, OPEN);
		}).getMessage();
		assertEquals("left is required.", message);
	}

	@Test
	public void testCompareWithNullRight() {
		RestrictionLevelComparator comparator = new RestrictionLevelComparator();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			comparator.compare(OPEN, null);
		}).getMessage();
		assertEquals("right is required.", message);
	}

	/**
	 * This test should fail if a new type is added and the comparator is not
	 * extended.
	 */
	@Test
	public void testCompairEachType() {
		for (RestrictionLevel level : RestrictionLevel.values()) {
			assertEquals(0, RestrictionLevelComparator.SINGLETON.compare(level, level));
		}
	}

}
