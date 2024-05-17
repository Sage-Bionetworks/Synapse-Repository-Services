package org.sagebionetworks.repo.manager.monitoring;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.util.VirtualMachineIdProvider;

public class TempDiskProviderImpl implements TempDiskProvider {

	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

	/**
	 * Get the percentage of disk spaced used for the drive containing the temporary
	 * directory.
	 * 
	 * @return Will return a number between zero and one.
	 */
	public double getDiskSpaceUsedPercent() {
		File tempDir = new File(TEMP_DIR);
		return  1.0 - ((double)tempDir.getFreeSpace() / (double)tempDir.getTotalSpace());
	}

	/**
	 * List the name and size of all files in the temporary directory.
	 * 
	 * @return
	 */
	public List<FileInfo> listTempFiles() {
		File tempDir = new File(TEMP_DIR);
		return Stream.of(tempDir.listFiles()).filter(file -> !file.isDirectory())
				.map(file -> new FileInfo(file.length(), file.getName())).collect(Collectors.toList());
	}

	/**
	 * Get the name of the temporary directory.
	 * 
	 * @return
	 */
	public String getTempDirectoryName() {
		return TEMP_DIR;
	}

	@Override
	public String getMachineId() {
		return VirtualMachineIdProvider.getVMID();
	}

}
