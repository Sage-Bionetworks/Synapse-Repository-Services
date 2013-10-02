package org.sagebionetworks.auth.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CrowdSynchronizerService {

	@Autowired
	private UserDAO userDAOImpl; // the CrowdUserDAO

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private UserProfileDAO userProfileDAO;

	@Autowired
	private GroupMembersDAO crowdGroupMembersDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private AuthenticationDAO authDAO;

	private static Logger log = LogManager
			.getLogger(CrowdSynchronizerService.class);

	/**
	 * Pulls data from Crowd into RDS 
	 * Note: does not create any principals
	 * 
	 * Data includes: 
	 * - Basic profile info (only if the profile does not exist)
	 * - Terms of use acceptance 
	 * - API keys 
	 * - Password hashes
	 * - Group membership 
	 */
	public void migrateAll() {
		// Get all the users that exist in Crowd
		List<String> crowdUsers = getUsersFromCrowd();
		
		// Migrate each user over, logging any exceptions that occur
		for (int i = 0; i < crowdUsers.size(); i++) {
			try {
				migrateUser(crowdUsers.get(i));
			} catch (Exception e) {
				log.error(e);
			}
		}
		
		// Migrate each group over
		List<String> crowdGroups = getGroupsFromCrowd();
		
		// Migrate each group over, logging any exceptions that occur
		// Since Crowd's group membership is not nest-able, 
		//   the groups can be created as they are processed
		for (int i = 0; i < crowdGroups.size(); i++) {
			String groupName = crowdGroups.get(i);
			String groupId = ensureGroupExists(groupName);
			try {
				migrateGroup(groupId);
			} catch (Exception e) {
				log.error(e);
			}
		}
		
		log.debug("Finished migrating");
	}

	/**
	 * Returns a list of names of users residing in Crowd
	 */
	private List<String> getUsersFromCrowd() {
		try {
			byte[] sessionXml = CrowdAuthUtil.executeRequest(
					CrowdAuthUtil.urlPrefix() + "/search?entity-type=user",
					"GET", "", HttpStatus.OK, "Could not perform query");

			List<String> users = CrowdAuthUtil.getMultiFromXML(
					"/users/user/@name", sessionXml);
			return users;
			
		// Failure at this step is catastrophic
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a list of names of groups residing in Crowd
	 */
	private List<String> getGroupsFromCrowd() {
		try {
			byte[] sessionXml = CrowdAuthUtil.executeRequest(
					CrowdAuthUtil.urlPrefix() + "/search?entity-type=group",
					"GET", "", HttpStatus.OK, "Could not perform query");

			List<String> groups = CrowdAuthUtil.getMultiFromXML(
					"/groups/group/@name", sessionXml);
			return groups;
			
		// Failure at this step is catastrophic
		} catch (AuthenticationException e) {
			throw new RuntimeException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Migrates the user's info from Crowd into RDS
	 */
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	protected void migrateUser(String username) throws Exception {
		if (!userGroupDAO.doesPrincipalExist(username)) {
			log.info("User " + username + " does not exist in RDS");
			return;
		}
		
		String principalId = userGroupDAO.findGroup(username, true).getId();

		// Fetch general information from Crowd via the CrowdUserDAO
		User userInfo = userDAOImpl.getUser(URLEncoder.encode(username, "UTF-8"));
		
		// Create the user's profile if necessary
		ensureUserProfileExists(userInfo, principalId);
		
		// Convert the boolean ToU acceptance state in User to a timestamp in the UserProfile
		// This will coincidentally re-serialize the profile's blob via the non-deprecated method
		//   See: https://sagebionetworks.jira.com/browse/PLFM-1756
		migrateToU(userInfo, principalId);
		
		// Get the user's secret key and stash it
		migrateSecretKey(username, principalId);
		
		// Get the user's password hash and stash it
		migratePassword(username);
	}
	
	/**
	 * Creates a default UserProfile for the user if necessary
	 */
	private void ensureUserProfileExists(User user, String principalId) throws InvalidModelException {
		try {
			userProfileDAO.get(principalId);
		} catch (NotFoundException e) {
			// Must make a new profile
			UserProfile userProfile = new UserProfile();
			userProfile.setOwnerId(principalId);
			userProfile.setFirstName(user.getFname());
			userProfile.setLastName(user.getLname());
			userProfile.setDisplayName(user.getDisplayName());
			userProfileDAO.create(userProfile);
		}
	}

	/**
	 * Converts the boolean user.isAgreesToTermsOfUse() to a timestamp stored in the UserProfile
	 */
	private void migrateToU(User user, String principalId) throws NotFoundException {
		UserProfile userProfile = userProfileDAO.get(principalId);
		
		// Migrate the boolean for the Terms of Use over
		long termsTimeStamp;
		if (user.isAgreesToTermsOfUse()
				&& user.getCreationDate() != null) {
			termsTimeStamp = user.getCreationDate().getTime() / 1000;
		} else {
			termsTimeStamp = 0L;
		}

		userProfile.setAgreesToTermsOfUse(termsTimeStamp);
		userProfileDAO.update(userProfile);
	}
	
	/**
	 * Copies the user's secret key from Crowd to RDS
	 */
	private void migrateSecretKey(String username, String principalId) throws IOException, NotFoundException {
		// Get the key from Crowd
		Map<String,Collection<String>> userAttributes = 
			 new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(username));
		Collection<String> secretKeyCollection = userAttributes.get(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE);
		String secretKey = null;
		if (secretKeyCollection != null && !secretKeyCollection.isEmpty()) {
			secretKey = secretKeyCollection.iterator().next();
		}
		
		// Stash the key in the AuthDAO
		if (secretKey != null) {
			authDAO.changeSecretKey(principalId, secretKey);
		}
	}
	
	private void migratePassword(String username) throws NotFoundException {
		authDAO.migratePasswordHashFromCrowd(username);
	}
	
	private String ensureGroupExists(String groupName) {
		UserGroup ug = userGroupDAO.findGroup(groupName, false);
		if (ug == null) {
			ug = new UserGroup();
			ug.setName(groupName);
			ug.setIsIndividual(false);
			userGroupDAO.create(ug);
			ug = userGroupDAO.findGroup(groupName, false);
		}
		return ug.getId();
	}

	/**
	 * Synchronizes the group's memberships from Crowd to RDS
	 */
	@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
	protected void migrateGroup(String principalId) {
		try {
			// Add/remove the minimal number of elements from each group
			List<UserGroup> existing = groupMembersDAO
					.getMembers(principalId);
			List<UserGroup> newbies = crowdGroupMembersDAO
					.getMembers(principalId);

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
		} catch (NotFoundException e) {
			log.warn("A group was deleted before the worker could finish processing it", e);
		}
	}
}
