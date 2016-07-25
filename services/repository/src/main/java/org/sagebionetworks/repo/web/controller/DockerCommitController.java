package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.docker.DockerCommit;

public class DockerCommitController {
	
	// TODO make sure this only adds commits for external repositories
	public DockerCommit addDockerCommit() {
		return null;
	}
	
	// TODO return a list of Docker commits, only the latest for each tag
	// commit list params:  offset, limit, asc/desc, alpha/time
	// make sure pattern matches those of other services
	public void listDockerCommits() {
		
	}

}
