package org.sagebionetworks.tool.migration.v4;

import java.util.Iterator;
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
	
	MigrationType type;
	SynapseAdminClient client;
	Iterator<RowMetadata> pageIterator;
	RowMetadataResult lastPage;
	BasicProgress progress;
	long minId;
	long maxId;
	long batchSize;
	Boolean hasNext;
	long offset;
	boolean done;
	
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
		this.offset = 0;
		this.done = false;
	}

	// TODO: If maxId-minId < batchSize, should only fetch one page ==> simplify below
	
	/**
	 * Get the next page with exponential back-off.
	 */
	private void getNextPageWithBackupoff() {
		try {
			getNextPage();
		} catch (Exception e) {
			// When there is a failure wait and try again.
			try {
				logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" will attempt again in one second", e);
				Thread.sleep(1000);
				getNextPage();
			} catch (Exception e1) {
				// Try one last time
				try {
					logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" for a second time.  Will attempt again in ten seconds", e);
					Thread.sleep(10000);
					getNextPage();
				} catch (Exception e2) {
					throw new RuntimeException("Failed to get a page of metadata from "+client.getRepoEndpoint(), e);
				}
			}
		} 
	}

	/**
	 * Get the next page.
	 * @return Returns true when there is no more data, and false when there is more data to read.
	 * @throws SynapseException
	 * @throws JSONObjectAdapterException
	 */
	private void getNextPage() throws SynapseException, JSONObjectAdapterException {
		long start = System.currentTimeMillis();
		this.lastPage = client.getRowMetadataByRange(type, minId, maxId, batchSize, offset);
		long elapse = System.currentTimeMillis()-start;
		System.out.println("Fetched "+batchSize+" ids at offset "+offset+" for minId "+minId+" and maxId "+maxId+" in "+elapse+" ms.");
		this.offset += this.batchSize;
		this.pageIterator = lastPage.getList().iterator();
		this.done = this.lastPage.getList().isEmpty();
		progress.setTotal(maxId-minId);
		return;
	}

	/**
	 * Get the next row metadata
	 * @return Returns non-null as long as there is more data to read. Throws NoSuchElementException when there is no more data to read. 
	 */
	@Override
	public RowMetadata next() {
		if (this.hasNext == null) {
			throw new IllegalStateException("HasNext() must be called prior to any call to next().");
		}

		if (! this.hasNext) {
			throw new NoSuchElementException();
		}

		progress.setCurrent(progress.getCurrent()+1);
		RowMetadata res = this.pageIterator.next();
		this.hasNext = null;
		return res;
	}

	@Override
	public boolean hasNext() {
		// If the page iterator is empty, we're done
		if (this.done) {
			this.hasNext = false;
			this.progress.setDone();
		} else {
			// First time
			if (lastPage == null) {
				getNextPageWithBackupoff();
			}
			this.hasNext = pageIterator.hasNext();
			// En of page iterator
			if (! this.hasNext) {
				getNextPageWithBackupoff();
				if (this.done) {
					this.hasNext = false;
					this.progress.setDone();
				} else {
					this.hasNext = pageIterator.hasNext();
				}
			}
		}
		return this.hasNext;
	}

	@Override
	public void remove() {
		new UnsupportedOperationException("Not supported");
	}

}
