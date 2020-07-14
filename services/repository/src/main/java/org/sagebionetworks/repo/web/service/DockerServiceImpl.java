package org.sagebionetworks.repo.web.service;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.DockerManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerServiceImpl implements DockerService {
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DockerManager dockerManager;
	
	@Autowired
	private Consumer consumer;

	private static final Logger log = LogManager.getLogger(DockerServiceImpl.class);

	@Override
	public DockerAuthorizationToken authorizeDockerAccess(Long userId, String accessToken, 
			String service, List<String> scopes) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return dockerManager.authorizeDockerAccess(userInfo, accessToken, service, scopes);
	}

	@Override
	public void addDockerCommit(Long userId, String entityId, DockerCommit dockerCommit) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		dockerManager.addDockerCommitToUnmanagedRespository(userInfo, entityId, dockerCommit);
	}

	@Override
	public PaginatedResults<DockerCommit> listDockerTags(Long userId,
			String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset) {
				UserInfo userInfo = userManager.getUserInfo(userId);
		return dockerManager.listDockerTags(userInfo, entityId, sortBy, ascending, limit, offset);
	}
	
	public void log(Exception e) {
		log.error("DockerServiceImpl error", e);
		// log twice, once with just the label
		ProfileData logEvent = new ProfileData();
		logEvent.setNamespace(DockerService.class.getName());
		logEvent.setName(e.getClass().getName()+" "+e.getMessage());
		logEvent.setValue(1.0);
		logEvent.setUnit("Count");
		logEvent.setTimestamp(new Date());
		consumer.addProfileData(logEvent);
	}

	@Override
	public void dockerRegistryNotification(
			DockerRegistryEventList registryEvents) {
		try {
			dockerManager.dockerRegistryNotification(registryEvents);
		} catch (Exception e) {
			log(e);
		}
	}

}
