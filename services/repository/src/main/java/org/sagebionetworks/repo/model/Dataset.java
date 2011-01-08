package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * @author bhoff
 *
 */
@PersistenceCapable(detachable = "true")
public class Dataset implements Revisable<Dataset> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
	@Persistent(dependent = "true") 	
	private Revision<Dataset> revision;
	
	@Persistent
	private String name;
	
	@Persistent
	private String description;
	
	@Persistent
	private Text overview;
	
	@Persistent
	private String studyArea;
	
	public enum DatasetStatus { PROPOSED, IN_PROGRESS, COMPLETED }
	
	@Persistent
	private DatasetStatus status;
	
	@Persistent
	private String species;
	
	@Persistent
	private Date releaseDate;
	
	@Persistent
	private List<String> contributors;
	
	@Persistent
	private boolean downloadable;
	
	private Collection<Key> layers;
	
//	// http://code.google.com/appengine/docs/java/datastore/relationships.html#Owned_One_to_Many_Relationships
//	// Note: the 'dataset' field is not in the AnalysisResult class but rather in the DatasetLayer class
//	@Persistent(mappedBy = "dataset")
//	@Element(dependent = "true")
//	private Collection<AnalysisResult> analysisResults;
	
	public Dataset() {
		setRevision(new Revision<Dataset>());
		getRevision().setOwner(this);
	}
	public Key getId() {
		return id;
	}
	public void setId(Key id) {
		this.id = id;
	}

	public Revision<Dataset> getRevision() {
		return revision;
	}
	public void setRevision(Revision<Dataset> revision) {
		this.revision = revision;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Text getOverview() {
		return overview;
	}
	public void setOverview(Text overview) {
		this.overview = overview;
	}
	public String getStudyArea() {
		return studyArea;
	}
	public void setStudyArea(String studyArea) {
		this.studyArea = studyArea;
	}
	public DatasetStatus getStatus() {
		return status;
	}
	public void setStatus(DatasetStatus status) {
		this.status = status;
	}
	public String getSpecies() {
		return species;
	}
	public void setSpecies(String species) {
		this.species = species;
	}
	public Date getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(Date released) {
		this.releaseDate = released;
	}
	public List<String> getContributors() {
		return contributors;
	}
	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}
	public boolean isDownloadable() {
		return downloadable;
	}
	public void setDownloadable(boolean downloadable) {
		this.downloadable = downloadable;
	}
	public Collection<Key> getLayers() {
		return layers;
	}
	public void setLayers(Collection<Key> layers) {
		this.layers =layers;
	}
//	public Collection<AnalysisResult> getAnalysisResults() {
//		return analysisResults;
//	}
//	public void setAnalysisResults(Collection<AnalysisResult> analysisResults) {
//		this.analysisResults = analysisResults;
//	}

	
}
