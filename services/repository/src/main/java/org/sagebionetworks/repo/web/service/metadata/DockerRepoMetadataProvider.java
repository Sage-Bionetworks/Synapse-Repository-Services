package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerRepository;

public class DockerRepoMetadataProvider implements TypeSpecificCreateProvider<DockerRepository>, TypeSpecificUpdateProvider<DockerRepository> {

	
	@Override
	public void entityCreated(UserInfo userInfo, DockerRepository dockerRepository) {
	}


	@Override
	public void entityUpdated(UserInfo userInfo, DockerRepository dockerRepository) {
	}

}
