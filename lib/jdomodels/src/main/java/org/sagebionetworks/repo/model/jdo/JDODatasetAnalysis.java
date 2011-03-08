package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



@PersistenceCapable(detachable = "true")
public class JDODatasetAnalysis {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;

	@Persistent
	private Collection<Long> layers;

	@Persistent
	private Long script;

	@Persistent(dependent = "true")
	private JDOAnalysisResult analysisResult;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<Long> getLayers() {
		return layers;
	}

	public void setLayers(Collection<Long> layers) {
		this.layers = layers;
	}

	public Long getScript() {
		return script;
	}

	public void setScript(Long script) {
		this.script = script;
	}

	public JDOAnalysisResult getAnalysisResult() {
		return analysisResult;
	}

	public void setAnalysisResult(JDOAnalysisResult analysisResult) {
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
		if (!(obj instanceof JDODatasetAnalysis))
			return false;
		JDODatasetAnalysis other = (JDODatasetAnalysis) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}


}
