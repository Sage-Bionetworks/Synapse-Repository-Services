package org.sagebionetworks.repo.model.gaejdo;

import org.sage.datamodel.AccessorFactory;
import org.sage.datamodel.Dataset;
import org.sage.datamodel.DatasetAccessor;
import org.sage.datamodel.DatasetLayer;
import org.sage.datamodel.ProjectAccessor;
import org.sage.datamodel.RevisionAccessor;
import org.sage.datamodel.Script;
import org.sage.datamodel.ScriptAccessor;

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
	
	public void close() {PMF.close();}
}
