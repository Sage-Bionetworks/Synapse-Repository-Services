package org.sagebionetworks.repo.model.ar;

import java.util.ArrayList;
import java.util.List;

public class SubjectStatus {
	
	private Long subjectId;
	private Long userId;
	private List<RestrictionStatus> accessRestrictions;
	
	public SubjectStatus(Long subjectId, Long userId) {
		super();
		this.subjectId = subjectId;
		this.userId = userId;
		this.accessRestrictions = new ArrayList<RestrictionStatus>();
	}
	
	public void addRestrictionStatus(RestrictionStatus toAdd) {
		this.accessRestrictions.add(toAdd);
	}

}
