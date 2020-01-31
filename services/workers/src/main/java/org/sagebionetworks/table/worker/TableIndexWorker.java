package org.sagebionetworks.table.worker;

import java.util.Iterator;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class TableIndexWorker implements ChangeMessageDrivenRunner {

	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableIndexConnectionFactory connectionFactory;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		// We only care about entity messages here
		if (ObjectType.TABLE.equals((message.getObjectType()))) {
			final String tableId = message.getObjectId();
			final IdAndVersion idAndVersion = IdAndVersion.newBuilder()
					.setId(KeyFactory.stringToKey(message.getObjectId())).setVersion(message.getObjectVersion())
					.build();
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(idAndVersion);
			} catch (TableIndexConnectionUnavailableException e) {
				// try again later.
				throw new RecoverableMessageException();
			}
			if (ChangeType.DELETE.equals(message.getChangeType())) {
				// Delete the table in the index
				tableEntityManager.deleteTableIfDoesNotExist(tableId);
				indexManager.deleteTableIndex(idAndVersion);
				return;
			} else {
				Iterator<TableChangeMetaData> iterator = tableEntityManager.newTableChangeIterator(tableId);
				indexManager.buildIndexToChangeNumber(progressCallback, idAndVersion, iterator);
			}
		}
	}

}
