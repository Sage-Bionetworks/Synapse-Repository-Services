package org.sagebionetworks.upload.multipart;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.upload.PartRange;

public class MultipartUploadUtils {

	protected static final int MAX_PARTS_IN_ONE_COMPOSE = 32;

	/**
	 * Create a part key using the base and part number.
	 *
	 * @param baseKey
	 * @param partNumber
	 * @return
	 */
	public static String createPartKey(String baseKey, long partNumber) {
		return String.format("%1$s/%2$d", baseKey, partNumber);
	}

	/**
	 * Create a part key using the base and part range.
	 *
	 * @param baseKey
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public static String createPartKeyFromRange(String baseKey, long lowerBound, long upperBound) {
		if (lowerBound==upperBound) {
			return createPartKey(baseKey, lowerBound);
		}
		return String.format("%1$s/%2$d-%3$d", baseKey, lowerBound, upperBound);
	}

	public static List<PartRange> getListOfPartRangesToLookFor(long lowerBound, long upperBound, long maxPartNumber) {
		long currentPartSize = upperBound - lowerBound + 1;
		currentPartSize = computeCurrentPartSize(currentPartSize);
		long newPartSize = currentPartSize * MAX_PARTS_IN_ONE_COMPOSE;
		long newLowerBound = computeNewLowerBound(lowerBound, newPartSize);
		long newUpperBound = Math.min(newLowerBound + newPartSize - 1, maxPartNumber);
		List<PartRange> partRanges = new ArrayList<>();
		for (long i = newLowerBound; i <= newUpperBound; i += currentPartSize) {
			partRanges.add(new PartRange(i, Math.min(i + currentPartSize - 1, newUpperBound)));
		}
		return partRanges;
	}

	protected static long computeCurrentPartSize(long currentPartSize) {
		return  (long) Math.pow(MAX_PARTS_IN_ONE_COMPOSE, Math.ceil(log(currentPartSize, MAX_PARTS_IN_ONE_COMPOSE)));
	}

	protected static long computeNewLowerBound(long oldLowerBound, long newPartSize) {
		return (oldLowerBound - 1) / newPartSize * newPartSize + 1;
	}

	private static double log(double value, double base) {
		return Math.log(value)/Math.log(base);
	}

}
