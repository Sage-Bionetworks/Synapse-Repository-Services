package org.sagebionetworks.asynchronous.workers.remote;

import org.sagebionetworks.common.util.progress.ProgressCallback;

/**
 * Provider for the message body to send to SQS
 * 
 * @author Marco Marasca
 *
 */
public interface SQSMessageProvider {

	/**
	 * When the {@link RemoteTriggerRunner} fires this method will be invoked to fetch the body of the message that
	 * will be sent to SQS
	 * 
	 * @param progressCallback
	 * @return The message body for the SQS message to send
	 */
	String getMessageBody(ProgressCallback progressCallback);

}
