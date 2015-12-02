package org.sagebionetworks.tool.migration.v4;

import java.util.Iterator;

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
	Iterator<RowMetadata> lastPageIterator;
	RowMetadataResult lastPage;
	boolean done = false;
	BasicProgress progress;
	long minId;
	long maxId;
	long batchSize;
	
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
		this.done = false;
		this.progress = progress;
		if (maxId - minId > batchSize) {
			throw new IllegalArgumentException("MaxId-MinId must be less than batchSize");
		}
	}

	// TODO: If maxId-minId < batchSize, should only fetch one page ==> simplify below
	
	/**
	 * Get the next page with exponential back-off.
	 */
	private boolean getNextPageWithBackupoff() {
		try {
			return getNextPage();
		} catch (Exception e) {
			// When there is a failure wait and try again.
			try {
				logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" will attempt again in one second", e);
				Thread.sleep(1000);
				return getNextPage();
			} catch (Exception e1) {
				// Try one last time
				try {
					logger.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" for a second time.  Will attempt again in ten seconds", e);
					Thread.sleep(10000);
					return getNextPage();
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
	private boolean getNextPage() throws SynapseException,	JSONObjectAdapterException {
		long start = System.currentTimeMillis();
		this.lastPage = client.getRowMetadataByRange(type, minId, maxId);
		long elapse = System.currentTimeMillis()-start;
//		System.out.println("Fetched "+batchSize+" ids in "+elapse+" ms");
		this.lastPageIterator = lastPage.getList().iterator();
		this.done = this.lastPage.getList().isEmpty();
		progress.setTotal(lastPage.getTotalCount());
		if(done){
			progress.setDone();
		}
		return done;
	}

	/**
	 * Get the next row metadata
	 * @return Returns non-null as long as there is more data to read. Returns null when there is no more data to read. 
	 */
	public RowMetadata next() {
		if(done) return null;
		progress.setCurrent(progress.getCurrent()+1);
		if(lastPage == null || !lastPageIterator.hasNext()){
			getNextPageWithBackupoff();
			if(done) return null;
		}
		return lastPageIterator.next();
	}

	@Override
	public boolean hasNext() {
		return !done;
	}

	@Override
	public void remove() {
		new UnsupportedOperationException("Not supported");
	}

}
