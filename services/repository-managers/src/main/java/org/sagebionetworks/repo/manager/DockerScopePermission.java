package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.docker.RegistryEventAction;

/**
 * Object representing the allowed actions for a Docker scope
 * @author zdong
 *
 */
public class DockerScopePermission {
	private String scopeType;
	private String repositoryPath;
	private Set<RegistryEventAction> permittedActions;


	public String getScopeType() {
		return scopeType;
	}
	
	public String getRepositoryPath() {
		return repositoryPath;
	}
	
	public Set<RegistryEventAction> getPermittedActions() {
		return permittedActions;
	}
	
	public DockerScopePermission(String scopeType, String repositoryPath, Set<RegistryEventAction> permittedActions){
		this.scopeType = scopeType;
		this.repositoryPath = repositoryPath;
		this.permittedActions = permittedActions;
	}
}