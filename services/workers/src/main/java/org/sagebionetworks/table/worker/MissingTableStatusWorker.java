package org.sagebionetworks.table.worker;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.IdVersionTableType;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker finds all tables/views with a missing status. For each missing
 * status a local message is pushed to the appropriate SNS topic.
 *
 */
public class MissingTableStatusWorker implements ProgressingRunner {

	static Log log = LogFactory.getLog(MissingTableStatusWorker.class);

	public static long LIMIT = 100_000;

	private final TableStatusDAO tableStatusDao;
	private final RepositoryMessagePublisher repositoryMessagePublisher;

	@Autowired
	public MissingTableStatusWorker(TableStatusDAO tableStatusDao,
			RepositoryMessagePublisher repositoryMessagePublisher) {
		super();
		this.tableStatusDao = tableStatusDao;
		this.repositoryMessagePublisher = repositoryMessagePublisher;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		
		List<IdVersionTableType> missingStatus = tableStatusDao.getAllTablesAndViewsWithMissingStatus(LIMIT);
		
		log.info(String.format("Found %d tables/views with missing status.", missingStatus.size()));
		
		missingStatus.forEach(m -> {
			log.info("Sending message for: " + m.toString());
			repositoryMessagePublisher.fireLocalStackMessage(new LocalStackChangeMesssage()
					.setChangeNumber(42L)
					.setObjectId(m.getIdAndVersion().getId().toString())
					.setObjectVersion(m.getIdAndVersion().getVersion().orElse(null))
					.setObjectType(m.getType().getObjectType())
					.setChangeType(ChangeType.UPDATE)
					.setTimestamp(new Date())
					.setUserId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
		});

	}

}
