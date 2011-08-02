package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOUserGroupDAOImpl extends
		JDOBaseDAOImpl<UserGroup, JDOUserGroup> implements
		UserGroupDAOInitializingBean {
	
	@Autowired
	private UserGroupCache userGroupCache;

	UserGroup newDTO() {
		UserGroup ug = new UserGroup();
		// ug.setCreatableTypes(new HashSet<String>());
		return ug;
	}

	JDOUserGroup newJDO() {
		JDOUserGroup g = new JDOUserGroup();
		return g;
	}

	void copyToDto(JDOUserGroup jdo, UserGroup dto) throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setName(jdo.getName());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setIndividual(jdo.getIsIndividual());
	}

	void copyFromDto(UserGroup dto, JDOUserGroup jdo)
			throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setIsIndividual(dto.isIndividual());
	}

	Class<JDOUserGroup> getJdoClass() {
		return JDOUserGroup.class;
	}

	/**
	 * @return the group matching the given name, and the given 'individual'
	 *         property
	 */
	public UserGroup findGroup(String name, boolean isIndividual)
			throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> groups = exec.execute(JDOUserGroup.class,
				"pName==this.name && isIndividual==pIndividual",
				String.class.getName() + " pName, " + Boolean.class.getName()
						+ " pIndividual", null, name, isIndividual);
		if (groups.size() > 1)
			throw new DatastoreException("Expected 0-1 but found "
					+ groups.size() + " for " + name);
		if (groups.size() == 0)
			return null;
		JDOUserGroup jdo = groups.iterator().next();
		UserGroup dto = new UserGroup();
		copyToDto(jdo, dto);
		return dto;
	}

	/**
	 * @return the NON-system groups and the admin group (if applicable) for the
	 *         list of group names
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupNames)
			throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> groups = exec.execute(
				JDOUserGroup.class,
				"pGroupNames.contains(this.name) && isIndividual==false",
				Collection.class.getName() + " pGroupNames, "
						+ String.class.getName() + " pAdminGroupName, ", null,
				groupNames, AuthorizationConstants.ADMIN_GROUP_NAME);
		Map<String, UserGroup> ans = new HashMap<String, UserGroup>();
		for (JDOUserGroup jdo : groups) {
			UserGroup dto = new UserGroup();
			copyToDto(jdo, dto);
			ans.put(dto.getName(), dto);
		}
		return ans;
	}

	/**
	 * a variant of the generic 'getAll' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */
	@Transactional(readOnly = true)
	public Collection<UserGroup> getAll(boolean isIndividual)
			throws DatastoreException {
		try {
			JDOExecutor exec = new JDOExecutor(jdoTemplate);
			List<JDOUserGroup> all = exec.execute(getJdoClass(),
					"isIndividual==" + isIndividual, null, null);
			return copyToDtoCollection(all);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * a variant of the generic 'getInRange' query, this allows the caller to
	 * separately retrieve the individual and non-individual groups.
	 */
	@Transactional(readOnly = true)
	public List<UserGroup> getInRange(long fromIncl, long toExcl,
			boolean isIndividual) throws DatastoreException {
		try {
			JDOExecutor exec = new JDOExecutor(jdoTemplate);
			List<JDOUserGroup> all = exec.execute(getJdoClass(),
					"isIndividual==" + isIndividual, null, null, fromIncl,
					toExcl, defaultSortField());
			return copyToDtoCollection(all);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	// initialization of UserGroups
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void afterPropertiesSet() throws Exception {
		// ensure public group is created
		// Make sure all of the default groups exist
		DEFAULT_GROUPS[] groups = DEFAULT_GROUPS.values();
		for (DEFAULT_GROUPS group : groups) {
			UserGroup pg = findGroup(group.name(), false);
			if (pg == null) {
				pg = new UserGroup();
				pg.setName(group.name());
				pg.setIndividual(false);
				create(pg);
			}
		}

		// ensure the anonymous principal is created
		UserGroup anon = findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		if (anon == null) {
			anon = new UserGroup();
			anon.setName(AuthorizationConstants.ANONYMOUS_USER_ID);
			anon.setIndividual(true);
			create(anon);
		}
	}

	@Override
	String defaultSortField() {
		return "name";
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		// The cache needs to remove this as well.
		userGroupCache.delete(KeyFactory.stringToKey(id));
		// The the base do the work
		super.delete(id);
	}

}
