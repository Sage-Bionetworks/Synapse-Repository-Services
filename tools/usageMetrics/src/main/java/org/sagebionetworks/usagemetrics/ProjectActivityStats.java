package org.sagebionetworks.usagemetrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.UserProfile;


/**
 * This class collects Synapse usage statistics, specifically for the following metric:
 * "Number of Active Projects (New content posted to project in last month by 2 different users; 
 * if users span multiple organizations count double; if project linked to publication count double)"
 * 
 * @author brucehoff
 *
 */
public class ProjectActivityStats {
	
	private static final String ID_TO_USERNAME_FILE = "/home/geoff/work/sage/notes/principalIdToUserNameMap.csv";
	private static Map<String, String> idToUser;
	
	private static final int TIME_WINDOW_DAYS = 30;
	
	private static final boolean VERBOSE = false;
	
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
	
	public static void main(String[] args) throws Exception {
		initIdToEmailMap();
		
		DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
		Date end = df.parse("27-JUL-2012");
		Long start = (end.getTime())-TIME_WINDOW_DAYS*24*3600*1000L;
		Synapse synapse = new Synapse();
		String username = args[0];
		String password = args[1];
		synapse.login(username, password);
		Map<String, Collection<String>> results = findProjectUsers(synapse,  start,  end.getTime());
		
		System.out.println("\nStart date: "+(start==null?"NONE":df.format(start))+" End date: "+(end==null?"NONE":df.format(end)));
		System.out.println("Note:  Not all contributors are listed below.  When we reach the maximum 'score' for a project we stop scanning it for contributors.");
		int totalScore = 0;
		for (String id : results.keySet()) {
			System.out.print("Project ID: "+id+" has contributor(s):");
			for (String name : results.get(id)) System.out.print(" "+name);
			int score = scoreContributors(results.get(id));
			System.out.println(" (score="+score+")");
			totalScore += score;
		}
		System.out.println("\nTOTAL SCORE: "+totalScore);
	}
	
	public static int scoreContributors(Collection<String> contributors) {
		if (maxContributorScore(contributors)) return 2; // at least two contributors from two different organizations
		if (contributors.size()>1) return 1; // at least two different contributors (but not from different organizations)
		return 0; // no more than one contributor
	}
	
	// these limits allow us to find multiple contributors without exhaustively
	// reviewing all the Data objects in large projects, like the Synapse Commons Repository
	private static final int MAX_STUDIES_PER_PROJECT = 200;
	private static final int MAX_CONTENT_PER_BUCKET = 200;
	
	/*
	 * For each project, find the content belonging to the project and 
	 * created within the given time bounds, recording the users who
	 * created or updated the content.
	 * 
	 * should omit 'users' that are Synapse developers, administrators or service accounts
	 * 
	 * @param synapse a synapse client, connecting as an administrator
	 * @param start the beginning of the time window (or null if no beginning)
	 * @param end the end of the time window (or null if no end)
	 * 
	 * @return a Map whose keys are project ids and who values are Lists
	 * of users who added or modified content
	 */
	public static Map<String, Collection<String>> findProjectUsers(Synapse synapse, Long start, Long end) throws SynapseException, JSONException {
		Map<String, Collection<String>> ans = new HashMap<String, Collection<String>>();
		// get all projects
		JSONObject projectQueryResults = synapse.query("select id, name from project");
		JSONArray projects = projectQueryResults.getJSONArray("results");
		// for each project ...
		for (int p=0; p<projects.length(); p++) {
			String projectId = ((JSONObject)projects.get(p)).getString("project.id");
			String projectName = ((JSONObject)projects.get(p)).getString("project.name");
			String projectKey = projectId+" "+projectName;
			System.out.println("Project: "+projectKey+" ("+(p+1)+" of "+projects.length()+")");
			
			analyzeAllDataTypes(projectKey, projectId, ans, start, end, true, synapse);
			// if we already have the max score, then don't bother searching for more new content in the project
			if (maxContributorScore(ans.get(projectKey))) continue;
			// find all the studies
			int studyOffset=1;
			int studyBatchSize = 20;
			int studyTotal=0;
			boolean projectMeetsMaxContributorScore = false;
			do {
				// get a batch of studies
				JSONObject studyIds = synapse.query("select id, name from study where parentId==\""+projectId+"\" LIMIT "+studyBatchSize+" OFFSET "+studyOffset);
				studyTotal = (int)studyIds.getLong("totalNumberOfResults");
				JSONArray a = studyIds.getJSONArray("results");
				for (int i=0; i<a.length() && !projectMeetsMaxContributorScore; i++) {
					JSONObject study = a.getJSONObject(i);
					String studyId = study.getString("study.id");
					if (VERBOSE) System.out.println("\tStudy: "+studyId);
					analyzeAllDataTypes(projectKey, studyId, ans, start, end, true, synapse);
					projectMeetsMaxContributorScore = maxContributorScore(ans.get(projectKey));
				}
				studyOffset += studyBatchSize;
			} while (studyOffset<=studyTotal && studyOffset<=MAX_STUDIES_PER_PROJECT && !projectMeetsMaxContributorScore);
			if (projectMeetsMaxContributorScore) continue;
		} // project iterator
		return ans;
	}
	
	public static final String[] contentTypes = {"data", "expressiondata", "genotypedata", "phenotypedata", "code"};
	
	public static void analyzeAllDataTypes(String projectKey,
			String bucketId, 
			Map<String, Collection<String>> results, 
			Long start, 
			Long end, 
			boolean stopIfMaxScore,
			Synapse synapse) throws SynapseException, JSONException {
		for (String objectType : contentTypes) {
			analyzeBucket(projectKey, bucketId, objectType, results, start, end, stopIfMaxScore, synapse);
			if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;
		}
	}
	
	/**
	 * 
	 * Look in the given bucket (e.g. a project or study) for objects of the given type (e.g. data or code)
	 * and record results.
	 * @param bucketId
	 * @param objectType
	 * @param results
	 * @throws InterruptedException 
	 */
	public static void analyzeBucket(String projectKey,
			String bucketId, 
			String objectType, 
			Map<String, Collection<String>> results, 
			Long start, 
			Long end, 
			boolean stopIfMaxScore,
			Synapse synapse) throws SynapseException, JSONException {
		// find all the data and code in the study
		int offset = 1;
		int batchSize = 20;
		int total = 0;
		do {
			String query = "select id, createdOn, modifiedOn, createdByPrincipalId, modifiedByPrincipalId from "+
					objectType+" where parentId==\""+bucketId+"\"";
			// if start is specified, then limit results to those whose modified date is >= start
			if (start!=null) query += " and modifiedOn >= "+start;
			// if end is specified then limit results to those whose creation date is <= end
			if (end!=null) query += "and createdOn <= "+end;
			query += " LIMIT "+batchSize+" OFFSET "+offset;
			JSONObject dataIds;
			
			dataIds = reliablyQuerySynapse(synapse, query);
			total = (int)dataIds.getLong("totalNumberOfResults");
			JSONArray d = dataIds.getJSONArray("results");
			for (int j=0; j<d.length(); j++) {
				if (j==0  && VERBOSE) System.out.println("\t\t"+objectType+" "+offset+" of "+total);
				JSONObject data = d.getJSONObject(j);
				addAllToMap(results, projectKey, getContributors(data, objectType, start, end));
				if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;
			}
			offset += batchSize;
		} while (offset<=total && offset<=MAX_CONTENT_PER_BUCKET);
		
	}

	private static JSONObject reliablyQuerySynapse(Synapse synapse, String query) {
		JSONObject dataIds = null;
		boolean succeeded = false;
		do {
			try {
				dataIds = synapse.query(query);
				succeeded = true;
			} catch (Exception e) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		} while (!succeeded);
		return dataIds;
	}
	
	/**
	 * 
	 *
	 * @param names list of contributors to a project
	 * @return true iff the list represents a maximum score, i.e. at least two contributors from different organizations
	 */
	public static boolean maxContributorScore(Collection<String> names) {
		if (names==null) return false;
		Set<String> organizations = new HashSet<String>();
		for (String name : names) {
			String organization = getOrganization(name);
			if (organization!=null) {
				organizations.add(organization);
				if (organizations.size()>=2) return true;
			}
		}
		return false;
	}
	
	public static String getOrganization(String name) {
		int at = name.indexOf("@");
		if (at<0) return null;
		return name.substring(at+1);
	}
	
	/*
	 * Add all the values in 'values' to the given map under the key 'key'
	 */
	public static void addAllToMap(Map<String, Collection<String>> map, String key, Collection<String> values) {
		if (values==null || values.isEmpty()) return;
		Collection<String> c = map.get(key);
		if (c==null) {
			c = new HashSet<String>();
			map.put(key, c);
		}
		c.addAll(values);
	}
	
	// if the creation date is within the time range, record the creator
	// if the modification date is within the time range, record the modifier
	public static List<String> getContributors(JSONObject o, String type, Long start, Long end) {
		List<String> ans = new ArrayList<String>();
 		try {
			long created = o.getLong(type+".createdOn");
			long modified = o.getLong(type+".modifiedOn");
			if ((start==null || start<=created) && (end==null || created<=end)) {
				String id = o.getString(type+".createdByPrincipalId");
				String email = idToUser.get(id);
				if (email != null && !UsageMetricsUtils.isOmittedName(email)) ans.add(email);
			}
			if ((start==null || start<=modified) && (end==null || modified<=end)) {
				String id = o.getString(type+".modifiedByPrincipalId");
				String email = idToUser.get(id);
				if (email != null && !UsageMetricsUtils.isOmittedName(email)) ans.add(email);
			}
		} catch (JSONException e) {
			throw new RuntimeException(o.toString(), e);
		}
		return ans;
	}

}
