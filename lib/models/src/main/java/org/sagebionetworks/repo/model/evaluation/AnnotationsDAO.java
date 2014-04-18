package org.sagebionetworks.repo.model.evaluation;

import java.util.List;

import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface AnnotationsDAO {
	
	public static final boolean DEFAULT_ANNOTATION_PRIVACY = true;
	
	/**
	 * Get all of the annotations for a specific SubmissionStatus. Reads from the typed tables.
	 * @param owner
	 * @return
	 */
	public Annotations getAnnotations(Long owner);
	
	/**
	 * get the IDs of the submissions that are new (are missing in Annotation tables) or have changed
	 * since the last annotations update
	 * @param scopeId
	 */
	public List<SubmissionBundle> getChangedSubmissions(Long scopeId);
	
	/**
	 * Get all of the annotations for a specific SubmissionStatus. Reads from the blob table.
	 * @param owner
	 * @return
	 */
	public Annotations getAnnotationsFromBlob(Long owner);

	/**
	 * Replace all annotations corresponding to a list of submissions.
	 * @param annotations
	 * @throws DatastoreException 
	 * @throws JSONObjectAdapterException 
	 */
	public void replaceAnnotations(List<Annotations> annotations) throws DatastoreException, JSONObjectAdapterException;
	
	/**
	 * Deletes all annotations associated with the given scope
	 * @param evalId
	 */
	public void deleteAnnotationsByScope(Long evalId);
}
