package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * A basic implementation of the MessageReceiver.
 * @author John
 *
 */
public class MessageReceiverImpl implements MessageReceiver {
	

	static private Log log = LogFactory.getLog(MessageReceiverImpl.class);
	
	@Autowired
	AmazonSQSClient awsSQSClient;
	
    /**
     * The maximum number of threads used to process messages.
     */
    private Integer maxNumberOfWorkerThreads;
    /**
     * The duration (in seconds) that the received messages are hidden from
     * subsequent retrieve requests after being retrieved by a
     * <code>ReceiveMessage</code> request.
     */
    private Integer visibilityTimeout;
    
    /**
     * The MessageQueue that this instance is watching.
     */
    private MessageQueue messageQueue;
	
    /**
     * Providers workers to processes messages.
     */
    private MessageWorkerFactory workerFactory;
	/**
	 * This is our thread pool.
	 */
	ExecutorService executors;
	
	/**
	 * Used for unit tests.
	 * @param awsSQSClient
	 * @param maxNumberOfWorkerThreads
	 * @param visibilityTimeout
	 * @param messageQueue
	 * @param workerFactory
	 */
	public MessageReceiverImpl(AmazonSQSClient awsSQSClient,
			Integer maxNumberOfWorkerThreads, Integer visibilityTimeout,
			MessageQueue messageQueue, MessageWorkerFactory workerFactory) {
		super();
		this.awsSQSClient = awsSQSClient;
		this.maxNumberOfWorkerThreads = maxNumberOfWorkerThreads;
		this.visibilityTimeout = visibilityTimeout;
		this.messageQueue = messageQueue;
		this.workerFactory = workerFactory;
	}
	
	/**
	 * Default used by Spring.
	 */
	public MessageReceiverImpl(){}
	
	/**
	 * Injected by spring or unit tests.
	 * @param awsSQSClient
	 */
	public void setAwsSQSClient(AmazonSQSClient awsSQSClient) {
		this.awsSQSClient = awsSQSClient;
	}


	/**
	 * The maximum number of threads used to process messages.
	 * 
	 * @param maxNumberOfWorkerThreads
	 */
	public void setMaxNumberOfWorkerThreads(Integer maxNumberOfWorkerThreads) {
		this.maxNumberOfWorkerThreads = maxNumberOfWorkerThreads;
	}


	/**
     * The duration (in seconds) that the received messages are hidden from
     * subsequent retrieve requests after being retrieved by a
     * <code>ReceiveMessage</code> request.
	 * @param visibilityTimeout
	 */
	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}


	/**
     * The MessageQueue that this instance is watching.
	 * @param messageQueue
	 */
	public void setMessageQueue(MessageQueue messageQueue) {
		this.messageQueue = messageQueue;
	}


	/**
     * Providers workers to processes messages.
	 * @param workerFactory
	 */
	public void setWorkerFactory(MessageWorkerFactory workerFactory) {
		this.workerFactory = workerFactory;
	}

	@Override
	public void triggerFired() throws InterruptedException{
		// Validate all config.
		verifyConfig();
		// When the timer is fired we receive messages from AWS SQS.
		// Note: The max number of messages is the same as the max number of worker threads used for processing.
		ReceiveMessageResult result = awsSQSClient.receiveMessage(new ReceiveMessageRequest(messageQueue.getQueueUrl()).withMaxNumberOfMessages(maxNumberOfWorkerThreads).withVisibilityTimeout(visibilityTimeout));
		// Process all of the messages.
		List<Future<Message>> currentWorkers = new LinkedList<Future<Message>>();
		for(Message message: result.getMessages()){
			Callable<Message> worker = workerFactory.createWorker(message);
			// Fire up the worker
			Future<Message> future = executors.submit(worker);
			// Add the worker to the list.
			currentWorkers.add(future);
		}
		// Give the workers a chance to start
		Thread.sleep(100);
		// Watch the workers.  As they complete their tasks, delete the messages from the queue.
		long startTime = System.currentTimeMillis();
		long visibilityMs = visibilityTimeout*1000;
		while(currentWorkers.size() > 0){
			long elapase = System.currentTimeMillis()-startTime;
			if(elapase > visibilityMs){
				log.error("Failed to process all messages within the visibilty window.");
				break;
			}
			// Once a worker is done we remove it.
			List<Future<Message>> toRemove = new LinkedList<Future<Message>>();
			// Used to keep track of messages that need to be deleted.
			List<DeleteMessageBatchRequestEntry> messagesToDelete = new LinkedList<DeleteMessageBatchRequestEntry>();
			for(Future<Message> future: currentWorkers){
				if(future.isDone()){
					try {
						Message message = future.get();
						// We processed the message
						messagesToDelete.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));
					} catch (InterruptedException e) {
						// We cannot remove this message from the queue.
						log.error("Failed to process a SQS message:", e);
					} catch (ExecutionException e) {
						// We cannot remove this message from the queue.
						log.error("Failed to process a SQS message:", e);
					}
					// done with this future
					toRemove.add(future);
				}
			}
			// Batch delete all of the completed message.
			if(messagesToDelete.size() > 0){
				awsSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(messageQueue.getQueueUrl(), messagesToDelete));
			}
			// remove all that we can
			currentWorkers.removeAll(toRemove);
			// Give other threads a chance to work
			Thread.yield();
		}
	}


	/**
	 * Validate that we have all of the required configuration.
	 */
	private void verifyConfig() {
		if(awsSQSClient == null) throw new IllegalStateException("awsSQSClient cannot be null");
		if(maxNumberOfWorkerThreads == null) throw new IllegalStateException("maxNumberOfWorkerThreads cannot be null");
		if(visibilityTimeout == null) throw new IllegalStateException("visibilityTimeout cannot be null");
		if(messageQueue == null) throw new IllegalStateException("messageQueue cannot be null");
		if(executors == null){
			// Create the thread pool
			executors = Executors.newFixedThreadPool(maxNumberOfWorkerThreads);
		}
	}

}
