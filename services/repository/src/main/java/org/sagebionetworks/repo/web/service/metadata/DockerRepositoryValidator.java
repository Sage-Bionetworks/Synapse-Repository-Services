package org.sagebionetworks.repo.web.service.metadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.web.NotFoundException;

public class DockerRepositoryValidator implements EntityValidator<DockerRepository> {
	
	// the regexps in this file are adapted from
	// https://github.com/docker/distribution/blob/master/reference/regexp.go
	// (Note: The cited source is controlled by this license: https://github.com/docker/distribution/blob/master/LICENSE)

	// one alpha numeric or several alphanumerics with hyphens internally
	public static final String hostnameComponentRegexp = "([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])";
	public static final String hostnameRegexp = hostnameComponentRegexp+
									"(\\.|"+hostnameComponentRegexp+")*"+
									"(:[0-9]+)?";
	
	// we keep the Regexp name from Docker, but it should be called 'lowerCaseAlpha...'
	public static final String alphaNumericRegexp = "[a-z0-9]+";
	public static final String separatorRegexp = "[._]|__|[-]*";

	public static final String nameComponentRegexp = alphaNumericRegexp +
								"("+separatorRegexp+alphaNumericRegexp+")*";
	
	public static final String PathRegexp = nameComponentRegexp+"(/"+nameComponentRegexp+")*";
	
	public static final String NameRegexp = "("+hostnameRegexp+"/"+")?"+PathRegexp;
	
	public static void validateName(String name) {
		if(!Pattern.matches("^"+NameRegexp+"$", name))
			throw new IllegalArgumentException("Invalid repository name: "+name);		
	}
	
	/*
	 * The entity name for a Docker repo is [hostregistry/]path
	 * where the optional hostregistry is an IP address or CNAME with optional :PORT
	 */
	public static String getRegistryHost(String name) {
		// is the whole name just a path?
		Matcher pathMatcher = Pattern.compile("^"+PathRegexp+"$").matcher(name);
		if (pathMatcher.find()) {
			return null;
		} else {
			// the path must come after the registry host
			Matcher hostAsPrefixMatcher = Pattern.compile("^"+hostnameRegexp+"/").matcher(name);
			if (hostAsPrefixMatcher.find()) {
				String hostWithSlash = hostAsPrefixMatcher.group();
				return hostWithSlash.substring(0, hostWithSlash.length()-1);
			} else {
				throw new InvalidModelException("Invalid repository name: "+name);
			}
		}
	}
	
	/*
	 * Get the registries which delegate authorization to Synapse
	 */
	public static List<String> getSynapseRegistries() {
		// TODO read from configuration file
		return Arrays.asList("docker.synapse.org");
	}
	
	public static boolean isSynapseRegistry(String registryHost) {
		return false; // TODO 
	}
	
	public static boolean isInBlackList(String registryHost) {
		return false; // TODO
	}
	
	// list the DNS names (wildcards allowed) which are not allowed
	// to be external registries
	public static final List<String> EXTERNAL_REGISTRY_BLACKLIST;
	static {
		EXTERNAL_REGISTRY_BLACKLIST = Arrays.asList("*.synapse.org");
	};
	
	public static void validateCommits(Set<DockerCommit> commits) {
		Set<String> tags = new HashSet<String>();
		Set<String> digests = new HashSet<String>();
		for (DockerCommit commit : commits) {
			String tag = commit.getTag();
			String digest = commit.getDigest();
			// TODO make sure tag and digest are formatted correctly
			if (tag!=null && tags.contains(tag)) 
				throw new InvalidModelException("Repeated tag "+tag);
			tags.add(tag);
			if (digest==null) 
				throw new InvalidModelException("digest cannot be null.");
			if (digests.contains(digest)) 
				throw new InvalidModelException("Repeated digest "+digest);
			digests.add(digest);
		}
	}
	
	
	//	Dns/subnet blacklist (reserved) for external urls
	//	Plus white list of allowed
	//	- no changing parentId.  parent must always be the initial one.
	//	- no changing name.
	@Override
	public void validateEntity(DockerRepository dockerRepository, EntityEvent event)
			throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		
		if (event.getType()==EventType.UPDATE) {
			if (dockerRepository.getId()==null) throw new InvalidModelException("Entity ID is required.");
			// TODO retrieve the entity based on its ID
			// TODO for efficiency, retrieve entity without annotations
			// if metadata is being changed, then reject it
			// this includes parentId, isManaged, name, commits
		} else if (event.getType()==EventType.CREATE) {
			validateName(dockerRepository.getName());
			String registryHost = getRegistryHost(dockerRepository.getName());
			if (registryHost!=null) {
				// check registryHost to see if its managed and set 'isManaged' accordingly
				if (isSynapseRegistry(registryHost)) {
					throw new InvalidModelException("Cannot create a managed Docker repository.");
				} else if (isInBlackList(registryHost)) {
					throw new InvalidModelException("Cannot a Docker repository having this registry host.");
				}
			}
			dockerRepository.setIsManaged(false);
			validateCommits(dockerRepository.getCommits());
		} else {
			throw new IllegalArgumentException("Unexpected event type "+event.getType());
		}
	}

}
