package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable = "true")
public class GAEJDODatasetAnalysis {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;

	@Persistent
	private Collection<Key> layers;

	@Persistent
	private Key script;

	@Persistent(dependent = "true")
	private GAEJDOAnalysisResult analysisResult;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public Collection<Key> getLayers() {
		return layers;
	}

	public void setLayers(Collection<Key> layers) {
		this.layers = layers;
	}

	public Key getScript() {
		return script;
	}

	public void setScript(Key script) {
		this.script = script;
	}

	public GAEJDOAnalysisResult getAnalysisResult() {
		return analysisResult;
	}

	public void setAnalysisResult(GAEJDOAnalysisResult analysisResult) {
		this.analysisResult = analysisResult;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GAEJDODatasetAnalysis))
			return false;
		GAEJDODatasetAnalysis other = (GAEJDODatasetAnalysis) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}


}
