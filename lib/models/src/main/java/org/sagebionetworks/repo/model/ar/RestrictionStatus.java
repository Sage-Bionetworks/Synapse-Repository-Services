package org.sagebionetworks.repo.model.ar;

public class RestrictionStatus {

	Long restrictionId;
	String restrictionType;
	boolean isUnmet;
	
	public RestrictionStatus(Long restrictionId, String restrictionType, boolean isUnmet) {
		super();
		this.restrictionId = restrictionId;
		this.restrictionType = restrictionType;
		this.isUnmet = isUnmet;
	}
	
	
}
