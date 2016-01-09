package org.sagebionetworks.repo.model.file;

/**
 * Utilities for part calculations for multi-part upload.
 *
 */
public class PartUtils {

	public static final long MAX_NUMBER_OF_PARTS = 10 * 1000; // 10K
	public static final long MIN_PART_SIZE_BYTES = 5 * (long)Math.pow(1024, 2); // 5 MB
	public static final long MAX_FILE_SIZE_BYTES = 5L * (long)Math.pow(1024, 4); // 5 TB
	public static final long MAX_PART_SIZE_BYTES = (long)Integer.MAX_VALUE;
	
	/**
	 * Choose a part size given a file size.
	 * @param fileSizeBytes
	 * @return
	 */
	public static long choosePartSize(long fileSizeBytes){
		validateFileSize(fileSizeBytes);
		long partSizeBytes = Math.max(MIN_PART_SIZE_BYTES, (fileSizeBytes/MAX_NUMBER_OF_PARTS));
		if(partSizeBytes > MAX_PART_SIZE_BYTES){
			// the could only happen if MAX_NUMBER_OF_PARTS or MAX_FILE_SIZE_BYTES are increased.
			throw new IllegalStateException("Part size exceeds maximum integer size");
		}
		return partSizeBytes;
	}

	/**
	 * Calculate the number of parts required to upload file given a part size
	 * and file size.
	 * 
	 * @param fileSizeBytes
	 * @param partSizeBytes
	 * @return
	 */
	public static int calculateNumberOfParts(long fileSizeBytes, long partSizeBytes) {
		validateFileSize(fileSizeBytes);
		if (partSizeBytes < MIN_PART_SIZE_BYTES) {
			throw new IllegalArgumentException("Part size of " + partSizeBytes
					+ " bytes is too small.  The minimum part size is :"
					+ MIN_PART_SIZE_BYTES + " bytes");
		}
		// Only one part is needed when the file is smaller than the part size.
		if (partSizeBytes > fileSizeBytes) {
			return 1;
		}
		int remainder = (int) (fileSizeBytes % partSizeBytes);
		int numberOfParts = ((int) (fileSizeBytes / partSizeBytes))
				+ (remainder > 0 ? 1 : 0);
		// Validate the number of parts.
		if (numberOfParts > MAX_NUMBER_OF_PARTS) {
			throw new IllegalArgumentException(
					"File Upload would required: "
							+ numberOfParts
							+ " parts, which exceeds the maximum number of allowed parts of: "
							+ MAX_NUMBER_OF_PARTS
							+ ". Please choose a larger part size");
		}
		return numberOfParts;
	}
	
	/**
	 * Validate the file size is between 1 byte and 5 TB (inclusive).
	 * @param fileSizeBytes
	 */
	public static void validateFileSize(long fileSizeBytes){
		if(fileSizeBytes < 1){
			throw new IllegalArgumentException("File size must be at least one bytes");
		}
		if(fileSizeBytes > MAX_FILE_SIZE_BYTES){
			throw new IllegalArgumentException("The maximum file size is 5 TB");
		}
	}

}
