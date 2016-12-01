package org.sagebionetworks.tool.migration.v5;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.AdminResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.progress.BasicProgress;

public class RangeMetadataIterator implements Iterator<RowMetadata> {
	
	static private Log logger = LogFactory.getLog(RangeMetadataIterator.class);
	
	final MigrationType type;
	final SynapseAdminClient client;
	final BasicProgress progress;
	final long minId;
	final long maxId;
	final long batchSize;

	// Paging state
	long offset;
	Iterator<RowMetadata> currentIterator;
	boolean isLastPage;

	/**
	 * Create a new iterator that can be used for one pass over a range of the data.
	 * 
	 * @param type - The type of data to iterate over.
	 * @param client - The Synapse client used to get the real data.
	 * @param minId - The start ID of the range
	 * @param maxId - The ending ID of the range
	 */
	public RangeMetadataIterator(MigrationType type, SynapseAdminClient client, long batchSize, long minId, long maxId, BasicProgress progress) {
		super();
		this.type = type;
		this.client = client;
		this.minId = minId;
		this.maxId = maxId;
		this.batchSize = batchSize;
		this.progress = progress;
		this.progress.setCurrent(0);
		currentIterator = null;
		offset = 0;
		isLastPage = false;
	}

	// TODO: If maxId-minId < batchSize, should only fetch one page ==> simplify below
	
	/**
	 * Get the next page with exponential back-off.
	 */
	private List<RowMetadata> getNextPageWithBackupoff(MigrationType type, long minId, long maxId, long batchSize, long offset) {
		try {
			return getRowMetadataByRange(client, type, minId, maxId, batchSize, offset);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get a page of metadata from "+client.getRepoEndpoint(), e);
		} 
	}

	/**
	 * Get the next row metadata
	 * @return Returns non-null as long as there is more data to read. Throws NoSuchElementException when there is no more data to read. 
	 */
	@Override
	public RowMetadata next() {
		if (this.currentIterator == null) {
			throw new IllegalStateException("Must call hasNext() before calling next().");
		}
		progress.setCurrent(progress.getCurrent()+1);
		return this.currentIterator.next();
	}

	@Override
	public boolean hasNext() {
		if (currentIterator == null) {
			// First call
			currentIterator = fetchNextPage();
		}
		if (currentIterator.hasNext()) {
			// Current page still has data
			return true;
		} else if (isLastPage) {
			// Current page is out of data and no more pages
			progress.setDone();
			return false;
		} else {
			// Current page out of data but there might be more pages
			currentIterator = fetchNextPage();
		}
		return currentIterator.hasNext();
		
	}
	
	/**
	 * Fetch the next page from the server, tracks the last page state and increment offset
	 * @return
	 */
	private Iterator<RowMetadata> fetchNextPage() {
		logger.info("Getting data for type: " + type + " , fetching rows at offset " + offset + ".");
		List<RowMetadata> page = this.getNextPageWithBackupoff(type, minId, maxId, batchSize, offset);
		offset += batchSize;
		if (page.size() < batchSize) {
			// Last page
			isLastPage = true;
		}
		return page.iterator();
	}

	@Override
	public void remove() {
		new UnsupportedOperationException("Not supported");
	}

	protected List<RowMetadata> getRowMetadataByRange(SynapseAdminClient conn, MigrationType type, Long minId, Long maxId, Long batchSize, Long offset) throws SynapseException, InterruptedException, JSONObjectAdapterException {
		RowMetadataResult res = null;
		try {
			res = conn.getRowMetadataByRange(type, minId, maxId, batchSize, offset);
			return res.getList();
		} catch (SynapseException e) {
			AsyncMigrationRowMetadataRequest req = new AsyncMigrationRowMetadataRequest();
			req.setType(type.name());
			req.setMinId(minId);
			req.setMaxId(maxId);
			req.setLimit(batchSize);
			req.setOffset(offset);
			BasicProgress progress = new BasicProgress();
			AsyncMigrationWorker worker = new AsyncMigrationWorker(conn, req, 900000, progress);
			AdminResponse resp = worker.call();
			res = (RowMetadataResult)resp;
			return res.getList();
		}
	}
}
