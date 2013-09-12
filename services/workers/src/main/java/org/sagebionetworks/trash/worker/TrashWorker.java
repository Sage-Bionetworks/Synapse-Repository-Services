package org.sagebionetworks.trash.worker;

import java.sql.Timestamp;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.web.NotFoundException;

public class TrashWorker {

	private final static long MONTH = 1000 * 60 * 60 * 24 * 30;
	private final Logger logger = LogManager.getLogger(TrashWorker.class);
	private final TrashManager trashManager;

	public TrashWorker(TrashManager trashManager) {
		if (trashManager == null) {
			throw new IllegalArgumentException("Trash manager cannot be null.");
		}
		this.trashManager = trashManager;
	}

	public void purgeTrash() throws DatastoreException, NotFoundException {
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis() - MONTH);
		List<TrashedEntity> trashList = trashManager.getTrashBefore(timestamp);
		logger.info("Purging " + trashList.size() + " entities from the trash can.");
		trashManager.purgeTrash(trashList);
	}
}
