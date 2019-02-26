package org.sagebionetworks.worker.utils;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.worker.job.tracking.JobTracker;
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
	JobTracker jobTracker;

	/**
	 * Profile any {@link MessageDrivenRunner#run(ProgressCallback, Message)}
	 * or {@link ChangeMessageDrivenRunner#run(ProgressCallback, ChangeMessage)}
	 * or {@link BatchChangeMessageDrivenRunner#run(ProgressCallback, List)}
	 * call.
	 * 
	 * @param progressCallback
	 * @param message
	 * @throws Throwable
	 */
	@Around("execution(* org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner.run(..))"
			+ " || execution(* org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner.run(..))"
			+ " || execution(* org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner.run(..))")
	public Object profileMessageDrivenRunner(ProceedingJoinPoint pjp)
			throws Throwable {
		// capture the simple name of the target class.
		final String targetClassName = pjp.getTarget().getClass()
				.getSimpleName();
		Object result = null;
		try {
			jobTracker.jobStarted(targetClassName);
			// the actual call.
			result = pjp.proceed();
		} finally {
			jobTracker.jobEnded(targetClassName);
		}
		return result;
	}

}
