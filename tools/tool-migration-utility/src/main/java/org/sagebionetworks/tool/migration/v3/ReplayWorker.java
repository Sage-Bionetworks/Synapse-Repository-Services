package org.sagebionetworks.tool.migration.v3;

import java.util.concurrent.Callable;

import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * Replays change messages.
 * 
 * @author jmhill
 *
 */
public class ReplayWorker implements Callable<Long>{
	
	SynapseAdministrationInt destination;
	long startChangeNumber;
	long batchSize;
	BasicProgress progress;
	

	/**
	 * Replay change messages.
	 * @param destination
	 * @param startChangeNumber
	 * @param batchSize
	 */
	public ReplayWorker(SynapseAdministrationInt destination,
			long startChangeNumber, long currentChangeNumber, long batchSize, BasicProgress progress) {
		super();
		this.destination = destination;
		this.startChangeNumber = startChangeNumber;
		this.batchSize = batchSize;
		this.progress = progress;
		this.progress.setCurrent(startChangeNumber);
		this.progress.setTotal(currentChangeNumber);
	}


	@Override
	public Long call() throws Exception {
		long lastChangeNumber = startChangeNumber;
		long count = 0;
		do{
			// Fire a single batch of change messages.
			long startNumber = lastChangeNumber;
			FireMessagesResult result = destination.fireChangeMessages(lastChangeNumber, this.batchSize);
			lastChangeNumber = result.getNextChangeNumber();
			progress.setCurrent(lastChangeNumber);
			if(lastChangeNumber>0){
				count += lastChangeNumber-startNumber;
			}
		}while(lastChangeNumber > 0);
		// Done
		progress.setDone();
		return count;
	}

}
