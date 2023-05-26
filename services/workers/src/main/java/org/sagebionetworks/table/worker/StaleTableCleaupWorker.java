package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class StaleTableCleaupWorker implements ProgressingRunner {
	
	private final Logger log;

	private TableIndexConnectionFactory connectionFactory;
	
	public StaleTableCleaupWorker(TableIndexConnectionFactory connectionFactory, LoggerProvider loggerProvider) {
		this.connectionFactory = connectionFactory;
		this.log = loggerProvider.getLogger(StaleTableCleaupWorker.class.getName());
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		long start = System.currentTimeMillis();
		int deletedTables = connectionFactory.connectToFirstIndex().deleteStaleTables();
		long total = System.currentTimeMillis() - start;
		
		if (deletedTables > 0) {
			log.info("Deleted " + deletedTables + " stale tables (Took: " + total + " ms)");
		}
	}

}
