package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.HashSet;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessControlListObjectHelper implements DoaObjectHelper<AccessControlList> {

	@Autowired
	AccessControlListDAO aclDao;

	@Override
	public AccessControlList create(Consumer<AccessControlList> consumer) {
		AccessControlList acl = new AccessControlList();
		ObjectType ownerType = ObjectType.ENTITY;
		acl.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		acl.setCreationDate(new Date());
		acl.setResourceAccess(new HashSet<>());

		consumer.accept(acl);

		if (acl.getModifiedBy() == null) {
			acl.setModifiedBy(acl.getCreatedBy());
		}
		if (acl.getModifiedOn() == null) {
			acl.setModifiedOn(acl.getCreationDate());
		}
		String id = aclDao.create(acl, ownerType);
		return aclDao.get(id, ownerType);
	}

}
