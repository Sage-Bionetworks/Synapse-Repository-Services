package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;



@PersistenceCapable(detachable = "true")
public class JDOAnalysisResult extends JDODatasetLayer<JDOAnalysisResult> {
	@Persistent(mappedBy = "analysisResult")
	private JDODatasetAnalysis owner; // this is the backwards pointer for
											// the 1-1 owned relationship

	// this is a link to the next revision of the layer (if any)
	@Persistent(dependent = "true")
	private JDOAnalysisResult nextVersion;


	@Persistent
	@Column(jdbcType="LONGVARCHAR")
	private String notes;

	public JDOAnalysisResult getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(JDOAnalysisResult nextVersion) {
		this.nextVersion = nextVersion;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public JDODatasetAnalysis getOwner() {
		return owner;
	}

	public void setOwner(JDODatasetAnalysis owner) {
		this.owner = owner;
	}

}
