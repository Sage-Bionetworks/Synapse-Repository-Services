package org.sagebionetworks.repo.model.ar;

import java.util.Comparator;

import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Comparator to rank RestrictionLevel from most restrictive to least restrictive.
 *
 */
public class RestrictionLevelComparator implements Comparator<RestrictionLevel> {

	@Override
	public int compare(RestrictionLevel left, RestrictionLevel right) {
		ValidateArgument.required(left, "left");
		ValidateArgument.required(right, "right");
		if(left == right) {
			return 0;
		}
		if(RestrictionLevel.CONTROLLED_BY_ACT == left) {
			return 1;
		}
		if(RestrictionLevel.CONTROLLED_BY_ACT == right) {
			return -1;
		}
		if(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE == left) {
			return 1;
		}else {
			return -1;
		}
	}

}
