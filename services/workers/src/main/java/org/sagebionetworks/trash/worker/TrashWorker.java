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

	private final static long SHIFT_ONE_DAY = 26;
	private final static long MONTH = 1000L * 60L * 60L * 24L * 30L;
	private final Logger logger = LogManager.getLogger(TrashWorker.class);
	private final TrashManager trashManager;

	public TrashWorker(TrashManager trashManager) {
		if (trashManager == null) {
			throw new IllegalArgumentException("Trash manager cannot be null.");
		}
		this.trashManager = trashManager;
	}

	public void purgeTrash() throws DatastoreException, NotFoundException {
		long now = System.currentTimeMillis();
		// Drop (very roughly) the hours, minutes, seconds so that the two workers,
		// one in prod and the other in staging, will have a good chance
		// to use the same timestamp to purge the trash can.
		long today = (now >> SHIFT_ONE_DAY) << SHIFT_ONE_DAY;
		final Timestamp timestamp = new Timestamp(today - MONTH);
		List<TrashedEntity> trashList = trashManager.getTrashBefore(timestamp);
		logger.info("Purging " + trashList.size() + " entities, before " +
					timestamp + ", from the trash can.");
		trashManager.purgeTrash(trashList, null);
	}
}
