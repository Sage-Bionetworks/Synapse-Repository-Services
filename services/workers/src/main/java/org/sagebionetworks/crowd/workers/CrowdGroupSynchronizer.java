package org.sagebionetworks.crowd.workers;

import java.io.ByteArrayInputStream;
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
import org.sagebionetworks.repo.model.GroupMembers;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
			}
			principalIds.add(principalId);
		}

		// Copy the un-nested group membership from Crowd to the GroupMembersDAO
		// Since there is no exposed method for modifying groups on RDS, 
		//   we can delete/create entries with impunity
		for (int i = 0; i < principalIds.size(); i++) {
			boolean isIndividual = i >= crowdGroups.size();
			if (!isIndividual) {
				// Add/remove the minimal number of elements from each group
				String principalId = principalIds.get(i);
				GroupMembers existing = groupMembersDAO.getMembers(principalId);
				GroupMembers newbies = crowdGroupMembersDAO.getMembers(principalId);
				
				Set<UserGroup> toDelete = new HashSet<UserGroup>(existing.getMembers());
				toDelete.removeAll(newbies.getMembers());
				
				Set<UserGroup> toAdd = new HashSet<UserGroup>(newbies.getMembers());
				toAdd.removeAll(existing.getMembers());
				
				GroupMembers operator = new GroupMembers();
				operator.setId(principalId);
				operator.setMembers(new ArrayList<UserGroup>(toDelete));
				groupMembersDAO.removeMembers(operator);
				
				operator.setMembers(new ArrayList<UserGroup>(toAdd));
				groupMembersDAO.addMembers(operator);
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
