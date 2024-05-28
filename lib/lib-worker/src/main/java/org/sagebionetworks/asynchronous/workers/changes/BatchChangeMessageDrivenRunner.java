package org.sagebionetworks.asynchronous.workers.changes;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * A change message driven worker that can accept an batch of change messages.
 * 
 * @author jhill
 *
 */
public interface BatchChangeMessageDrivenRunner extends ChangeMessageRunner {
	/**
	 * This method will be called when there is a message to process. The
	 * message will be deleted from the queue after the this call terminates
	 * unless a RecoverableMessageException is thrown.
	 * 
	 * @param progressCallback
	 *            The runner is expected to call progressMade(Message) to notify
	 *            the container that the runner is still working on the message.
	 *            The container will refresh the message visibility timeout if
	 *            needed when progressMade(Message) is called.
	 * @param messages
	 *            List of messages to be processed. The message will be deleted
	 *            from the queue after the this call terminates unless a
	 *            RecoverableMessageException is thrown.
	 * @throws RecoverableMessageException
	 *             The caller is expected to throw a
	 *             {@link RecoverableMessageException} to indicates that the
	 *             given message cannot be processed at this time but it should
	 *             be possible to process it in the future. For example, if a
	 *             runner depends on a services that is currently unavailable,
	 *             then throwing a a {@link RecoverableMessageException} will
	 *             indicates to the container that the message should be
	 *             returned to the queue for future processing.
	 */
	public void run(ProgressCallback progressCallback,
			List<ChangeMessage> messages) throws RecoverableMessageException,
			Exception;
}
