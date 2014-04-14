package org.sagebionetworks.annotations.worker;

import java.util.List;

import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.evaluation.AnnotationsDAO;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.dao.DeadlockLoserDataAccessException;

public class StubbedAnnotationDAO implements AnnotationsDAO {

	@Override
	public Annotations getAnnotations(Long owner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotations getAnnotationsFromBlob(Long owner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void replaceAnnotations(Annotations annotations)
			throws DatastoreException, JSONObjectAdapterException {
		throw new DeadlockLoserDataAccessException("foo", null);
	}

	@Override
	public void deleteAnnotationsByOwnerId(Long ownerId) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<SubmissionBundle> getChangedSubmissions(Long scopeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void replaceAnnotationsBatch(List<Annotations> annotations)
			throws DatastoreException, JSONObjectAdapterException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAnnotationsByScope(Long evalId) {
		// TODO Auto-generated method stub
		
	}

}
