package org.sagebionetworks.repo.model.util;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;

public class ModelConstants {
	
	public static final String VALID_ENTITY_NAME_REGEX = "^[a-z,A-Z,0-9,_,., ,\\-,\\+,(,)]+";

	public static final Set<ACCESS_TYPE> ENITY_ADMIN_ACCESS_PERMISSIONS = new HashSet<ACCESS_TYPE>(
			Arrays.asList(READ, UPDATE, DELETE, CREATE, CHANGE_PERMISSIONS, CHANGE_SETTINGS));
	
	public static final Set<ACCESS_TYPE> EVALUATION_ADMIN_ACCESS_PERMISSIONS = new HashSet<ACCESS_TYPE>(
			Arrays.asList(READ, SUBMIT, READ_PRIVATE_SUBMISSION, UPDATE_SUBMISSION, 
					CHANGE_PERMISSIONS, UPDATE, DELETE, DELETE_SUBMISSION));
		

}
