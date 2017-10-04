package org.sagebionetworks.worker.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Gathers and reports worker statistics to CloudWatch.
 * 
 */
@Aspect
public class WorkerProfiler {
	
	@Autowired
	Consumer consumer;
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;

	
	/**
	 * Profile any {@link MessageDrivenRunner#run(ProgressCallback, Message)} call.
	 * 
	 * @param progressCallback
	 * @param message
	 * @throws Throwable 
	 */
	@Around("execution(* org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner"
			+ ".run(org.sagebionetworks.common.util.progress.ProgressCallback, com.amazonaws.services.sqs.model.Message))"
			+ " && args(progressCallback, message)")
	public Object profileMessageDrivenRunner(ProceedingJoinPoint pjp, ProgressCallback progressCallback, Message message) throws Throwable{
		// Lookup the job type.
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		System.out.println(status.getRequestBody().getClass().getName());
		Object retVal = null;
		long startTime = System.currentTimeMillis();
		try{
			// the actual call.
			retVal = pjp.proceed();
		}finally{
			long elapse = System.currentTimeMillis() - startTime;
			System.out.println("Elapse: "+elapse);
		}
		return retVal;
	}

}
