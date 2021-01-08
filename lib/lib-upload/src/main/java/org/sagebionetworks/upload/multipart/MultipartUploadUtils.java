package org.sagebionetworks.upload.multipart;

import java.math.RoundingMode;

import org.sagebionetworks.repo.model.upload.PartRange;

import com.google.common.math.LongMath;

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


	/**
	 * This method determines, for a given part, which parts must already be uploaded in order to stitch the parts
	 * together in Google Cloud. This method does <b>not</b> verify which parts have been uploaded.
	 * @param lowerBound the lower bound of the part that has been uploaded
	 * @param upperBound the upper bound of the part that has been uploaded
	 * @param maxPartNumber the highest part number in the upload. i.e. the number of parts the user will upload
	 *                      for the entire file.
	 * @return a part range that defines the lower bound, upper bound, and expected number of parts between the bounds
	 * that must be uploaded to proceed with stitching the parts.
	 */
	public static PartRange getRangeOfPotentialStitchTargets(long lowerBound, long upperBound, long maxPartNumber) {
		PartRange rangeOfStitchTargets = new PartRange();

		long stitchTargetSize = computeStitchTargetSize(lowerBound, upperBound, maxPartNumber);
		long nextLevelSize = stitchTargetSize * MAX_PARTS_IN_ONE_COMPOSE;

		rangeOfStitchTargets.setLowerBound(
				computeNextLevelLowerBound(lowerBound, nextLevelSize)
		);

		rangeOfStitchTargets.setUpperBound(
				Math.min(rangeOfStitchTargets.getLowerBound() + nextLevelSize - 1, maxPartNumber)
		);

		/*
		 * The number of parts needed to create the next level is the next level's part size
		 *  divided by the stitch target size, rounded up.
		 */
		rangeOfStitchTargets.setNumberOfParts(
				LongMath.divide(
						rangeOfStitchTargets.getUpperBound() - rangeOfStitchTargets.getLowerBound() + 1,
						stitchTargetSize,
						RoundingMode.CEILING
				)
		);
		return rangeOfStitchTargets;
	}

	/**
	 * Computes the size (in number of parts) of other parts to stitch to.
	 * This is usually the size of the part passed in, but not always the case for parts at the end boundary.
	 * @param lowerBound The lower bound of a given part
	 * @param upperBound The upper bound of a given part
	 * @return The size of the other parts that the given part should be stitched to. This size is necessarily
	 * greater than or equal to the size of the given part.
	 */
	protected static long computeStitchTargetSize(long lowerBound, long upperBound, long totalNumberOfParts) {
		long partSize = upperBound - lowerBound + 1;
		if (upperBound == totalNumberOfParts && isPowerOf32(lowerBound - 1)) {
			// Handles edge case
			// Consider a 33-part upload with compose size of 32
			// The 33rd part has size 1 but has no stitch targets of size == 1
			// The part should be combined with [1 - 32], giving a stitch target size of 32.
			return (long) Math.pow(MAX_PARTS_IN_ONE_COMPOSE, Math.floor(log(lowerBound, MAX_PARTS_IN_ONE_COMPOSE)));
		} else {
			// In all other cases, we just round up to the nearest power of 32.
			return (long) Math.pow(MAX_PARTS_IN_ONE_COMPOSE, Math.ceil(log(partSize, MAX_PARTS_IN_ONE_COMPOSE)));
		}
	}

	protected static boolean isPowerOf32(long x) {
		// Adapted from https://kuaiyumath.wordpress.com/2015/12/28/check-if-an-integer-is-power-of-4-using-bit-operations/
		long mask = 0x1084210842108421L; // 0b1000010000...100001 - this captures bits that represent 32^0, 32^1 ... 32^12 (long overflow past that point)
		return ((x & (x - 1)) == 0) && // isPowerOf2 and
				(x & mask) > 0; // contains a bit that represents 32^n
	}

	/**
	 * Computes the lower boundary of the part that is the "next level" up.
	 * @param oldLowerBound
	 * @param newPartSize
	 * @return
	 */
	protected static long computeNextLevelLowerBound(long oldLowerBound, long newPartSize) {
		return (oldLowerBound - 1) - ((oldLowerBound - 1) % newPartSize) + 1;
	}

	private static double log(double value, double base) {
		// log_x(a) / log_x(b) = log_b(a)
		return Math.log(value) / Math.log(base);
	}
}
