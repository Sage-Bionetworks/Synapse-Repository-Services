package org.sagebionetworks.repo.init;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.BooleanUtils;
import org.quartz.JobExecutionException;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.MethodInvoker;

public class DelayedInitializer extends MethodInvoker implements InitializingBean {

	private CountingSemaphoreDao semaphore;

	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();
	}

	@Required
	public void setSemaphore(CountingSemaphoreDao semaphore) {
		this.semaphore = semaphore;
	}

	public void postInitialize() throws InvocationTargetException, IllegalAccessException, JobExecutionException {
		String lockToken = semaphore.attemptToAcquireLock();
		if (lockToken != null) {
			try {
				Boolean result = (Boolean) this.invoke();
				if (BooleanUtils.isTrue(result)) {
					// Initialization is done. Unschedule this trigger for the lifetime of the java process
					JobExecutionException jobExecutionException = new JobExecutionException("Initialization done");
					jobExecutionException.setUnscheduleFiringTrigger(true);
					throw jobExecutionException;
				}
			} finally {
				semaphore.releaseLock(lockToken);
			}
		}
	}
}
