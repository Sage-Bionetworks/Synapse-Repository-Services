package org.sagebionetworks.usagemetrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserProfile;

public class DataAccessAudit {

	public static void main(String[] args) throws Exception {
		initIdToEmailMap();
		SynapseClientImpl synapse = new SynapseClientImpl();
		String username = args[0];
		String password = args[1];
		synapse.login(username, password);
		File idFile = new File(args[2]);
		
		List<String> projectIds = Arrays.asList(new String[] {"417885"});
		
		getAclList(synapse, projectIds);
		
	}

	private static final String ID_TO_USERNAME_FILE = "/home/geoff/Downloads/principalIdToUserNameMap.csv";
	private static Map<String, String> idToUser;

	public static void initIdToEmailMap() {
		// Load the csv file and process it into the map.
		File file = new File(ID_TO_USERNAME_FILE);
		FileInputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			idToUser = new HashMap<String, String>(600);
			String s = br.readLine();
			while (s != null) {
				String[] values = s.split(",");
				try {
					if (Integer.parseInt(values[2]) == 1) {
						idToUser.put(values[0], values[3]);
					}
				} catch (NumberFormatException e) {
				}
				s = br.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static void getAclList(SynapseClientImpl synapse, List<String> projectIds)
			throws SynapseException, IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("/home/geoff/Documents/output.txt")));
		
		for (String entityId : projectIds) {

			AccessControlList acl;
			try {
				acl = synapse.getACL(entityId);
			} catch (SynapseNotFoundException e) {
				try {
				EntityHeader benefactor = synapse.getEntityBenefactor(entityId);
				acl = synapse.getACL(benefactor.getId());
				} catch (SynapseNotFoundException e2) {
					System.out.format("Skipping: %s%n", entityId);
					continue;
				}
			}
			Set<ResourceAccess> resourceAccess = acl.getResourceAccess();
			System.out.format("Entity: %s%n", entityId);
			System.out.format("Entity: %s%n", entityId);
			
			for (ResourceAccess access : resourceAccess) {
				String profileName;
				String email;
				try {
					UserProfile userProfile = synapse.getUserProfile(access
							.getPrincipalId().toString());
					profileName = userProfile.getUserName();
					email = idToUser.get(userProfile.getOwnerId());
				} catch (SynapseNotFoundException e) {
					profileName = access.getPrincipalId().toString();
					email = "";
				}
				System.out.format("\t%s - %s - %s - %s%n", access.getPrincipalId(),
						profileName, email, access.getAccessType());
			}
		}
	}

	private static List<String> getIdsFromFile(File idListFile) throws Exception {
		ArrayList<String> entityList = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(idListFile));
		
		String line;
		while ((line = reader.readLine()) != null) {
			entityList.add(line);
		}
		return entityList;
	}
}
