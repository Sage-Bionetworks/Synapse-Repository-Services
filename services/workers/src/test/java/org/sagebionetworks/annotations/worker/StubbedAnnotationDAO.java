package org.sagebionetworks.annotations.worker;

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

}
