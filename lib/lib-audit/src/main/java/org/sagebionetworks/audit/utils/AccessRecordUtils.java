package org.sagebionetworks.audit.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;

public class AccessRecordUtils {


	/**
	 * This Comparator compares AccessRecord based on the time stamp.
	 * 
	 * @author jmhill
	 * 
	 */
	public static class AccessRecordComparator implements
			Comparator<AccessRecord> {
		@Override
		public int compare(AccessRecord one, AccessRecord two) {
			if (one == null)
				throw new IllegalArgumentException("One cannot be null");
			if (one.getTimestamp() == null)
				throw new IllegalArgumentException(
						"One.timestamp cannot be null");
			if (two == null)
				throw new IllegalArgumentException("Two cannot be null");
			if (two.getTimestamp() == null)
				throw new IllegalArgumentException(
						"Two.timestamp cannot be null");
			return one.getTimestamp().compareTo(two.getTimestamp());
		}
	}

	/**
	 * Sort the list of AccessRecord based on timestamp
	 * 
	 * @param toSort
	 */
	public static void sortByTimestamp(List<AccessRecord> toSort) {
		Collections.sort(toSort, new AccessRecordComparator());
	}
}
