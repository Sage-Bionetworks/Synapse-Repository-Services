package org.sagebionetworks.repo.manager.wiki;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * WikiManager implemention.
 * 
 * @author John
 *
 */
public class WikiManagerImpl implements WikiManager {
	
	private static final String USER_IS_NOT_AUTHORIZED_TEMPLATE = "User is not authorized to '%1$s' a WikiPage with an onwerId: '%2$s' of type: '%3$s'";

	@Autowired
	WikiPageDao wikiPageDao;
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage createWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toCreate) throws NotFoundException, UnauthorizedException{
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.CREATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), objectId, objectType.name()));
		}
		// pass to the DAO
		return wikiPageDao.create(toCreate, objectId, objectType);
	}

	@Override
	public WikiPage getWikiPage(UserInfo user, WikiPageKey key) throws NotFoundException, UnauthorizedException {
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.READ)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.CREATE.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
		}
		// Pass to the DAO
		return wikiPageDao.get(key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteWiki(UserInfo user, WikiPageKey key) throws UnauthorizedException, DatastoreException, NotFoundException{
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, key.getOwnerObjectId(), key.getOwnerObjectType(), ACCESS_TYPE.DELETE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.DELETE.name(), key.getOwnerObjectId(), key.getOwnerObjectType().name()));
		}
		// Pass to the DAO
		wikiPageDao.delete(key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public WikiPage updateWikiPage(UserInfo user, String objectId,	ObjectType objectType, WikiPage toUpdate) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		// Check that the user is allowed to perform this action
		if(!authorizationManager.canAccess(user, objectId,	objectType, ACCESS_TYPE.UPDATE)){
			throw new UnauthorizedException(String.format(USER_IS_NOT_AUTHORIZED_TEMPLATE, ACCESS_TYPE.UPDATE.name(), objectId, objectType.name()));
		}
		// Before we can update the Wiki we need to lock.
		String currentEtag = wikiPageDao.lockForUpdate(toUpdate.getId());
		if(currentEtag.equals(toUpdate.getEtag())){
			throw new ConflictingUpdateException("ObjectId: "+objectId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Pass to the DAO
		return wikiPageDao.updateWikiPage(toUpdate, objectId, objectType, false);
	}

}
