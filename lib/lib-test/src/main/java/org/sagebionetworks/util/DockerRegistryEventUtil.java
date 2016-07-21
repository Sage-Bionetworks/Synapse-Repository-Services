package org.sagebionetworks.util;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.docker.RegistryEventActor;
import org.sagebionetworks.repo.model.docker.RegistryEventRequest;
import org.sagebionetworks.repo.model.docker.RegistryEventTarget;

public class DockerRegistryEventUtil {

	// helper function to construct registry events in the prescribed format
	public static DockerRegistryEventList createDockerRegistryEvent(
			RegistryEventAction action, String host, long userId, String repositoryPath, String tag, String digest) {
		DockerRegistryEvent event = new DockerRegistryEvent();
		event.setAction(action);
		RegistryEventRequest eventRequest = new RegistryEventRequest();
		event.setRequest(eventRequest);
		eventRequest.setHost(host);
		RegistryEventActor eventActor = new RegistryEventActor();
		event.setActor(eventActor);
		eventActor.setName(""+userId);
		RegistryEventTarget target = new RegistryEventTarget();
		target.setRepository(repositoryPath);
		target.setTag(tag);
		target.setDigest(digest);
		event.setTarget(target);
		DockerRegistryEventList eventList = new DockerRegistryEventList();
		List<DockerRegistryEvent> events = new ArrayList<DockerRegistryEvent>();
		eventList.setEvents(events);
		events.add(event);
		return eventList;
	}
	


}
