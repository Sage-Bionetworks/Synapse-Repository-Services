package org.sagebionetworks.repo.model.util;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.MODERATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SEND_MESSAGE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;

public class ModelConstants {
	
	public static final String VALID_ENTITY_NAME_REGEX = "^[a-z,A-Z,0-9,_,., ,\\-,\\+,(,)]+";

	public static final Set<ACCESS_TYPE> ENITY_ADMIN_ACCESS_PERMISSIONS = new HashSet<ACCESS_TYPE>(
		Arrays.asList(READ, DOWNLOAD, UPDATE, DELETE, CREATE, CHANGE_PERMISSIONS, CHANGE_SETTINGS, MODERATE));
	
	public static final Set<ACCESS_TYPE> EVALUATION_ADMIN_ACCESS_PERMISSIONS = new HashSet<ACCESS_TYPE>(
			Arrays.asList(CREATE, READ, SUBMIT, READ_PRIVATE_SUBMISSION, UPDATE_SUBMISSION, 
					CHANGE_PERMISSIONS, UPDATE, DELETE, DELETE_SUBMISSION));
	
	public static final Set<ACCESS_TYPE> TEAM_ADMIN_PERMISSIONS = new HashSet<ACCESS_TYPE>(
		Arrays.asList(READ, 
		SEND_MESSAGE, 
		UPDATE, 
		DELETE, 
		TEAM_MEMBERSHIP_UPDATE));

	/*
	 * These are the default permissions for non-admin members joining a Team
	 */
	public static final Set<ACCESS_TYPE> TEAM_MESSENGER_PERMISSIONS = new HashSet<ACCESS_TYPE>(
			Arrays.asList(READ, SEND_MESSAGE));
		
}
