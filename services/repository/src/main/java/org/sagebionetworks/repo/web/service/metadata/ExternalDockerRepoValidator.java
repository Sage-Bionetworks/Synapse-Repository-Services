package org.sagebionetworks.repo.web.service.metadata;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.web.NotFoundException;

public class ExternalDockerRepoValidator implements EntityValidator<DockerRepository> {
	
	// list the DNS names (wildcards allowed) which are not allowed
	// to be external registries
	public static final List<String> RESERVED_REGISTRY_NAME_LIST;
	static {
		RESERVED_REGISTRY_NAME_LIST = Arrays.asList("*.synapse.org");
	};
	
	public static boolean isReserved(String registryHost) {
		return false; // TODO
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
				if (DockerNameUtil.getSynapseRegistries().contains(registryHost)) {
					throw new InvalidModelException("Cannot create a managed Docker repository.");
				} else if (isReserved(registryHost)) {
					throw new InvalidModelException("Cannot a Docker repository having a reserved registry host.");
				}
			}
			dockerRepository.setIsManaged(false);
		} else if (event.getType()==EventType.UPDATE) {
			throw new IllegalArgumentException("Update is not allowed.");
		} else {
			throw new IllegalArgumentException("Unexpected event type "+event.getType());
		}
	}

}
