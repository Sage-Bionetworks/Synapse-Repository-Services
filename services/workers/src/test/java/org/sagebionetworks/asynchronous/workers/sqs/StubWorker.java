package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

/**
 * A stub worker that will sleep and/or throw exceptions.
 * 
 * @author John
 *
 */
public class StubWorker implements Callable<Message>{
	
	private long sleepTime;
	private Exception exceptionToThrow;
	private Message message;
	
	/**
	 * Setup a worker that will sleep and/or throw an exception.
	 * @param sleepTime
	 * @param exceptionToThrow
	 */
	public StubWorker(long sleepTime, Exception exceptionToThrow) {
		super();
		this.sleepTime = sleepTime;
		this.exceptionToThrow = exceptionToThrow;
	}


	@Override
	public Message call() throws Exception {
		Thread.sleep(sleepTime);
		if(exceptionToThrow != null) throw exceptionToThrow;
		return message;
	}
	
	/**
	 * Capture the message.
	 * @param message
	 * @return
	 */
	public StubWorker withMessage(Message message){
		this.message = message;
		return this;
	}
	
}