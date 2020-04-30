package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * These services process events from the Docker Registry.
 * They are separated from other Docker Controllers because the authorization 
 * is different, i.e. basic authorization using a key/value pair.
 * 
 * These services are not intended to be used by Synapse clients, only by
 * the Docker registry.
 * 
 *
 */
@ControllerInfo(displayName="Docker Registry Event Services", path="dockerRegistryListener/v1")
@Controller
@RequestMapping(UrlHelpers.DOCKER_REGISTRY_PATH)
public class DockerRegistryEventController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Post a list of Docker registry events (pushes and pulls).  Synapse will process 
	 * accordingly.  The main purpose is to create managed Docker repositories and/or
	 * add new commits (tag/digest pairs) when a push occurs.
	 * 
	 * @param registryEvents
	 */
	@RequiredScope({})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.DOCKER_REGISTRY_EVENTS, method = RequestMethod.POST)
	public void registryEvents(@RequestBody DockerRegistryEventList registryEvents)  {
		serviceProvider.getDockerService().dockerRegistryNotification(registryEvents);
	}
}
