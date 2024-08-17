package org.sagebionetworks.workers.util.aws.message;

import java.util.List;

import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

/**
 * Abstraction for a runner that is driven by an AWS SQS Message.
 * 
 */
public interface MessageDrivenRunner {

	/**
	 * This method will be called when there is a message to process. The
	 * message will be deleted from the queue after the this call terminates
	 * unless a RecoverableMessageException is thrown.
	 * 
	 * @param progressCallback
	 *            The runner is expected to call progressMade(null) to notify
	 *            the container that the runner is still working on the message.
	 *            The container will refresh the message visibility timeout if
	 *            needed when progressMade(null) is called.
	 * @param message
	 *            The message to be processed. The message will be deleted from
	 *            the queue after the this call terminates unless a
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
	void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception;
	
	/**
	 * @return The message attribute names to be included in the Message, you can specify a list of
	 *         attribute names to receive, or you can return allof the attributes by specifying All or
	 *         .* in your request. You can also use all messageattributes starting with a prefix, for
	 *         example bar.*. Default is null, e.g. no message attribute is included.
	 */
	default List<String> getMessageAttributeNames() {
		return null;
	}
}
