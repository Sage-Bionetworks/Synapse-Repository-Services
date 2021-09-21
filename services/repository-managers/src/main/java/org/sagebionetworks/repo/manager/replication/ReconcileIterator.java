package org.sagebionetworks.repo.manager.replication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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

	Iterator<ChangeMessage> currentBatch;

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

	private Optional<IdAndChecksum> truthNext() {
		if (truth.hasNext()) {
			return Optional.of(truth.next());
		} else {
			return Optional.empty();
		}
	}

	private Optional<IdAndChecksum> replicationNext() {
		if (replication.hasNext()) {
			return Optional.of(replication.next());
		} else {
			return Optional.empty();
		}
	}

	private Iterator<ChangeMessage> findNextBatch() {
		List<ChangeMessage> results = new ArrayList<>();
		Optional<IdAndChecksum> t = truthNext();
		Optional<IdAndChecksum> r = replicationNext();
		while (true) {
			if (t.isPresent()) {
				if (!r.isPresent()) {
					// object missing from replication
					results.add(newChange(ChangeType.CREATE, t.get().getId()));
					return results.iterator();
				} else {
					// both t and r exist
					IdAndChecksum fromT = t.get();
					IdAndChecksum fromR = r.get();
					if (fromT.equals(fromR)) {
						// we have a match so move both to next
						t = truthNext();
						r = replicationNext();
						continue;
					} else {
						// t != r
						if (fromT.getId().equals(fromR.getId())) {
							// Same ID but different checksums so send an update
							results.add(newChange(ChangeType.UPDATE, t.get().getId()));
							return results.iterator();
						}else {
							// different IDs
							if(fromT.getId() > fromR.getId()) {
								results.add(newChange(ChangeType.DELETE, r.get().getId()));
								r = replicationNext();
								// we must continue because we are not done with t.
								continue;
							}else {
								results.add(newChange(ChangeType.CREATE, t.get().getId()));
								t = truthNext();
								// we must continue because we are not done with r.
								continue;
							}
						}
					}
				}
			} else {
				// truth is exhausted
				if(r.isPresent()) {
					results.add(newChange(ChangeType.DELETE, r.get().getId()));
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
