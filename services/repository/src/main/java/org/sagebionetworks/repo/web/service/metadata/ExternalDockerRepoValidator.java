package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;
import java.util.regex.Pattern;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalDockerRepoValidator implements EntityValidator<DockerRepository> {
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private DockerNodeDao dockerNodeDao;
	
	@Autowired
	private StackConfiguration stackConfiguration;
	
	public static boolean isReserved(String registryHost) {
		if (registryHost==null) return false; // it's an implicit reference to DockerHub
		String hostSansPort = DockerNameUtil.getRegistryHostSansPort(registryHost);
		List<String> reservedHostRegexps = StackConfigurationSingleton.singleton().getDockerReservedRegistryHosts();
		for (String reservedHostRegexp : reservedHostRegexps) {
			if (Pattern.compile(reservedHostRegexp).matcher(hostSansPort).find()) 
				return true;
		}
		return false;
	}
	
	/*
	 * Allow only the creation of an external docker repository.  No update is allowed.
	 * @see org.sagebionetworks.repo.web.service.metadata.EntityValidator#validateEntity(org.sagebionetworks.repo.model.Entity, org.sagebionetworks.repo.web.service.metadata.EntityEvent)
	 */
	@Override
	public void validateEntity(DockerRepository dockerRepository, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		
		if (event.getType()!=EventType.CREATE && event.getType()!=EventType.UPDATE) {
			throw new IllegalArgumentException("Unexpected event type "+event.getType());
		}
		
		String repositoryName = dockerRepository.getRepositoryName();
		DockerNameUtil.validateName(repositoryName);
		String registryHost = DockerNameUtil.getRegistryHost(repositoryName);
		if (registryHost!=null) {
			if (stackConfiguration.getDockerRegistryHosts().contains(registryHost)) {
				throw new InvalidModelException("Cannot create or update a managed Docker repository.");
			} else if (isReserved(registryHost)) {
				throw new InvalidModelException("Cannot create or update a Docker repository having a reserved registry host.");
			}
		}
		dockerRepository.setIsManaged(false);
		String parentId = dockerRepository.getParentId();
		if (parentId==null) throw new IllegalArgumentException("parentId is required.");
		try {
			EntityType parentType = nodeDAO.getNodeTypeById(parentId);
			if (parentType!=EntityType.project) {
				throw new InvalidModelException("Parent must be a project.");
			}
		} catch (NotFoundException e) {
			throw new NotFoundException("parentId "+parentId+" does not exist.", e);
		}
		
		if (event.getType()==EventType.UPDATE) {
			if (dockerRepository.getId()==null) throw new InvalidModelException("Entity ID is required for update.");
			// Check whether entity ID of updated Docker Repository is already used for a managed repository.
			// If so, reject the update.
			String managedRepositoryName = dockerNodeDao.getRepositoryNameForEntityId(dockerRepository.getId());
			if (managedRepositoryName!=null) {
				throw new InvalidModelException("Cannot convert a managed Docker repository into an unmanaged one.");
			}
		}
	}

}
