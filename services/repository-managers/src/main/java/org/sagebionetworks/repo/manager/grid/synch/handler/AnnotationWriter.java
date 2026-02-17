package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.util.Map;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;

public interface AnnotationWriter {

	/**
	 * Updates only the annotations that changed in the copy. This prevents data
	 * loss when external changes occur in the source between reading and writing,
	 * as unchanged annotations will retain their current values.
	 * 
	 * @param user         The user making the changes
	 * @param key          The entity ID
	 * @param changedCells Map of annotation names to their new values (only changed
	 *                     annotations)
	 * @return The updated annotations
	 */
	Annotations updateChangedAnnotations(UserInfo user, String key, Map<String, AnnotationsValue> changedCells);
}
