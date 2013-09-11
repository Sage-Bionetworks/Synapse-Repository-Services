package org.sagebionetworks.trash.worker;

import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashWorkerFactory implements Runnable {

	@Autowired
	private TrashManager trashManager;

	@Override
	public void run() {
		TrashWorker worker = new TrashWorker(trashManager);
		try {
			worker.purgeTrash();
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
