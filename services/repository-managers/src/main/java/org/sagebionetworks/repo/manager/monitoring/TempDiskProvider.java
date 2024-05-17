package org.sagebionetworks.repo.manager.monitoring;

import java.util.List;

public interface TempDiskProvider {

	/**
	 * Get the percentage of disk spaced used for the drive containing the temporary
	 * directory.
	 * 
	 * @return Will return a number between zero and one.
	 */
	double getDiskSpaceUsedPercent();

	/**
	 * List the name and size of all files in the temporary directory.
	 * 
	 * @return
	 */
	List<FileInfo> listTempFiles();

	/**
	 * Get the name of the temporary directory.
	 * 
	 * @return
	 */
	String getTempDirectoryName();

	/**
	 * Get the ID for this machine.
	 * 
	 * @return
	 */
	String getMachineId();
}
