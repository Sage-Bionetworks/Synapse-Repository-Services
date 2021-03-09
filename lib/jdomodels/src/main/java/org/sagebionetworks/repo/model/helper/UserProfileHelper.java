package org.sagebionetworks.repo.model.helper;

import java.util.function.Consumer;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProfileHelper implements DaoObjectHelper<UserProfile> {

	private UserGroupDoaObjectHelper userGroupHelper;
	private UserProfileDAO userProfileDao;
	
	@Autowired
	public UserProfileHelper(UserGroupDoaObjectHelper userGroupHelper, UserProfileDAO userProfileDAO) {
		this.userGroupHelper = userGroupHelper;
		this.userProfileDao = userProfileDAO;
	}
	
	@Override
	public UserProfile create(Consumer<UserProfile> consumer) {
		UserProfile up = new UserProfile();
		
		UserGroup ug = userGroupHelper.create((u) -> {});
		up.setOwnerId(ug.getId());
		
		consumer.accept(up);
		
		String profileId = userProfileDao.create(up);
		
		return userProfileDao.get(profileId);
	}

}
