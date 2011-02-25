package org.sagebionetworks.repo.model;

/**
 * This is the interface implemented by DAO Factories
 * 
 * @author bhoff
 * 
 */
public interface DAOFactory {

	public ProjectDAO getProjectDAO();

	public ScriptDAO getScriptDAO();

	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public DatasetDAO getDatasetDAO(String userId);
	
	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserDAO getUserDAO(String userId);

	/**
	 * @param userId
	 * @return
	 */
	public UserCredentialsDAO getUserCredentialsDAO(String userId);
	
	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserGroupDAO getUserGroupDAO(String userId);

	public DatasetAnalysisDAO getDatasetAnalysisDAO();

	public LayerPreviewDAO getLayerPreviewDAO(String userId);

	public LayerLocationsDAO getLayerLocationsDAO(String userId);
}
