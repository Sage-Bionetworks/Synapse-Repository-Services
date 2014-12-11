package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;

/**
 * A basic implementation of the MessageReceiver.
 * @author John
 *
 */
public class MessageReceiverImpl implements MessageReceiver {
	

	static private Logger log = LogManager.getLogger(MessageReceiverImpl.class);
	
	/**
	 * The maximum number of seconds that a message visibility timeout can be set to.
	 * Note: This does not mean a worker cannot run for more than this time.
	 * The message receiver will automatically refresh the timeout for any message
	 * that reaches its timeout half-life.
	 * By keeping the visibility timeout small we can quickly recover from failures.
	 */
	public static final int MAX_VISIBILITY_TIMEOUT_SECS = 60;
	
	@Autowired
	QueueServiceDao sqsDao;
	
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
	public MessageReceiverImpl(QueueServiceDao sqsDao,
			Integer maxNumberOfWorkerThreads, Integer maxMessagePerWorker, Integer visibilityTimeout,
			MessageQueue messageQueue, MessageWorkerFactory workerFactory) {
		super();
		this.sqsDao = sqsDao;
		this.maxNumberOfWorkerThreads = maxNumberOfWorkerThreads;
		this.maxMessagePerWorker = maxMessagePerWorker;
		setVisibilityTimeoutSec(visibilityTimeout);
		this.messageQueue = messageQueue;
		this.workerFactory = workerFactory;
	}
	
	/**
	 * Default used by Spring.
	 */
	public MessageReceiverImpl(){}
	

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
// Will put this back after all workers can be ajusted.
//		if(visibilityTimeout > MAX_VISIBILITY_TIMEOUT_SECS) {
//			throw new IllegalArgumentException("Visibility Timeout Seconds cannot exceed: "+MAX_VISIBILITY_TIMEOUT_SECS+" seconds. This does not limit the amount of time a worker can run since, the message receiver will automatically refresh the timeout for any message that reaches its timeout half-life.");
//		}
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
			long start = System.currentTimeMillis();
			int count =triggerFiredImpl();
			long elapse = System.currentTimeMillis()-start;
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
		// Do nothing if this queue is not enabled
		if(!messageQueue.isEnabled()){
			if(log.isDebugEnabled()){
				log.debug("Nothing to do since the queue is disabled: "+messageQueue.getQueueName());
			}
			return 0;
		}

		int maxMessages = maxNumberOfWorkerThreads*maxMessagePerWorker;
		// Receive the maximum number of messages from the queue.
		List<Message> toBeProcessed = sqsDao.receiveMessages(messageQueue.getQueueUrl(), visibilityTimeoutSec, maxMessages);
		if (toBeProcessed.size() < 1) {
			return 0;
		}
		// When workers continue to make progress on a message the messages will get added to this queue.
		final ConcurrentLinkedQueue<Message> progressingMessagesQueue = new ConcurrentLinkedQueue<Message>();
		WorkerProgress workerProgresss = new WorkerProgress() {
			@Override
			public void progressMadeForMessage(Message messag) {
				// Add this message to the 
				progressingMessagesQueue.add(messag);
			}

			@Override
			public void retryMessage(Message message, int retryTimeoutInSeconds) {
				sqsDao.resetMessageVisibility(messageQueue.getQueueUrl(), retryTimeoutInSeconds, Collections.singletonList(message));
			}
		};
		// start all of the workers
		List<WorkerData> currentWorkers = startWorkers(toBeProcessed, workerProgresss);
		// Give the workers a chance to start
		Thread.sleep(100);
		// Watch the workers.  As they complete their tasks, delete the messages from the queue.
		long startTime = System.currentTimeMillis();
		long visibilityMs = visibilityTimeoutSec*1000;
		long visibilityMsHalfLife = visibilityMs/2L;
		while(currentWorkers.size() > 0){
			long elapase = System.currentTimeMillis()-startTime;
			if(elapase > visibilityMsHalfLife){
				// If the visibility timeout is exceeded the messages will once again become visible
				// to other works.  We do not want this to happen to messages that are currently bing processed.
				// Therefore we reset the visibility timeout of all active messages when the half-life is reached.
				
				// Build up the set of messages that need to be updated
				Set<Message> messagesToUpdate = new HashSet<Message>();
				for(Message message = progressingMessagesQueue.poll(); message != null; message = progressingMessagesQueue.poll()){
					messagesToUpdate.add(message);
				}
				sqsDao.resetMessageVisibility(messageQueue.getQueueUrl(), visibilityTimeoutSec, messagesToUpdate);
				// Reset the clock
				startTime = System.currentTimeMillis();
			}
			// Once a worker is done we remove it.
			List<WorkerData> toRemove = new LinkedList<WorkerData>();
			// Used to keep track of messages that need to be deleted.
			List<Message> messagesToDelete = new LinkedList<Message>();
			for(WorkerData data: currentWorkers){
				if(data.getFuture().isDone()){
					try {
						List<Message> messages = data.getFuture().get();
						// all returned messages are to be deleted.
						messagesToDelete.addAll(messages);
					} catch (InterruptedException e) {
						// We cannot remove this message from the queue.
						log.error("Failed to process a SQS message:", e);
					} catch (ExecutionException e) {
						// We cannot remove this message from the queue.
						log.error("Failed to process a SQS message:", e);
					}
					// done with this future
					toRemove.add(data);
				}
			}
			// Batch delete all of the completed message.
			if (messagesToDelete.size() > 0) {
				sqsDao.deleteMessages(messageQueue.getQueueUrl(), messagesToDelete);
			}
			// remove all that we can
			currentWorkers.removeAll(toRemove);
			// Give other threads a chance to work
			Thread.sleep(100);
		}
		// Return the number of messages that were on the queue.
		return toBeProcessed.size();
	}

	/**
	 * @param toBeProcessed
	 * @param workerProgresss
	 * @return
	 */
	public List<WorkerData> startWorkers(List<Message> toBeProcessed,
			WorkerProgress workerProgresss) {
		List<WorkerData> currentWorkers = new LinkedList<WorkerData>();
		// Batch for each worker.
		List<List<Message>> batches = Lists.partition(toBeProcessed, maxMessagePerWorker);
		int workerId = 0;
		for(List<Message> batch: batches){
			Callable<List<Message>> worker = workerFactory.createWorker(batch, workerProgresss);
			// Fire up the worker
			Future<List<Message>> future = executors.submit(worker);
			WorkerData data = new WorkerData(workerId++, batch, future);
			// Add the worker to the list.
			currentWorkers.add(data);
		}
		return currentWorkers;
	}
	
	/**
	 * Validate that we have all of the required configuration.
	 */
	private void verifyConfig() {
		if(sqsDao == null) throw new IllegalStateException("sqsDao cannot be null");
		if(maxNumberOfWorkerThreads == null) throw new IllegalStateException("maxNumberOfWorkerThreads cannot be null");
		if(visibilityTimeoutSec == null) throw new IllegalStateException("visibilityTimeout cannot be null");
		if(messageQueue == null) throw new IllegalStateException("messageQueue cannot be null");
		if(executors == null && messageQueue.isEnabled()){
			// Create the thread pool
			executors = Executors.newFixedThreadPool(maxNumberOfWorkerThreads);
		}
	}

	@Override
	public void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do{
			count = triggerFired();
			log.debug("Emptying the file message queue, there were at least: "+count+" messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis()-start;
			long timeoutMS = visibilityTimeoutSec*1000*10;
			if(elapse > timeoutMS) throw new RuntimeException("Timed-out waiting process all messages that were on the queue.");
		}while(count > 0);
	}
	
	/**
	 * Captures data about a single worker.
	 * @author John
	 *
	 */
	private static class WorkerData {
		int workerId;
		List<Message> messagesPassedToWorker;
		Future<List<Message>> future;
		
		public WorkerData(int workerId, List<Message> messagesPassedToWorker,
				Future<List<Message>> future) {
			super();
			this.workerId = workerId;
			this.messagesPassedToWorker = messagesPassedToWorker;
			this.future = future;
		}
		public int getWorkerId() {
			return workerId;
		}
		public List<Message> getMessagesPassedToWorker() {
			return messagesPassedToWorker;
		}
		public Future<List<Message>> getFuture() {
			return future;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + workerId;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WorkerData other = (WorkerData) obj;
			if (workerId != other.workerId)
				return false;
			return true;
		}
	}

}
