package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;

@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "Used for backfilling the table row change")
public class TableRowChangeBackfillWorker implements ProgressingRunner {
	
	private static final Logger LOG = LogManager.getLogger(TableRowChangeBackfillWorker.class);
	
	static final long BATCH_SIZE = 10_000;

	private TableEntityManager tableEntityManager;

	@Autowired
	public TableRowChangeBackfillWorker(TableEntityManager tableEntityManager) {
		this.tableEntityManager = tableEntityManager;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			tableEntityManager.backfillTableRowChangesBatch(BATCH_SIZE);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
		}
		
	}

}
