package org.sagebionetworks.repo.model.file;

/**
 * Utilities for part calculations for multi-part upload.
 *
 */
public class PartUtils {

	public static final long MAX_NUMBER_OF_PARTS = 10 * 1000; // 10K
	public static final long MIN_PART_SIZE_BYTES = 5 * (long)Math.pow(1024, 2); // 5 MB
	public static final long MAX_PART_SIZE_BYTES = 5L * (long)Math.pow(1024, 3); // 5 GB
	public static final long MAX_FILE_SIZE_BYTES = 5L * (long)Math.pow(1024, 4); // 5 TB
	
	/**
	 * Choose a part size given a file size.
	 * @param fileSizeBytes
	 * @return
	 */
	public static long choosePartSize(long fileSizeBytes){
		validateFileSize(fileSizeBytes);
		long partSizeBytes = Math.max(MIN_PART_SIZE_BYTES, (long)Math.ceil((double)fileSizeBytes/(double)MAX_NUMBER_OF_PARTS));
		validatePartSize(partSizeBytes);
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
		validatePartSize(partSizeBytes);
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
					"File Upload would require "
							+ numberOfParts
							+ " parts, which exceeds the maximum number of allowed parts of: "
							+ MAX_NUMBER_OF_PARTS
							+ ". Please choose a larger part size.");
		}
		return numberOfParts;
	}
	
	/**
	 * Validate the file size is between 1 byte and 5 TB (inclusive).
	 * @param fileSizeBytes
	 */
	public static void validateFileSize(long fileSizeBytes){
		if(fileSizeBytes < 1){
			throw new IllegalArgumentException("File size must be at least one byte");
		}
		if(fileSizeBytes > MAX_FILE_SIZE_BYTES){
			throw new IllegalArgumentException("The maximum file size is 5 TB");
		}
	}
	
	/**
	 * Validate the part size
	 * 
	 * @param partSizeBytes
	 */
	public static void validatePartSize(long partSizeBytes) {
		if (partSizeBytes < MIN_PART_SIZE_BYTES) {
			throw new IllegalArgumentException("The part size must be at least " + MIN_PART_SIZE_BYTES + " bytes.");
		}
		if (partSizeBytes > MAX_PART_SIZE_BYTES) {
			throw new IllegalArgumentException("The part size must not exceed " + MAX_PART_SIZE_BYTES + " bytes.");
		}
	}
	
	/**
	 * Returns the byte range of a part given a part size and the total file size
	 * 
	 * @param partNumber The part number
	 * @param partSizeBytes The part size in bytes
	 * @param fileSizeBytes The total file size in bytes
	 * @return An array of two elements containing the start and end bytes of the range
	 */
	public static long[] getPartRange(long partNumber, long partSizeBytes, long fileSizeBytes) {
		long firstByte = (partNumber - 1) * partSizeBytes;
		
		// The last part might be smaller than partSizeBytes, so check to make sure that lastByte isn't beyond the fileSizeBytes
		long lastByte = Math.min(firstByte + partSizeBytes - 1, fileSizeBytes - 1);
		
		return new long[] {firstByte, lastByte};
	}

}
