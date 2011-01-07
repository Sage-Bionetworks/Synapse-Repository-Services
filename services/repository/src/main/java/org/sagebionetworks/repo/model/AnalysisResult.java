package org.sagebionetworks.repo.model;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable(detachable = "true")
public class AnalysisResult extends DatasetLayer {
//	@Persistent
//	private Dataset dataset; // backwards pointer for the 1-many owned relationship
//
//	public Dataset getDataset() {
//		return dataset;
//	}
//
//	public void setDataset(Dataset dataset) {
//		this.dataset = dataset;
//	}
//


	@Persistent
	private Text notes;

	public Text getNotes() {
		return notes;
	}

	public void setNotes(Text notes) {
		this.notes = notes;
	}
}
