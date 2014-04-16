package org.sagebionetworks.evaluation.manager;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;

public class EvaluationSubmissionsObservableEntity implements ObservableEntity {
	private String idString;
	private String etag;
	
	public EvaluationSubmissionsObservableEntity(String idString, String etag) {
		this.idString = idString;
		this.etag=etag;
	}

	@Override
	public String getIdString() {
		return idString;
	}

	@Override
	public String getParentIdString() {
		return null;
	}

	@Override
	public String getEtag() {
		return etag;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.EVALUATION_SUBMISSIONS;
	}

}
