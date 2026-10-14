package org.sagebionetworks.table.worker;

import java.time.Instant;
import java.util.Date;

import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao.DependentObject;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

/**
 * Listens for table status changes. When a source table/view becomes AVAILABLE, fans out a rebuild
 * trigger to every defining-SQL object (materialized view, search index, ...) that depends on it.
 * Each dependent is published as a non-durable message to the {@link ObjectType#SOURCE_DEPENDENCY_EVENT}
 * topic, carrying the dependent's own {@link ObjectType} so the corresponding rebuild worker
 * (materialized view update, search index rebuild) picks it up and the others ignore it.
 */
@Service
public class DefiningSqlSourceUpdateWorker implements TypedMessageDrivenRunner<TableStatusChangeEvent> {

	private static final long DEPENDENTS_PAGE_SIZE = 1000;

	private final DefiningSqlDependencyDao definingSqlDependencyDao;
	private final RepositoryMessagePublisher repositoryMessagePublisher;

	@Autowired
	public DefiningSqlSourceUpdateWorker(DefiningSqlDependencyDao definingSqlDependencyDao,
			RepositoryMessagePublisher repositoryMessagePublisher) {
		this.definingSqlDependencyDao = definingSqlDependencyDao;
		this.repositoryMessagePublisher = repositoryMessagePublisher;
	}

	@Override
	public Class<TableStatusChangeEvent> getObjectClass() {
		return TableStatusChangeEvent.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, TableStatusChangeEvent event)
			throws RecoverableMessageException, Exception {
		if (ObjectType.TABLE_STATUS_EVENT != event.getObjectType()) {
			throw new IllegalStateException("Unsupported object type: expected "
					+ ObjectType.TABLE_STATUS_EVENT.name() + ", got " + event.getObjectType());
		}
		// Fan out only when the source became available.
		if (event.getState() != TableState.AVAILABLE) {
			return;
		}
		IdAndVersion sourceTableId = KeyFactory.idAndVersion(event.getObjectId(), event.getObjectVersion());

		PaginationIterator<DependentObject> dependents = new PaginationIterator<>(
				(limit, offset) -> definingSqlDependencyDao.getDependentsPage(sourceTableId, limit, offset),
				DEPENDENTS_PAGE_SIZE);

		dependents.forEachRemaining(dependent -> repositoryMessagePublisher.publishLocalStackMessageToTopic(
				ObjectType.SOURCE_DEPENDENCY_EVENT,
				new LocalStackChangeMesssage()
						.setObjectId(KeyFactory.keyToString(dependent.objectId().getId()))
						.setObjectVersion(dependent.objectId().getVersion().orElse(null))
						.setObjectType(ObjectType.valueOf(dependent.objectType()))
						.setChangeType(ChangeType.UPDATE)
						.setTimestamp(Date.from(Instant.now()))));
	}

}
