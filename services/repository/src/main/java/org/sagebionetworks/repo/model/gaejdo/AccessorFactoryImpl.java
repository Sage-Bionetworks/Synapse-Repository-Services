package org.sagebionetworks.repo.model.gaejdo;

import org.sagebionetworks.repo.model.AccessorFactory;
import org.sagebionetworks.repo.model.AnalysisResultAccessor;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetAccessor;
import org.sagebionetworks.repo.model.DatasetLayer;
import org.sagebionetworks.repo.model.InputDataLayerAccessor;
import org.sagebionetworks.repo.model.ProjectAccessor;
import org.sagebionetworks.repo.model.RevisionAccessor;
import org.sagebionetworks.repo.model.Script;
import org.sagebionetworks.repo.model.ScriptAccessor;

public class AccessorFactoryImpl implements AccessorFactory {
	public ProjectAccessor getProjectAccessor() {
		return new ProjectAccessorImpl(PMF.get());
	}
	
	public ScriptAccessor getScriptAccessor() {
		return new ScriptAccessorImpl(PMF.get());
	}
	
	public RevisionAccessor<Script> getScriptRevisionAccessor() {
		return new RevisionAccessorImpl<Script>(PMF.get());
	}
	
	public RevisionAccessor<Dataset> getDatasetRevisionAccessor() {
		return new RevisionAccessorImpl<Dataset>(PMF.get());
	}
	public RevisionAccessor<DatasetLayer> getDatasetLayerRevisionAccessor() {
		return new RevisionAccessorImpl<DatasetLayer>(PMF.get());
	}
	
	public DatasetAccessor getDatasetAccessor() {
		return new DatasetAccessorImpl(PMF.get());
	}
	
	public InputDataLayerAccessor getInputDataLayerAccessor() {
		return new InputDataLayerAccessorImpl(PMF.get());
	}
	
	public AnalysisResultAccessor getAnalysisResultAccessor() {
		return new AnalysisResultAccessorImpl(PMF.get());
	}
	
	public void close() {PMF.close();}
}
