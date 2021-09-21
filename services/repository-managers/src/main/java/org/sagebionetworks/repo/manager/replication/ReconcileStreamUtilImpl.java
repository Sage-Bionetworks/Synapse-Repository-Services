package org.sagebionetworks.repo.manager.replication;

import java.util.Iterator;

import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.stereotype.Service;

@Service
public class ReconcileStreamUtilImpl implements ReconcileStreamUtil {

	@Override
	public Iterator<ChangeMessage> reconcileStreams(ObjectType objectType, Iterator<IdAndChecksum> truth,
			Iterator<IdAndChecksum> replication) {
		return new ReconcileIterator(objectType, truth, replication);
	}

}
