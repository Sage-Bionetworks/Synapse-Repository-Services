package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserGroupDoaObjectHelper implements DaoObjectHelper<UserGroup>{
	
	@Autowired
	private UserGroupDAO userGroupDao;

	@Override
	public UserGroup create(Consumer<UserGroup> consumer) {
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setEtag(UUID.randomUUID().toString());
		ug.setIsIndividual(true);
		
		consumer.accept(ug);
		
		Long id = userGroupDao.create(ug);
		return userGroupDao.get(id);
	}

}
