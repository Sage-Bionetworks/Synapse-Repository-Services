package org.sagebionetworks.repo.model.ar;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Comparator to rank RestrictionLevel from most restrictive to least
 * restrictive.
 *
 */
public class RestrictionLevelComparator implements Comparator<RestrictionLevel> {

	public static final RestrictionLevelComparator SINGLETON = new RestrictionLevelComparator();

	private static final Map<RestrictionLevel, Integer> MAP;
	static {
		MAP = new HashMap<RestrictionLevel, Integer>(3);
		MAP.put(RestrictionLevel.CONTROLLED_BY_ACT, 3);
		MAP.put(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, 2);
		MAP.put(RestrictionLevel.OPEN, 1);
	}

	@Override
	public int compare(RestrictionLevel left, RestrictionLevel right) {
		ValidateArgument.required(left, "left");
		ValidateArgument.required(right, "right");
		Integer leftValue = MAP.get(left);
		if (leftValue == null) {
			throw new IllegalStateException("Unknown type: " + left.name());
		}
		Integer rightValue = MAP.get(right);
		if (rightValue == null) {
			throw new IllegalStateException("Unknown type: " + right.name());
		}
		return leftValue.compareTo(rightValue);
	}

}
