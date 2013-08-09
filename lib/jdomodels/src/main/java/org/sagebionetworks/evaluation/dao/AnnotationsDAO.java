package org.sagebionetworks.evaluation.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface AnnotationsDAO {
	
	/**
	 * Get all of the annotations for a specific SubmissionStatus. Reads from the typed tables.
	 * @param owner
	 * @return
	 */
	public Annotations getAnnotations(Long owner);
	
	/**
	 * Get all of the annotations for a specific SubmissionStatus. Reads from the blob table.
	 * @param owner
	 * @return
	 */
	public Annotations getAnnotationsFromBlob(Long owner);

	/**
	 * Replace all annotations.
	 * @param annotations
	 * @throws DatastoreException 
	 * @throws JSONObjectAdapterException 
	 */
	public void replaceAnnotations(Annotations annotations) throws DatastoreException, JSONObjectAdapterException;

	/**
	 * Deletes all the annotations associated with the specified owner.
	 */
	public void deleteAnnotationsByOwnerId(Long ownerId);
}
