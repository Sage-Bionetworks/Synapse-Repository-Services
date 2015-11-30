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

/**
 * Provides a buffered Iterator over all metadata in a stack.
 * This iterator is state-full and should only be used for one pass.
 * @author John
 *
 */
public class MetadataIterator implements Iterator<RowMetadata> {
	
	static private Log log = LogFactory.getLog(MetadataIterator.class);
	
	MigrationType type;
	SynapseAdminClient client;
	long batchSize;
	Iterator<RowMetadata> lastPageIterator;
	RowMetadataResult lastPage;
	long offset;
	boolean done = false;
	BasicProgress progress;
	
	/**
	 * Create a new iterator that can be used for one pass over the data.
	 * 
	 * @param type - The type of data to iterate over.
	 * @param client - The Synapse client used to get the real data.
	 * @param batchSize - The batch size is the page size of data fetched from a stack.
	 */
	public MetadataIterator(MigrationType type,	SynapseAdminClient client, long batchSize, BasicProgress progress) {
		super();
		this.type = type;
		this.client = client;
		this.batchSize = batchSize;
		this.offset = 0;
		this.done = false;
		this.progress = progress;
	}

	/**
	 * Get the next page with exponential back-off.
	 */
	private boolean getNextPageWithBackupoff() {
		try {
			return getNextPage();
		} catch (Exception e) {
			// When there is a failure wait and try again.
			try {
				log.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" will attempt again in one second", e);
				Thread.sleep(1000);
				return getNextPage();
			} catch (Exception e1) {
				// Try one last time
				try {
					log.warn("Failed to get a page of metadata from client: "+client.getRepoEndpoint()+" for a second time.  Will attempt again in ten seconds", e);
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
		this.lastPage = client.getRowMetadata(type, batchSize, offset);
		long elapse = System.currentTimeMillis()-start;
//		System.out.println("Fetched "+batchSize+" ids in "+elapse+" ms");
		this.offset += this.batchSize;
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
