package org.sagebionetworks.repo.manager.monitoring;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * Monitors the disk space associated with the temporary directory.
 *
 */
public class DiskMonitor {

	private static final int MAX_NUM_FILES_TO_LIST = 10;
	private final TempDiskProvider provider;
	private final Logger logger;
	private final Consumer consumer;
	private final String namespace;

	public DiskMonitor(ApplicationType applicationType, TempDiskProvider provider, LoggerProvider loggerProvider,
			Consumer consumer, String instance) {
		this.provider = provider;
		this.logger = loggerProvider.getLogger(DiskMonitor.class.getName());
		this.consumer = consumer;
		this.namespace = String.format("%s-Disk-%s", StringUtils.capitalize(applicationType.name()), instance);
	}

	public void collectMetrics() {

		double usedPercent = provider.getDiskSpaceUsedPercent();
		
		consumer.addProfileData(new ProfileData().setNamespace(namespace).setName("percentTempDiskSpaceUsed")
				.setValue(Double.valueOf(usedPercent * 100)).setUnit(StandardUnit.Percent.name())
				.setDimension(Collections.singletonMap("machineId", provider.getMachineId())));

		if (usedPercent > 0.9) {

			// gather info on the top 10 largest files in temp dir.
			List<FileInfo> tempFiles = provider.listTempFiles();
			Collections.sort(tempFiles);
			Collections.reverse(tempFiles);
			StringJoiner joiner = new StringJoiner("\n");
			tempFiles.stream().limit(MAX_NUM_FILES_TO_LIST).forEach(i -> joiner.add(i.toString()));

			StringBuilder builder = new StringBuilder(
					String.format("Drive of the temp directory: '%s' is %.2f %% full. Top 10 files by size:\n",
							provider.getTempDirectoryName(), usedPercent * 100));
			builder.append(joiner.toString());
			logger.info(builder.toString());
		}

	}

}
