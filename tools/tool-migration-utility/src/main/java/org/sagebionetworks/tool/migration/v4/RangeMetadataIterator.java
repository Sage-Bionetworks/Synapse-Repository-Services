package org.sagebionetworks.tool.migration.v4;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
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
			return client.getRowMetadataByRange(type, minId, maxId, batchSize, offset).getList();
		} catch (Exception e) {
			// When there is a failure wait and try again.
			try {
				logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" will attempt again in one second", e);
				Thread.sleep(1000);
				return client.getRowMetadataByRange(type, minId, maxId, batchSize, offset).getList();
			} catch (Exception e1) {
				// Try one last time
				try {
					logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" for a second time.  Will attempt again in ten seconds", e);
					Thread.sleep(10000);
					return client.getRowMetadataByRange(type, minId, maxId, batchSize, offset).getList();
				} catch (Exception e2) {
					throw new RuntimeException("Failed to get a page of metadata from "+client.getRepoEndpoint(), e);
				}
			}
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
		logger.info("Type: " + type + ", range [" + minId + ", " + maxId + "]:  Fetching " + batchSize + " rows at offset " + offset + ".");
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

}
