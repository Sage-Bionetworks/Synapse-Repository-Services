package org.sagebionetworks.athena.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.glue.model.Table;

public class AthenaPartitionScannerWorker implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(AthenaPartitionScannerWorker.class);

	private AthenaSupport athenaSupport;

	@Autowired
	public AthenaPartitionScannerWorker(AthenaSupport athenaSupport) {
		this.athenaSupport = athenaSupport;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		LOG.info("Scanning partitions...");

		List<Table> tables = athenaSupport.getPartitionedTables();

		LOG.info("Found {} partitioned tables", tables.size());

		tables.forEach(table -> {
			try {
				athenaSupport.repairTable(table);
			} catch (ServiceUnavailableException e) {
				LOG.error("Could not repair table " + table.getName() + ": " + e.getMessage(), e);
			}
		});

		LOG.info("Scanning partitions...DONE");
	}

}
