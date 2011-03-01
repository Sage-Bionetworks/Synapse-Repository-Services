package org.sagebionetworks.repo.model.jdo;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;


@PersistenceCapable(detachable = "true")
public class GAEJDOAnalysisResult extends GAEJDODatasetLayer<GAEJDOAnalysisResult> {
	@Persistent(mappedBy = "analysisResult")
	private GAEJDODatasetAnalysis owner; // this is the backwards pointer for
											// the 1-1 owned relationship

	// this is a link to the next revision of the layer (if any)
	@Persistent(dependent = "true")
	private GAEJDOAnalysisResult nextVersion;


	@Persistent
	private String notes;

	public GAEJDOAnalysisResult getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(GAEJDOAnalysisResult nextVersion) {
		this.nextVersion = nextVersion;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public GAEJDODatasetAnalysis getOwner() {
		return owner;
	}

	public void setOwner(GAEJDODatasetAnalysis owner) {
		this.owner = owner;
	}

}
