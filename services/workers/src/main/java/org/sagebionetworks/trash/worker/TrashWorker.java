package org.sagebionetworks.trash.worker;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashWorker implements ProgressingRunner<Void>{
	private final Logger logger = LogManager.getLogger(TrashWorker.class);
	public static final long MAX_TRASH_ITEMS = 250000;
	public static final long TRASH_AGE_IN_DAYS = 30; //about 1 month
	
	@Autowired
	private TrashManager trashManager;

	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		
		List<Long> trashList = trashManager.getTrashLeavesBefore(TRASH_AGE_IN_DAYS, MAX_TRASH_ITEMS);
		logger.info("Purging " + trashList.size() + " entities, before " +
					TRASH_AGE_IN_DAYS + " days, from the trash can.");
		UserInfo adminUser = new UserInfo(true, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		trashManager.purgeTrashAdmin(trashList,adminUser, null);
	}

}
