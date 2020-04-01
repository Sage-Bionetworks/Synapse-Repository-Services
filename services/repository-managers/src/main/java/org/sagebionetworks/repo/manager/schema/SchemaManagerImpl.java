package org.sagebionetworks.repo.manager.schema;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.manager.PermissionsManagerUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.OrganizationRequest;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class SchemaManagerImpl implements SchemaManager {

	public static final String BAD_ORGANIZATION_NAME_MESSAGE = "Organziation name must start and end with a letter [a-z], and can contain digits [0-9] and periods [.]";

	public static int MAX_ORGANZIATION_NAME_CHARS = 250;
	public static int MIN_ORGANZIATION_NAME_CHARS = 3;
	public static Pattern ORGANIZATOIN_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9.]+[a-z]");

	@Autowired
	OrganizationDao organizationDao;

	@Autowired
	AccessControlListDAO aclDao;

	public static final Set<ACCESS_TYPE> ADMIN_PERMISSIONS = Sets.newHashSet(READ, CHANGE_PERMISSIONS, UPDATE, DELETE);

	@WriteTransaction
	@Override
	public Organization createOrganziation(UserInfo user, OrganizationRequest request) {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "OrganizationRequest");

		AuthorizationUtils.disallowAnonymous(user);

		String processedOrganizationName = processAndValidateOrganizationName(request.getOrganizationName());
		Organization org = organizationDao.createOrganization(processedOrganizationName, user.getId());

		// Create an ACL for the
		AccessControlList acl = AccessControlListUtil.createACL(org.getId(), user, ADMIN_PERMISSIONS, new Date());
		aclDao.create(acl, ObjectType.ORGANIZATION);

		return org;
	}

	/**
	 * If the provided organization name is not valid, an IllegalArgumentException
	 * will be thrown. The process name will be trimmed and converted to lower case.
	 * 
	 * @param name
	 * @return
	 */
	public static String processAndValidateOrganizationName(String name) {
		ValidateArgument.required(name, "organizationName");
		String processedName = name.trim().toLowerCase();
		if (processedName.length() > MAX_ORGANZIATION_NAME_CHARS) {
			throw new IllegalArgumentException(
					"Organization name must be " + MAX_ORGANZIATION_NAME_CHARS + " characters or less");
		}
		if (processedName.length() < MIN_ORGANZIATION_NAME_CHARS) {
			throw new IllegalArgumentException(
					"Organization name must be at least " + MIN_ORGANZIATION_NAME_CHARS + " chracters");
		}
		if (!ORGANIZATOIN_NAME_PATTERN.matcher(processedName).matches()) {
			throw new IllegalArgumentException(BAD_ORGANIZATION_NAME_MESSAGE);
		}
		return processedName;
	}

	@Override
	public AccessControlList getOrganizationAcl(UserInfo user, String organziationId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(organziationId, "organziationId");
		// Validate read access.
		aclDao.canAccess(user, organziationId, ObjectType.ORGANIZATION, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return aclDao.get(organziationId, ObjectType.ORGANIZATION);
	}

	@WriteTransaction
	@Override
	public AccessControlList updateOrganizationAcl(UserInfo user, String organziationId, AccessControlList acl) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(organziationId, "organziationId");
		ValidateArgument.required(acl, "acl");
		Long organziationIdLong;
		try {
			organziationIdLong = Long.parseLong(organziationId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid organziationId: " + organziationId);
		}
		// id must match the value from the URL path.
		acl.setId(organziationId);

		// Ensure the user does not revoke their own access to the ACL.
		PermissionsManagerUtils.validateACLContent(acl, user, organziationIdLong);

		// Validate CHANGE_PERMISSIONS
		aclDao.canAccess(user, organziationId, ObjectType.ORGANIZATION, ACCESS_TYPE.CHANGE_PERMISSIONS)
				.checkAuthorizationOrElseThrow();

		aclDao.update(acl, ObjectType.ORGANIZATION);
		return aclDao.get(organziationId, ObjectType.ORGANIZATION);
	}

}
