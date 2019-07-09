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

import java.util.Collections;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import com.google.common.collect.Sets;


/**
 * This class includes constants that are shared by both the backend and the GWT Portal frontend
 */
public class ModelConstants {

	public static final String VALID_ENTITY_NAME_REGEX = "^[a-zA-Z0-9,_. \\-+()]+";

	public static final Set<ACCESS_TYPE> ENTITY_ADMIN_ACCESS_PERMISSIONS =
			Sets.newHashSet(READ, DOWNLOAD, UPDATE, DELETE, CREATE, CHANGE_PERMISSIONS, CHANGE_SETTINGS, MODERATE);
	
	public static final Set<ACCESS_TYPE> EVALUATION_ADMIN_ACCESS_PERMISSIONS = Sets.newHashSet(CREATE, READ, SUBMIT,
			READ_PRIVATE_SUBMISSION, UPDATE_SUBMISSION, CHANGE_PERMISSIONS, UPDATE, DELETE, DELETE_SUBMISSION);

	// Maximum permissions that may be granted to public, anonymous users on evaluations
	public static final Set<ACCESS_TYPE> EVALUATION_PUBLIC_MAXIMUM_ACCESS_PERMISSIONS = Collections.singleton(READ);
	public static final Set<ACCESS_TYPE> EVALUATION_AUTH_USER_MAXIMUM_ACCESS_PERMISSIONS = Collections.singleton(READ);
	public static final Set<ACCESS_TYPE> EVALUATION_ANONYMOUS_MAXIMUM_ACCESS_PERMISSIONS = Collections.singleton(READ);

	public static final Set<ACCESS_TYPE> TEAM_ADMIN_PERMISSIONS = Sets.newHashSet(READ, SEND_MESSAGE, UPDATE, DELETE, TEAM_MEMBERSHIP_UPDATE);

	/*
	 * These are the default permissions for non-admin members joining a Team
	 */
	public static final Set<ACCESS_TYPE> TEAM_MESSENGER_PERMISSIONS = Sets.newHashSet(READ, SEND_MESSAGE);
		
}
