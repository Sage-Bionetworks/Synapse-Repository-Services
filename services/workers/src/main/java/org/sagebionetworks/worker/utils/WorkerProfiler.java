package org.sagebionetworks.worker.utils;

import java.util.Collections;
import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.sqs.model.Message;

/**
 * Gathers and reports worker statistics to CloudWatch.
 * 
 */
@Aspect
public class WorkerProfiler {
	
	public static final String COUNT = "count";
	public static final String RUNTIME = "runtime";
	public static final String WORKER_NAME = "name";
	public static final Double ONE = new Double(1);
	
	public static final String WORKER_COUNT_NAMESPACE = "Worker-Count-2"
			+ StackConfiguration.getStackInstance();
	public static final String WORKER_RUNTIME_NAMESPACE = "Worker-Runtime-2"
			+ StackConfiguration.getStackInstance();
	
	@Autowired
	Consumer consumer;
	
	
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
		final String targetClassName = pjp.getTarget().getClass().getSimpleName();
		Object retVal = null;
		// add count metric before the start
		long startTime = System.currentTimeMillis();
		ProgressListener progressListener = new ProgressListener() {
			@Override
			public void progressMade() {
				// add count metric as progress is made
				fireWorkerCountMetric(targetClassName);
			}
		};
		progressCallback.addProgressListener(progressListener);
		try{
			// the actual call.
			retVal = pjp.proceed();
		}finally{
			// unconditionally remove the progress listener
			progressCallback.removeProgressListener(progressListener);
			long elapseTimeMS = System.currentTimeMillis() - startTime;
			// add a runtime metric
			fireWorkerRuntimeMetric(targetClassName, elapseTimeMS);
			// add count metric at the end.
			fireWorkerCountMetric(targetClassName);
		}
		return retVal;
	}
	
	/**
	 * Fire a worker count metric
	 * @param workerName
	 */
	private void fireWorkerCountMetric(String workerName){
		ProfileData pd = new ProfileData();
		pd.setNamespace(WORKER_COUNT_NAMESPACE);
		pd.setTimestamp(new Date(System.currentTimeMillis()));
		pd.setName(COUNT);
		pd.setUnit(StandardUnit.Count.name());
		pd.setValue(ONE);
		pd.setDimension(Collections.singletonMap(WORKER_NAME, workerName));
		consumer.addProfileData(pd);
	}
	
	/**
	 * Fire a worker runtime metric.
	 * @param workerName
	 * @param elapseTimeMS
	 */
	private void fireWorkerRuntimeMetric(String workerName, long elapseTimeMS){
		ProfileData pd = new ProfileData();
		pd.setNamespace(WORKER_RUNTIME_NAMESPACE);
		pd.setTimestamp(new Date(System.currentTimeMillis()));
		pd.setName(RUNTIME);
		pd.setUnit(StandardUnit.Milliseconds.name());
		pd.setValue(new Double(elapseTimeMS));
		pd.setDimension(Collections.singletonMap(WORKER_NAME, workerName));
		consumer.addProfileData(pd);
	}
	
	
}
