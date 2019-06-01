package org.sagebionetworks.search.workers.sqs.search;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This worker is used to reconcile missing documents from the search index. The
 * worker will stream over all relevant changes and push any document to the
 * search index that is not already in the index.
 * 
 * See: PLFM-5570.
 *
 */
public class SearchReconciliationWorker implements ProgressingRunner {

	static private Logger log = LogManager.getLogger(SearchReconciliationWorker.class);

	@Autowired
	WorkerLogger workerLogger;

	@Autowired
	DBOChangeDAO changeDao;

	@Autowired
	SearchManager searchManager;

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		Set<ObjectType> objectTypes = Sets.newHashSet(ObjectType.ENTITY, ObjectType.WIKI);
		Set<ChangeType> changeTypes = Sets.newHashSet(ChangeType.CREATE, ChangeType.UPDATE);
		try {
			changeDao.streamOverChanges(objectTypes, changeTypes, (ChangeMessage change) -> {
				pushChange(change);
			});
		} catch (Exception e) {
			log.error("Streaming over changes failed:", e);
			workerLogger.logWorkerFailure(SearchReconciliationWorker.class.getName(), e, true);
		}
	}

	/**
	 * Push this change to the
	 * 
	 * @param change
	 */
	void pushChange(ChangeMessage change) {
		try {
			searchManager.documentChangeMessages(Lists.newArrayList(change));
		} catch (Exception e) {
			log.warn(e.getMessage());
			workerLogger.logWorkerFailure(SearchReconciliationWorker.class.getName(), e, false);
		}
	}
}
