package org.sagebionetworks.asynchronous.workers.timed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.semaphore.ProgressingRunner;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class SingletonBeanTimedWorkerFactoryRunner implements ProgressingRunner {

	static private Logger log = LogManager.getLogger(SingletonBeanTimedWorkerFactoryRunner.class);

	private TimedWorker worker;

	@Required
	public void setWorker(TimedWorker worker) {
		this.worker = worker;
	}

	@Override
	public void run(ProgressCallback<Void> callback) throws Exception {
		try {
			worker.run(callback);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}
}
