package org.sagebionetworks.trash.worker;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashWorker implements ProgressingRunner {
	private final Logger logger = LogManager.getLogger(TrashWorker.class);
	public static final long TRASH_DELETE_LIMIT = 10000;
	public static final long CUTOFF_TRASH_AGE_IN_DAYS = 30; //about 1 month
	private static final int PURGE_BATCH_SIZE = 1000;
	
	@Autowired
	private TrashManager trashManager;

	@Override
	public void run(ProgressCallback progressCallback) {
		long startTime = System.currentTimeMillis();
		
		try {
			List<Long> trashList = trashManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
			
			logger.info("Purging " + trashList.size() + " entities, older than " + CUTOFF_TRASH_AGE_IN_DAYS + " days, from the trash can.");
			
			UserInfo adminUser = new UserInfo(true, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
			
			for (List<Long> batch : ListUtils.partition(trashList, PURGE_BATCH_SIZE)) {
				trashManager.purgeTrashAdmin(batch, adminUser);
			}
	
			logger.info("Sucessfully purged " +  trashList.size() + " trash entities. Worker took " + (System.currentTimeMillis() - startTime) + " miliseconds.");
		} catch (Exception e){
			logger.error("Unable to purge the trash entities. Worker took "+ (System.currentTimeMillis() - startTime) +" miliseconds. ");
			logger.catching(e);
		}
		
	}

}
