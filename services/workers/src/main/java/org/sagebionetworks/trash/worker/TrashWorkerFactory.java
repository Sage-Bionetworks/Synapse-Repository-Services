package org.sagebionetworks.trash.worker;

import org.apache.log4j.Logger;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashWorkerFactory implements Runnable {

	private final Logger logger = Logger.getLogger(TrashWorkerFactory.class);

	@Autowired
	private TrashManager trashManager;

	@Override
	public void run() {
		TrashWorker worker = new TrashWorker(trashManager);
		try {
			worker.purgeTrash();
		} catch (DatastoreException e) {
			logger.error(e.getMessage());
		} catch (NotFoundException e) {
			logger.error(e.getMessage());
		}
	}
}
