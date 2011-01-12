package org.sagebionetworks.repo.model.gaejdo;

import org.sagebionetworks.repo.model.AccessorFactory;
import org.sagebionetworks.repo.model.AnalysisResultAccessor;
import org.sagebionetworks.repo.model.AnnotationsAccessor;
import org.sagebionetworks.repo.model.DatasetAccessor;
import org.sagebionetworks.repo.model.DatasetAnalysisAccessor;
import org.sagebionetworks.repo.model.InputDataLayerAccessor;
import org.sagebionetworks.repo.model.ProjectAccessor;
import org.sagebionetworks.repo.model.RevisionAccessor;
import org.sagebionetworks.repo.model.ScriptAccessor;

public class AccessorFactoryImpl implements AccessorFactory {
	public ProjectAccessor getProjectAccessor() {
		return new ProjectAccessorImpl();
	}
	
	public ScriptAccessor getScriptAccessor() {
		return new ScriptAccessorImpl();
	}
	
	public RevisionAccessor<GAEJDOScript> getScriptRevisionAccessor() {
		return new RevisionAccessorImpl<GAEJDOScript>();
	}
	
	public RevisionAccessor<GAEJDODataset> getDatasetRevisionAccessor() {
		return new RevisionAccessorImpl<GAEJDODataset>();
	}
	public RevisionAccessor<GAEJDODatasetLayer> getDatasetLayerRevisionAccessor() {
		return new RevisionAccessorImpl<GAEJDODatasetLayer>();
	}
	
	public DatasetAccessor getDatasetAccessor() {
		return new DatasetAccessorImpl();
	}
	
	public InputDataLayerAccessor getInputDataLayerAccessor() {
		return new InputDataLayerAccessorImpl();
	}
	
	public AnalysisResultAccessor getAnalysisResultAccessor() {
		return new AnalysisResultAccessorImpl();
	}
	
	public AnnotationsAccessor<GAEJDODataset> getDatasetAnnotationsAccessor() {
		return new AnnotationsAccessorImpl<GAEJDODataset>(GAEJDODataset.class);
	}
	
	public DatasetAnalysisAccessor getDatasetAnalysisAccessor() {
		return new DatasetAnalysisAccessorImpl();
	}
}
