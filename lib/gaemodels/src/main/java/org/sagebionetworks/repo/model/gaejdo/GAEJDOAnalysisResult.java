package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable(detachable = "true")
public class GAEJDOAnalysisResult extends GAEJDODatasetLayer<GAEJDOAnalysisResult> {
	@Persistent(mappedBy = "analysisResult")
	private GAEJDODatasetAnalysis owner; // this is the backwards pointer for
											// the 1-1 owned relationship

	// this is a link to the next revision of the layer (if any)
	@Persistent(dependent = "true")
	private GAEJDOAnalysisResult nextVersion;


	@Persistent
	private Text notes;

	public GAEJDOAnalysisResult getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(GAEJDOAnalysisResult nextVersion) {
		this.nextVersion = nextVersion;
	}

	public Text getNotes() {
		return notes;
	}

	public void setNotes(Text notes) {
		this.notes = notes;
	}

	public GAEJDODatasetAnalysis getOwner() {
		return owner;
	}

	public void setOwner(GAEJDODatasetAnalysis owner) {
		this.owner = owner;
	}

}
