package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;

@FunctionalInterface
public interface FifoQueueParameterProvider {
	
	FifoQueueParameters getParameters(AsynchronousJobStatus jobStatus);
}