package org.sagebionetworks.athena.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.glue.model.Table;

public class AthenaPartitionScannerWorker implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(AthenaPartitionScannerWorker.class);

	private AthenaSupport athenaSupport;
	private WorkerLogger workerLogger;

	@Autowired
	public AthenaPartitionScannerWorker(AthenaSupport athenaSupport, WorkerLogger workerLogger) {
		this.athenaSupport = athenaSupport;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		LOG.info("Scanning partitions...");

		List<Table> tables = athenaSupport.getPartitionedTables();

		LOG.info("Found {} partitioned tables", tables.size());

		tables.forEach(table -> {
			try {
				athenaSupport.repairTable(table);
			} catch (Throwable e) {
				LOG.error("Could not repair table " + table.getName() + ": " + e.getMessage(), e);
				boolean willRetry = false;
				// Sends a fail metric for cloud watch
				workerLogger.logWorkerFailure(AthenaPartitionScannerWorker.class.getName(), e, willRetry);
			}
		});

		LOG.info("Scanning partitions...DONE");
	}

}
