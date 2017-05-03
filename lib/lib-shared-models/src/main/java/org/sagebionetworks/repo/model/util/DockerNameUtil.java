package org.sagebionetworks.repo.model.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DockerNameUtil {
	// the regexps in this file are adapted from
	// https://github.com/docker/distribution/blob/master/reference/regexp.go
	// (Note: The cited source is controlled by this license: https://github.com/docker/distribution/blob/master/LICENSE)

	// one alpha numeric or several alphanumerics with hyphens internally
	public static final String hostnameComponentRegexp = "([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])";
	public static final String hostnameRegexp = hostnameComponentRegexp+
									"(\\.|"+hostnameComponentRegexp+")*"+"(:[0-9]+)?";
	
	// we keep the Regexp name from Docker.  It could more accurately be called 'lowerCaseAlpha...'
	public static final String alphaNumericRegexp = "[a-z0-9]+";
	
	public static final String separatorRegexp = "[._]|__|[-]*";

	public static final String nameComponentRegexp = alphaNumericRegexp +
			"("+separatorRegexp+alphaNumericRegexp+")*";

	public static final String REPO_NAME_PATH_SEP = "/";
	
	// here we deviate slightly from the .go code:  By requiring that the first part of the path not have
	// separator characters we can differentiate between a host name (like 'quay.io') and a repo name.
	// This is consistent with the use of repo paths in Dockerhub (where the first field is a user or
	// organzation name, with no separator characters) and Synapse (where the first field is a Synapse ID).
	public static final String PathRegexp = alphaNumericRegexp+"("+REPO_NAME_PATH_SEP+nameComponentRegexp+")*";
	
	public static final String NameRegexp = "("+hostnameRegexp+REPO_NAME_PATH_SEP+")?"+PathRegexp;
	
	public static void validateName(String name) {
		if(name.endsWith("/") || !Pattern.matches("^"+NameRegexp+"$", name))
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
			Matcher hostAsPrefixMatcher = Pattern.compile("^"+hostnameRegexp+REPO_NAME_PATH_SEP).matcher(name);
			if (hostAsPrefixMatcher.find()) {
				String hostWithSlash = hostAsPrefixMatcher.group();
				return hostWithSlash.substring(0, hostWithSlash.length()-1);
			} else {
				throw new IllegalArgumentException("Invalid repository name: "+name);
			}
		}
	}
	
	/*
	 * Given a valid host name with optional port, return just the host name
	 */
	public static String getRegistryHostSansPort(String host) {
		if (host==null) return null;
		int colon = host.indexOf(":");
		if (colon<0) return host;
		return host.substring(0, colon);
	}
	
	public static String getParentIdFromRepositoryPath(String name) {
		int i = name.indexOf(REPO_NAME_PATH_SEP);
		String result = name;
		if (i>0) result = name.substring(0, i);
		// validate that the string is a valid ID (i.e. "syn" followed by a number)
		if (!result.startsWith("syn")) throw new IllegalArgumentException("Repository path must start with 'syn'.");
		try {
			Long.parseLong(result.substring(3));
		} catch (NumberFormatException e) {
			 throw new IllegalArgumentException("Repository path must start with project ID: 'syn', followed by a number.");
		}
		return result;
	}


}
