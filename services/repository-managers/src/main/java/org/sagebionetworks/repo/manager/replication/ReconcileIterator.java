package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An Iterator to find changes between two streams of data.
 * Note: Both streams are expected to be ordered by ID ascending.
 *
 */
public class ReconcileIterator implements Iterator<ChangeMessage> {

	private final Iterator<IdAndChecksum> truth;
	private final Iterator<IdAndChecksum> replication;
	private final ObjectType objectType;

	private Iterator<ChangeMessage> currentBatch;

	public ReconcileIterator(ObjectType objectType, Iterator<IdAndChecksum> truth,
			Iterator<IdAndChecksum> replication) {
		ValidateArgument.required(true, "truth");
		ValidateArgument.required(replication, "replication");
		this.truth = truth;
		this.replication = replication;
		this.objectType = objectType;
		this.currentBatch = null;
	}

	@Override
	public boolean hasNext() {
		if(currentBatch == null) {
			currentBatch = findNextBatch();
			if(!currentBatch.hasNext()) {
				return false;
			}
		}
		if(currentBatch.hasNext()) {
			return true;
		}else {
			// had a current batch but it is now exhausted.
			currentBatch = null;
			return hasNext();
		}
	}

	@Override
	public ChangeMessage next() {
		return currentBatch.next();
	}

	private IdAndChecksum truthNext() {
		if (truth.hasNext()) {
			return truth.next();
		} else {
			return null;
		}
	}

	private IdAndChecksum replicationNext() {
		if (replication.hasNext()) {
			return replication.next();
		} else {
			return null;
		}
	}

	private Iterator<ChangeMessage> findNextBatch() {
		List<ChangeMessage> results = new ArrayList<>();
		IdAndChecksum truthItem = truthNext();
		IdAndChecksum replicationItem = replicationNext();
		while (true) {
			if (truthItem != null) {
				if (replicationItem == null) {
					// object missing from replication
					results.add(newChange(ChangeType.CREATE, truthItem.getId()));
					return results.iterator();
				} else {
					// both truth and replication exist
					if (truthItem.equals(replicationItem)) {
						// we have a match so move both to next
						truthItem = truthNext();
						replicationItem = replicationNext();
						continue;
					} else {
						// t != r
						if (truthItem.getId().equals(replicationItem.getId())) {
							// Same ID but different checksums so send an update
							results.add(newChange(ChangeType.UPDATE, truthItem.getId()));
							return results.iterator();
						}else {
							// different IDs
							if(truthItem.getId() > replicationItem.getId()) {
								results.add(newChange(ChangeType.DELETE, replicationItem.getId()));
								replicationItem = replicationNext();
								// we must continue because we are not done with truthItem.
								continue;
							}else {
								results.add(newChange(ChangeType.CREATE, truthItem.getId()));
								truthItem = truthNext();
								// we must continue because we are not done with repItem.
								continue;
							}
						}
					}
				}
			} else {
				// truth is exhausted
				if(replicationItem != null) {
					results.add(newChange(ChangeType.DELETE, replicationItem.getId()));
					return results.iterator();
				}else {
					// both truth and replication are exhausted, so we are done.
					return results.iterator();
				}
			}
		}
	}

	private ChangeMessage newChange(ChangeType changeType, Long objectId) {
		return new ChangeMessage().setChangeType(changeType).setObjectType(objectType)
				.setObjectId(objectId.toString());
	}

}
