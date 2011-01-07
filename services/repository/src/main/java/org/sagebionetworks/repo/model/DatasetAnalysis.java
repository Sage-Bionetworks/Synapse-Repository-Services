package org.sagebionetworks.repo.model;

import java.util.Collection;

import com.google.appengine.api.datastore.Key;

public class DatasetAnalysis {
	private Key id;
	private Collection<DatasetLayer> datasetLayers;
	private Script script;
	private AnalysisResult analysisResult;
	public Key getId() {
		return id;
	}
	public void setId(Key id) {
		this.id = id;
	}
	public Collection<DatasetLayer> getDatasetLayers() {
		return datasetLayers;
	}
	public void setDatasetLayers(Collection<DatasetLayer> datasetLayers) {
		this.datasetLayers = datasetLayers;
	}
	public Script getScript() {
		return script;
	}
	public void setScript(Script script) {
		this.script = script;
	}
	public AnalysisResult getAnalysisResult() {
		return analysisResult;
	}
	public void setAnalysisResult(AnalysisResult analysisResult) {
		this.analysisResult = analysisResult;
	}
}
