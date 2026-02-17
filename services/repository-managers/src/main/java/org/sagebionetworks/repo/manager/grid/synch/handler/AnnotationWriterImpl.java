package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.springframework.stereotype.Component;

/**
 * Implementation that updates only the annotations that changed in the copy.
 * This prevents data loss when external changes occur in the source between
 * reading and writing, as unchanged annotations will retain their current
 * values.
 */
@Component
public class AnnotationWriterImpl implements AnnotationWriter {

	private final NodeDAO nodeDao;
	private final NodeManager nodeManager;

	public AnnotationWriterImpl(NodeDAO nodeDao, NodeManager nodeManager) {
		super();
		this.nodeDao = nodeDao;
		this.nodeManager = nodeManager;
	}

	@NewWriteTransaction
	@Override
	public Annotations updateChangedAnnotations(UserInfo user, String key, Map<String, AnnotationsValue> changedCells) {

		// By locking the node we can ensure that the annotations cannot change between
		// the call to get the annotations and the call to update the annotations.
		String currentEtag = nodeDao.lockNode(key);
		Annotations annos = nodeManager.getUserAnnotations(user, key);
		Map<String, AnnotationsValue> annoMap = annos.getAnnotations();

		// Only update annotations that actually changed in the copy. This prevents
		// data loss if external changes occurred in the source after we read it but
		// before we push our changes. Unchanged annotations retain their current
		// values.
		for (Entry<String, AnnotationsValue> e : changedCells.entrySet()) {
			AnnotationsValue value = e.getValue();
			if (value == null) {
				annoMap.remove(e.getKey());
			} else {
				annoMap.put(e.getKey(), e.getValue());
			}
		}
		return nodeManager.updateUserAnnotations(user, key, annos);
	}

}
