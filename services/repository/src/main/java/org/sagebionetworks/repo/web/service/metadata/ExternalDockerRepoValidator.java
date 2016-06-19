package org.sagebionetworks.repo.web.service.metadata;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalDockerRepoValidator implements EntityValidator<DockerRepository> {
	@Autowired
	private NodeDAO nodeDAO;

		
	public static boolean isReserved(String registryHost) {
		if (registryHost==null) return false; // it's an implicit reference to DockerHub
		String hostSansPort = DockerNameUtil.getRegistryHostSansPort(registryHost);
		List<String> reservedHostRegexps = StackConfiguration.getDockerReservedRegistryHosts();
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
		
		if (event.getType()==EventType.CREATE) {
			DockerNameUtil.validateName(dockerRepository.getName());
			String registryHost = DockerNameUtil.getRegistryHost(dockerRepository.getName());
			if (registryHost!=null) {
				if (StackConfiguration.getDockerRegistryHosts().contains(registryHost)) {
					throw new InvalidModelException("Cannot create a managed Docker repository.");
				} else if (isReserved(registryHost)) {
					throw new InvalidModelException("Cannot create a Docker repository having a reserved registry host.");
				}
			}
			dockerRepository.setIsManaged(false);
			String parentId = dockerRepository.getParentId();
			if (parentId==null) throw new IllegalArgumentException("parentId is required.");
			List<EntityHeader> headers = nodeDAO.getEntityHeader(Collections.singleton(KeyFactory.stringToKey(parentId)));
			if (headers.size()==0) throw new NotFoundException("parentId "+parentId+" does not exist.");
			if (headers.size()>1) throw new IllegalStateException("Expected 0-1 result for "+parentId+" but found "+headers.size());
			if (EntityTypeUtils.getEntityTypeForClassName(headers.get(0).getType())!=EntityType.project) {
				throw new IllegalArgumentException("Parent must be a project.");
			}
		} else if (event.getType()==EventType.UPDATE) {
			throw new IllegalArgumentException("Update is not allowed.");
		} else {
			throw new IllegalArgumentException("Unexpected event type "+event.getType());
		}
	}

}
