package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.gaejdo.GAEJDODatasetLayer;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODataset;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOScript;

public interface AccessorFactory {
	public ProjectAccessor getProjectAccessor();
	
	public ScriptAccessor getScriptAccessor();
	
	public RevisionAccessor<GAEJDOScript> getScriptRevisionAccessor();
	public RevisionAccessor<GAEJDODataset> getDatasetRevisionAccessor();
	public RevisionAccessor<GAEJDODatasetLayer> getDatasetLayerRevisionAccessor();
	
	public DatasetAccessor getDatasetAccessor();
	public InputDataLayerAccessor getInputDataLayerAccessor();
	public AnalysisResultAccessor getAnalysisResultAccessor();
	
	public AnnotationsAccessor<GAEJDODataset> getDatasetAnnotationsAccessor();
	
	public DatasetAnalysisAccessor getDatasetAnalysisAccessor();
}
