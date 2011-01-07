package org.sagebionetworks.repo.model;

public interface AccessorFactory {
	public ProjectAccessor getProjectAccessor();
	
	public ScriptAccessor getScriptAccessor();
	
	public RevisionAccessor<Script> getScriptRevisionAccessor();
	public RevisionAccessor<Dataset> getDatasetRevisionAccessor();
	public RevisionAccessor<DatasetLayer> getDatasetLayerRevisionAccessor();
	
	public DatasetAccessor getDatasetAccessor();
	
	public void close();
}
