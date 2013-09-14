package org.sagebionetworks.crowd.workers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;


/**
 * Synchronizes Crowd and RDS on groups and users.
 */
public class CrowdGroupSynchronizer implements Runnable {

	@Autowired
	private GroupMembersDAO crowdGroupMembersDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private UserDAO userDAOImpl; // the CrowdUserDAO
	
	private static Logger log = LogManager.getLogger(CrowdGroupSynchronizer.class); 
	
	private enum CROWD_TYPE {
		group, 
		user
	}
	
	@Override
	public void run() {
		List<String> crowdGroups = getCrowdGroups(CROWD_TYPE.group);
		List<String> crowdUsers = getCrowdGroups(CROWD_TYPE.user);
		List<String> principalIds = new ArrayList<String>();
		
		// Make sure all the Crowd groups exist in RDS
		for (int i = 0; i < crowdGroups.size() + crowdUsers.size(); i++) {
			boolean isIndividual = i >= crowdGroups.size();
			String name = isIndividual ? crowdUsers.get(i - crowdGroups.size()) : crowdGroups.get(i);
			String principalId;
			
			// Make sure a UserGroup exists for each user/group 
			if (userGroupDAO.doesPrincipalExist(name)) {
				principalId = userGroupDAO.findGroup(name, isIndividual).getId();
			} else {
				UserGroup ug = new UserGroup();
				ug.setIsIndividual(isIndividual);
				ug.setName(name);
				principalId = userGroupDAO.create(ug);
			}
			
			// Make sure a profile exists for each user
			if (isIndividual) {
				User user;
				try {
					user = userDAOImpl.getUser(URLEncoder.encode(name, "UTF-8"));
				} catch (DatastoreException e) {
					throw new RuntimeException(e);
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}

				UserProfile userProfile;
				try {
					userProfile = userProfileDAO.get(principalId);
				} catch (NotFoundException e) {
					// Must make a new profile
					try {
						userProfile = new UserProfile();
						userProfile.setOwnerId(principalId);
						userProfile.setFirstName(user.getFname());
						userProfile.setLastName(user.getLname());
						userProfile.setDisplayName(user.getDisplayName());
						userProfileDAO.create(userProfile);
					} catch (InvalidModelException ime) {
						throw new RuntimeException(ime);
					}
					
					// Re-fetch the profile after making it
					try {
						userProfile = userProfileDAO.get(principalId);
					} catch (NotFoundException nfe) {
						throw new RuntimeException(nfe);
					}
				}
				
				// For profiles that already exist, migrate the boolean for the termsOfUse over
				long termsTimeStamp;
				if (user.isAgreesToTermsOfUse() && user.getCreationDate() != null) {
					termsTimeStamp = user.getCreationDate().getTime() / 1000;
				} else {
					termsTimeStamp = 0L;
				}
				
				// Don't needlessly re-update the profile with the same data
				if (userProfile.getAgreesToTermsOfUse() == null 
						|| userProfile.getAgreesToTermsOfUse().longValue() < termsTimeStamp) {
					userProfile.setAgreesToTermsOfUse(termsTimeStamp);
					try {
						userProfileDAO.update(userProfile);
					} catch (DatastoreException e) {
						throw new RuntimeException(e);
					} catch (InvalidModelException e) {
						throw new RuntimeException(e);
					} catch (ConflictingUpdateException e) {
						throw new RuntimeException(e);
					} catch (NotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
			principalIds.add(principalId);
		}

		// Copy the un-nested group membership from Crowd to the GroupMembersDAO
		// Since there is no exposed method for modifying groups on RDS, 
		//   we can delete/create entries with impunity
		for (int i = 0; i < principalIds.size(); i++) {
			boolean isIndividual = i >= crowdGroups.size();
			try {
				if (!isIndividual) {
					// Add/remove the minimal number of elements from each group
					String principalId = principalIds.get(i);
					List<UserGroup> existing = groupMembersDAO.getMembers(principalId);
					List<UserGroup> newbies = crowdGroupMembersDAO.getMembers(principalId);
					
					Set<UserGroup> toDelete = new HashSet<UserGroup>(existing);
					toDelete.removeAll(newbies);
					
					Set<UserGroup> toAdd = new HashSet<UserGroup>(newbies);
					toAdd.removeAll(existing);
					
					List<String> operator = new ArrayList<String>();
					for (UserGroup ug : toDelete) {
						operator.add(ug.getId());
					}
					groupMembersDAO.removeMembers(principalId, operator);
	
					operator.clear();
					for (UserGroup ug : toAdd) {
						operator.add(ug.getId());
					}
					groupMembersDAO.addMembers(principalId, operator);
				}
			} catch (NotFoundException e) {
				log.warn("A group was deleted before the worker could finish processing it", e);
			}
		}
	}
	
	/**
	 * Returns a list of names of groups fetched from Crowd
	 * @param entityType Either "user" or "group"
	 * @return 
	 */
	private List<String> getCrowdGroups(CROWD_TYPE entityType) {
		try {
			byte[] sessionXml = CrowdAuthUtil.executeRequest(
					CrowdAuthUtil.urlPrefix()+"/search?entity-type="+entityType.name(), 
					"GET", "", HttpStatus.OK, "Could not perform query");
			return CrowdAuthUtil.getMultiFromXML("/"+entityType.name()+"s/"+entityType.name()+"/@name", sessionXml);
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} 
	}
}
