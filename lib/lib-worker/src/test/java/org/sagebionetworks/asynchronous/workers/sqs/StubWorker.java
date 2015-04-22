package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

/**
 * A stub worker that will sleep and/or throw exceptions.
 * 
 * @author John
 *
 */
public class StubWorker implements Callable<List<Message>>{
	
	private long sleepTime;
	private Exception exceptionToThrow;
	private List<Message> messages;
	private WorkerProgress progress;
	private int numberProgressReports;
	
	/**
	 * Setup a worker that will sleep and/or throw an exception.
	 * @param sleepTime
	 * @param exceptionToThrow
	 */
	public StubWorker(long sleepTime, int numberProgressReports, Exception exceptionToThrow) {
		super();
		this.sleepTime = sleepTime;
		this.exceptionToThrow = exceptionToThrow;
		this.numberProgressReports = numberProgressReports;
	}


	@Override
	public List<Message> call() throws Exception {
		if(numberProgressReports > 0){
			// Divide up the time by the number of reports
			for(int i=0; i<numberProgressReports; i++){
				Thread.sleep(sleepTime/numberProgressReports);
				for(Message toReport: this.messages){
					progress.progressMadeForMessage(toReport);
				}
			}
		}else{
			Thread.sleep(sleepTime);
		}
		if(exceptionToThrow != null) throw exceptionToThrow;
		return messages;
	}
	
	/**
	 * Capture the message.
	 * @param message
	 * @return
	 */
	public StubWorker withMessage(List<Message> messages){
		this.messages = messages;
		System.out.println("Worker passed: "+messages.size()+" messages");
		return this;
	}
	
	public StubWorker withProgress(WorkerProgress progress){
		this.progress = progress;
		return this;
	}
	
}