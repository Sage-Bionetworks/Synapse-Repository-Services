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
	Iterator<RowMetadata> rangeIterator;
	RowMetadataResult range;
	BasicProgress progress;
	long minId;
	long maxId;
	long batchSize;
	Boolean hasNext;
	
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
		if (maxId - minId > batchSize) {
			throw new IllegalArgumentException("MaxId-MinId must be less than batchSize");
		}
	}

	// TODO: If maxId-minId < batchSize, should only fetch one page ==> simplify below
	
	/**
	 * Get the next page with exponential back-off.
	 */
	private void getRangeWithBackupoff() {
		try {
			getRange();
		} catch (Exception e) {
			// When there is a failure wait and try again.
			try {
				logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" will attempt again in one second", e);
				Thread.sleep(1000);
				getRange();
			} catch (Exception e1) {
				// Try one last time
				try {
					logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" for a second time.  Will attempt again in ten seconds", e);
					Thread.sleep(10000);
					getRange();
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
	private void getRange() throws SynapseException, JSONObjectAdapterException {
		long start = System.currentTimeMillis();
		this.range = client.getRowMetadataByRange(type, minId, maxId);
		long elapse = System.currentTimeMillis()-start;
//		System.out.println("Fetched "+batchSize+" ids in "+elapse+" ms");
		this.rangeIterator = range.getList().iterator();
//		this.done = this.lastPage.getList().isEmpty();
		progress.setTotal(maxId-minId);
		return;
	}

	/**
	 * Get the next row metadata
	 * @return Returns non-null as long as there is more data to read. Throws NoSuchElementException when there is no more data to read. 
	 */
	public RowMetadata next() {
		if (this.hasNext == null) {
			this.hasNext();
		}

		if (! this.hasNext) {
			throw new NoSuchElementException();
		}

		progress.setCurrent(progress.getCurrent()+1);
		RowMetadata res = this.rangeIterator.next();
		this.hasNext = null;
		return res;
	}

	@Override
	public boolean hasNext() {
		if (range == null){
			getRangeWithBackupoff();
		}
		this.hasNext = rangeIterator.hasNext();
		if (! this.hasNext) {
			this.progress.setDone();
		}
		return this.hasNext;
	}

	@Override
	public void remove() {
		new UnsupportedOperationException("Not supported");
	}

}
