package org.sagebionetworks.crowd.workers;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


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
	private UserDAO userDAO;
	
	
	@Override
	public void run() {
		List<String> crowdGroups = getCrowdGroups("group");
		List<String> crowdUsers = getCrowdGroups("user");
		List<String> principalIds = new ArrayList<String>();
		
		// Make sure all the Crowd groups exist in RDS
		for (int i = 0; i < crowdGroups.size() + crowdUsers.size(); i++) {
			boolean isIndividual = i >= crowdGroups.size();
			String name = isIndividual ? crowdUsers.get(i - crowdGroups.size()) : crowdGroups.get(i);
			String principalId;
			
			if (userGroupDAO.doesPrincipalExist(name)) {
				principalId = userGroupDAO.findGroup(name, isIndividual).getId();
			} else {
				UserGroup ug = new UserGroup();
				ug.setIsIndividual(isIndividual);
				ug.setName(name);
				principalId = userGroupDAO.create(ug);
				
				if (isIndividual) {
					// Each migrated individual needs to have a profile moved over
					ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
					try {
						userProfileDAO.get(principalId, schema);
						// The profile already exists
						
					} catch (NotFoundException e) {
						// Must make a new profile
						try {
							User user = userDAO.getUser(URLEncoder.encode(name, "UTF-8"));
							UserProfile userProfile = new UserProfile();
							userProfile.setOwnerId(principalId);
							userProfile.setFirstName(user.getFname());
							userProfile.setLastName(user.getLname());
							userProfile.setDisplayName(user.getDisplayName());
							userProfileDAO.create(userProfile, schema);
						} catch (NotFoundException nfe) {
							throw new RuntimeException(nfe);
						} catch (InvalidModelException ime) {
							throw new RuntimeException(ime);
						} catch (UnsupportedEncodingException uee) {
							throw new RuntimeException(uee);
						}
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
				// A group was deleted before the worker could finish processing it
			}
		}
	}
	
	/**
	 * Returns a list of names of groups fetched from Crowd
	 * @param entityType Either "user" or "group"
	 * @return 
	 */
	private List<String> getCrowdGroups(String entityType) {
		try {
			byte[] sessionXml = CrowdAuthUtil.executeRequest(
					CrowdAuthUtil.urlPrefix()+"/search?entity-type="+entityType, 
					"GET", "", HttpStatus.OK, "Could not perform query");
			
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList results = (NodeList)xpath.evaluate("/"+entityType+"s/"+entityType+"/@name", 
					new InputSource(new ByteArrayInputStream(sessionXml)), 
					XPathConstants.NODESET);
			
			List<String> groups = new ArrayList<String>();
			for (int i = 0; i < results.getLength(); i++) {
				String groupName = results.item(i).getNodeValue();
				groups.add(groupName);
			}
			return groups;
		} catch (AuthenticationException e) {
		} catch (XPathExpressionException e) { }
		return new ArrayList<String>();
	}
}
