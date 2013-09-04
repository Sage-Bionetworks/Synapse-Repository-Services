package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	

	static private Logger log = LogManager.getLogger(MessageReceiverImpl.class);
	
	@Autowired
	AmazonSQSClient awsSQSClient;
	
    /**
     * The maximum number of threads used to process messages.
     */
    private Integer maxNumberOfWorkerThreads;
    
    /**
     * The maximum number of messages that each worker should handle.
     */
    private Integer maxMessagePerWorker;
    /**
     * The duration (in seconds) that the received messages are hidden from
     * subsequent retrieve requests after being retrieved by a
     * <code>ReceiveMessage</code> request.
     */
    private Integer visibilityTimeoutSec;
    
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
			Integer maxNumberOfWorkerThreads, Integer maxMessagePerWorker, Integer visibilityTimeout,
			MessageQueue messageQueue, MessageWorkerFactory workerFactory) {
		super();
		this.awsSQSClient = awsSQSClient;
		this.maxNumberOfWorkerThreads = maxNumberOfWorkerThreads;
		this.maxMessagePerWorker = maxMessagePerWorker;
		this.visibilityTimeoutSec = visibilityTimeout;
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
	public void setVisibilityTimeoutSec(Integer visibilityTimeout) {
		this.visibilityTimeoutSec = visibilityTimeout;
	}


	/**
     * The MessageQueue that this instance is watching.
	 * @param messageQueue
	 */
	public void setMessageQueue(MessageQueue messageQueue) {
		this.messageQueue = messageQueue;
	}


	/**
     * The maximum number of messages that each worker should handle.
	 * @return
	 */
	public Integer getMaxMessagePerWorker() {
		return maxMessagePerWorker;
	}

	/**
     * The maximum number of messages that each worker should handle.
	 * @param maxMessagePerWorker
	 */
	public void setMaxMessagePerWorker(Integer maxMessagePerWorker) {
		this.maxMessagePerWorker = maxMessagePerWorker;
	}

	/**
     * The maximum number of threads used to process messages.
	 * @return
	 */
	public Integer getMaxNumberOfWorkerThreads() {
		return maxNumberOfWorkerThreads;
	}

	/**
     * Providers workers to processes messages.
	 * @param workerFactory
	 */
	public void setWorkerFactory(MessageWorkerFactory workerFactory) {
		this.workerFactory = workerFactory;
	}
	
	@Override
	public void run(){
		try {
			triggerFired();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int triggerFired() throws InterruptedException{
		try{
			if(log.isTraceEnabled()){
				log.trace("Starting trigger...");
			}
			long start = System.currentTimeMillis();
			int count =triggerFiredImpl();
			long elapse = System.currentTimeMillis()-start;
			if(log.isTraceEnabled()){
				log.trace("Finished trigger in "+elapse+" ms");
			}
			return count;
		}catch (Throwable e){
			log.error("Trigger fired failed", e);
			// We only want to throw a runtime.
			RuntimeException runtime = null;
			if(e instanceof RuntimeException){
				runtime = (RuntimeException) e;
			}else{
				runtime = new RuntimeException(e);
			}
			throw runtime;
		}
	}

	private int triggerFiredImpl() throws InterruptedException {
		// Validate all config.
		verifyConfig();
		// When the timer is fired we receive messages from AWS SQS.
		// Note: The max number of messages is the maxNumberOfWorkerThreads*maxMessagePerWorker as each worker is expected to handle a batch of messages.
		int maxMessages = maxNumberOfWorkerThreads*maxMessagePerWorker;
		ReceiveMessageResult result = awsSQSClient.receiveMessage(new ReceiveMessageRequest(messageQueue.getQueueUrl()).withMaxNumberOfMessages(maxMessages).withVisibilityTimeout(visibilityTimeoutSec));
		if(result.getMessages().size() < 1) return 0;
		// Process all of the messages.
		List<Future<List<Message>>> currentWorkers = new LinkedList<Future<List<Message>>>();
		List<Message> messageBatch = new LinkedList<Message>();
		for(Message message: result.getMessages()){
			// Add this message to a batch
			messageBatch.add(message);
			if(messageBatch.size() >= maxMessagePerWorker){
				Callable<List<Message>> worker = workerFactory.createWorker(messageBatch);
				// Fire up the worker
				Future<List<Message>> future = executors.submit(worker);
				// Add the worker to the list.
				currentWorkers.add(future);
				// Create a new batch
				messageBatch = new LinkedList<Message>();
			}
		}
		// Add any dangling messages
		if(messageBatch.size() > 0){
			Callable<List<Message>> worker = workerFactory.createWorker(messageBatch);
			// Fire up the worker
			Future<List<Message>> future = executors.submit(worker);
			// Add the worker to the list.
			currentWorkers.add(future);
		}
		// Give the workers a chance to start
		Thread.sleep(100);
		// Watch the workers.  As they complete their tasks, delete the messages from the queue.
		long startTime = System.currentTimeMillis();
		long visibilityMs = visibilityTimeoutSec*1000;
		while(currentWorkers.size() > 0){
			long elapase = System.currentTimeMillis()-startTime;
			if(elapase > visibilityMs){
				log.error("Failed to process all messages within the visibilty window.");
				break;
			}
			// Once a worker is done we remove it.
			List<Future<List<Message>>> toRemove = new LinkedList<Future<List<Message>>>();
			// Used to keep track of messages that need to be deleted.
			List<DeleteMessageBatchRequestEntry> messagesToDelete = new LinkedList<DeleteMessageBatchRequestEntry>();
			for(Future<List<Message>> future: currentWorkers){
				if(future.isDone()){
					try {
						List<Message> messages = future.get();
						// We processed the message
						for(Message toDelet: messages){
							messagesToDelete.add(new DeleteMessageBatchRequestEntry(toDelet.getMessageId(), toDelet.getReceiptHandle()));
						}
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
		// Return the number of messages that were on the queue.
		return result.getMessages().size();
	}


	/**
	 * Validate that we have all of the required configuration.
	 */
	private void verifyConfig() {
		if(awsSQSClient == null) throw new IllegalStateException("awsSQSClient cannot be null");
		if(maxNumberOfWorkerThreads == null) throw new IllegalStateException("maxNumberOfWorkerThreads cannot be null");
		if(visibilityTimeoutSec == null) throw new IllegalStateException("visibilityTimeout cannot be null");
		if(messageQueue == null) throw new IllegalStateException("messageQueue cannot be null");
		if(executors == null){
			// Create the thread pool
			executors = Executors.newFixedThreadPool(maxNumberOfWorkerThreads);
		}
	}

}
