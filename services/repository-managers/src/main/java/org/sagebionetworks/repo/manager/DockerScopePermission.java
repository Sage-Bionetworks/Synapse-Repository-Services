package org.sagebionetworks.repo.manager;

import java.util.Set;

/**
 * Object representing the allowed actions for a Docker scope
 * @author zdong
 *
 */
public class DockerScopePermission {
	private String scopeType;
	private String repositoryPath;
	private Set<String> permittedActions;


	public String getScopeType() {
		return scopeType;
	}
	
	public String getRepositoryPath() {
		return repositoryPath;
	}
	
	public Set<String> getPermittedActions() {
		return permittedActions;
	}
	
	public DockerScopePermission(String scopeType, String repositoryPath, Set<String> permittedActions){
		this.scopeType = scopeType;
		this.repositoryPath = repositoryPath;
		this.permittedActions = permittedActions;
	}
}