package org.sagebionetworks.table.worker;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker listens to entity change events for the sole purpose of marking the
 * view scopes associated with the changed entity as out-of-date.
 *
 */
public class ScopeOutOfDateMarker implements ChangeMessageDrivenRunner {
	
	/**
	 * Maximum age a messages that should be processed.
	 */
	public static final long MAX_MESSAGE_AGE_MS = 1000*60*10;
	
	@Autowired
	Clock clock;
	@Autowired
	TableIndexConnectionFactory connectionFactory;


	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {

		if(ObjectType.ENTITY.equals(message.getObjectType())){
			// old messages are ignored.
			if(message.getTimestamp().getTime() > (clock.currentTimeMillis() - MAX_MESSAGE_AGE_MS)) {
				connectionFactory.connectToFirstIndex().markEntityScopeOutOfDate(message.getObjectId());
			}
		}
	}

}
