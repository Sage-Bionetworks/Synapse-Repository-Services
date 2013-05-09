package org.sagebionetworks.usagemetrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityPath;

public class DataAuditStats {
	private static class DataObject {

		public DataObject(String entityId, String name) {
			this.entityId = entityId;
			this.name = name;
		}

		public String entityId;
		public String name;
		public String uri;

	}

	private static final String ID_TO_USERNAME_FILE = "/home/geoff/Downloads/principalIdToUserNameMap.csv";
	private static Map<String, String> idToUser;


	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Synapse synapse = new Synapse();
		String username = args[0];
		String password = args[1];
		synapse.login(username, password);

		initIdToEmailMap();

		ArrayList<String> externalUserIds = getExternalUserIds();

		Map<String, ArrayList<DataObject>> entitiesByUser = getEntities(synapse, externalUserIds);

		Map<String, Map<String, ArrayList<DataObject>>> rootProjects = getRootProjects(synapse, entitiesByUser);
		
		System.out.format("There a total of %d non-Sage employees who have uploaded data.%n%n", entitiesByUser.size());
		
		printProjectsByUser(rootProjects);
		System.out.println();
		System.out.println("------------------------------------------------------------------");
		System.out.println();
		printUserProjectsPlusData(rootProjects);
	}

	private static void printProjectsByUser(
			Map<String, Map<String, ArrayList<DataObject>>> rootProjects) {
		for (String user : rootProjects.keySet()) {
			System.out.format("Projects owned by %s:%n", idToUser.get(user));
			for (String projectId : rootProjects.get(user).keySet()) {
				System.out.format("\t%s%n", projectId);
			}
			System.out.println();
		}
	}

	private static Map<String, Map<String, ArrayList<DataObject>>> getRootProjects(Synapse synapse,
			Map<String, ArrayList<DataObject>> entitiesByUser) {
		
		Map<String, Map<String, ArrayList<DataObject>>> userToProjects = new HashMap<String,Map<String, ArrayList<DataObject>>>();
		
		for (String user : entitiesByUser.keySet()) {
			for (DataObject obj : entitiesByUser.get(user)) {
				EntityPath entityPath;
				try {
					entityPath = synapse.getEntityPath(obj.entityId);
				} catch (SynapseException e) {
					System.out.format("Could not find entity %s%n", obj.entityId);
					e.printStackTrace();
					continue;
				}
				Map<String, ArrayList<DataObject>> map = userToProjects.get(user);
				if (map == null) {
					map = new HashMap<String, ArrayList<DataObject>>();
					userToProjects.put(user, map);
				}
				
				String rootProjectId = entityPath.getPath().get(1).getId();
				
				ArrayList<DataObject> arrayList = map.get(rootProjectId);
				if (arrayList == null) {
					arrayList = new ArrayList<DataObject>();
					map.put(rootProjectId, arrayList);
				}
				arrayList.add(obj);
			}
		}
		
		return userToProjects;
	}

	private static <obj> void printUserProjectsPlusData(
			Map<String, Map<String, ArrayList<DataObject>>> rootProjects) {
		for (String user : rootProjects.keySet()) {
			System.out.format("%s projects%n", idToUser.get(user));
			for (Entry<String, ArrayList<DataObject>> entries : rootProjects
					.get(user).entrySet()) {
				if (entries.getValue().size() != 0) {
					System.out.format("\tProject Id - %s:%n", entries.getKey());
					for (DataObject obj : entries.getValue()) {
						System.out.format("\t\t%s - %s%n", obj.entityId, obj.name);
					}
				}
			}
		}
	}

	public static ArrayList<String> getExternalUserIds() {
		ArrayList<String> userIds = new ArrayList<String>();

		for (Entry<String, String> entry : idToUser.entrySet()) {
			if (! (isSageEmployee(entry.getValue()) ||
				   isSpecialCaseSageEmployee(entry.getValue()))) {
				userIds.add(entry.getKey());
			}
		}

		return userIds;

	}

	// Rewrite this method to be much smarter.  Several things: approach from the other direction.
	// filter the list of users for non-sage employees, then run per-user queries limiting data
	// where createdBy = userId.  Second.  This step is potentially parallelizable, make that object a runnable
	// and then dispatch three or four threads with lists of users to process.
	public static Map<String, ArrayList<DataObject>> getEntities(Synapse synapse, ArrayList<String> externalUserIds) throws SynapseException, JSONException, InterruptedException {
		String baseQuery = "select id, name " +
							"from data where createdByPrincipalId == \"%s\" limit %d offset %d";

		Map<String, ArrayList<DataObject>> userDataMap = new HashMap<String, ArrayList<DataObject>>(externalUserIds.size());

		for (String userId : externalUserIds) {
			int total;
			int offset = 1;
			int batchSize = 40;

			ArrayList<DataObject> dataList = new ArrayList<DataObject>();

			do {
				JSONObject dataBatch = synapse.query(String.format(baseQuery, userId, batchSize, offset));
				total = (int)dataBatch.getLong("totalNumberOfResults");
				JSONArray d = dataBatch.getJSONArray("results");
				for (int j=0; j<d.length(); j++) {
					JSONObject data = d.getJSONObject(j);

					DataObject obj = new DataObject(data.getString("data.id"),
							data.getString("data.name"));

					dataList.add(obj);
				}

				offset += batchSize;
				if (offset == 401)
					Thread.sleep(500);
			} while (offset < total);
			
			userDataMap.put(userId, dataList);
		}

		return userDataMap;
	}

	private static boolean isSpecialCaseSageEmployee(String email) {
		List<String> names = Arrays.asList(new String[] {"isjang", "nicole.deflaux", "matthew.furia",
															"david.gerrard", "creighto", "th8623",
															"ninpy.weiyi", "cwarden", "jpeng", "sean.cory",
															"yifeic", "zzsw311", "kimhaseong", "vlagani", 
															"r.s.savage", "yingwooi", "frank.dondelinger",
															"aviad"});

		if (names.contains(email.split("@")[0])) {
			return true;
		}

		return false;
	}

	private static boolean isSageEmployee(String email) {
		if (email == null)
			return false;

		String[] split = email.split("@");

		if (split.length != 2 || !split[1].equalsIgnoreCase("sagebase.org")) {
			return false;
		}
		return true;
	}

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

}
