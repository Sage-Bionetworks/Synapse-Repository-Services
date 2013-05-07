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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;


/**
 * This class collects Synapse usage statistics, specifically for the following metric:
 * "Number of Active Projects (New content posted to project in last month by 2 different users; 
 * if users span multiple organizations count double; if project linked to publication count double)"
 * 
 * @author brucehoff
 *
 */
public class ProjectActivityStats {
	
	private static Map<String, String> idToUser;
	
	private static final int TIME_WINDOW_DAYS = 30;
	
	private static final boolean VERBOSE = false;
	
	public static void main(String[] args) throws Exception {
		DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");		
		Synapse synapse = new Synapse();
		final String username = args[0];
		final String password = args[1];
		final Date end = df.parse(args[2]); // i.e. "27-JUL-2012"
		final String jdoUserGroupTableCsvPath = args[3];
		initIdToEmailMap(jdoUserGroupTableCsvPath);
		
		Long start = (end.getTime())-TIME_WINDOW_DAYS*24*3600*1000L;		
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
			Queue<String> toProcess = new LinkedList<String>();
			toProcess.add(projectId);		
			analyzeEntity(projectKey, ans, start, end, true, synapse, toProcess);
			// if we already have the max score, then don't bother searching for more new content in the project
			if (maxContributorScore(ans.get(projectKey))) continue;
		} 
		return ans;
	}
	
	public static final String[] contentTypes = {"entity"};
		
	/**
	 * 
	 * Look in the given bucket (e.g. a project or study) for objects of the given type (e.g. data or code)
	 * and record results.
	 * @param bucketId
	 * @param objectType
	 * @param results
	 * @param entitiesToProcess 
	 * @param contributors 
	 * @throws InterruptedException 
	 */
	public static void analyzeEntity(String projectKey, 
			Map<String, Collection<String>> results, 
			Long start, 
			Long end, 
			boolean stopIfMaxScore,
			Synapse synapse, 
			Queue<String> entitiesToProcess) throws SynapseException, JSONException {
		
		if(entitiesToProcess.size() == 0) return;
				
		// next in BFS from head of queue
		String entityId = entitiesToProcess.poll();
						
		// Count for Entity
		addAllToMap(results, projectKey, lookupEntityContributors(entityId, start, end, synapse));
		if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;
		
		// Count for Wiki
		addAllToMap(results, projectKey, lookupWikiContributors(entityId, start, end, synapse));
		if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;
		
		// Count for Provenance
		addAllToMap(results, projectKey, lookupProvenanceContributors(entityId, start, end, synapse));
		if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;				

		// get bundle
		int partsMask = EntityBundle.ENTITY | EntityBundle.HAS_CHILDREN;
		EntityBundle bundle = synapse.getEntityBundle(entityId, partsMask);		

		// Count for Older versions
		addAllToMap(results, projectKey, lookupOldVersionContributors(start, end, synapse,	bundle.getEntity()));
		if (stopIfMaxScore && maxContributorScore(results.get(projectKey))) return;				
			
		
		// Add children to DFS queue if exist					
		addChildren(synapse, entitiesToProcess, entityId, bundle.getHasChildren());							
		// continue on DFS if needed
		if(entitiesToProcess.size() > 0) analyzeEntity(projectKey, results, start, end, stopIfMaxScore, synapse, entitiesToProcess);
	}

	private static List<String> lookupOldVersionContributors(Long start,
			Long end, Synapse synapse, Entity entity) throws SynapseException {
		List<String> contributors = new ArrayList<String>();

		if(entity instanceof Versionable) {			 				
			PaginatedResults<VersionInfo> versions = synapse.getEntityVersions(entity.getId(), 1, Integer.MAX_VALUE);
			for(VersionInfo info : versions.getResults()) {								
				// if version is in start/end range, add to DFS queue
				long modified = info.getModifiedOn().getTime();
				if ((start==null || start<=modified) && (end==null || modified<=end)) {
					String email = idToUser.get(info.getModifiedByPrincipalId());
					if (email != null && !UsageMetricsUtils.isOmittedName(email)) { 
						contributors.add(email);
					}
				}
			}
		}
		return contributors;
	}

	private static void addChildren(Synapse synapse,
			Queue<String> entitiesToProcess, String entityId,
			boolean hasChildren) throws JSONException {
		if(hasChildren) {					
			int offset = 1;
			int batchSize = 1000;
			int total = 0;
			do {
				// Count Children
				String query = "select id from entity where parentId==\""+entityId+"\"";
				query += " LIMIT "+batchSize+" OFFSET "+offset;
				JSONObject queryResult = reliablyQuerySynapse(synapse, query);
				total = (int)queryResult.getLong("totalNumberOfResults");
				JSONArray resultsArray = queryResult.getJSONArray("results");
				for (int j=0; j<resultsArray.length(); j++) {
					JSONObject child = resultsArray.getJSONObject(j);
					entitiesToProcess.add(child.getString("entity.id"));
				}
				offset += batchSize;
			} while (offset<=total && offset<=MAX_CONTENT_PER_BUCKET);
		}
	}

	/**
	 * This method is required until principalId is added to the Entity object (not just name for createdOn/modifiedOn)
	 * @param entityId
	 * @param start
	 * @param end
	 * @param versionId 
	 * @param synapse
	 * @return
	 * @throws SynapseException
	 */
	private static List<String> lookupEntityContributors(String entityId, Long start, Long end, Synapse synapse) throws SynapseException {
		List<String> contributors = new ArrayList<String>();		

		// Count Children
		String query = "select id,createdOn,modifiedOn,createdByPrincipalId,modifiedByPrincipalId from entity where id==\""+entityId+"\"";
		query += " LIMIT "+1+" OFFSET "+1;
		JSONObject queryResult = reliablyQuerySynapse(synapse, query);
		JSONArray resultsArray;
		try {
			resultsArray = queryResult.getJSONArray("results");
			JSONObject entityHit = resultsArray.getJSONObject(0);		
			addUserToListWithinDateRange(start, end, contributors, entityHit.getLong("entity.createdOn"), entityHit.getString("entity.createdByPrincipalId"));
			addUserToListWithinDateRange(start, end, contributors, entityHit.getLong("entity.modifiedOn"), entityHit.getString("entity.modifiedByPrincipalId"));
		} catch (JSONException e) {
			throw new SynapseException(e);
		}

		return contributors;
	}
	
	private static Collection<String> lookupWikiContributors(String entityId, Long start, Long end, Synapse synapse) throws SynapseException {
		List<String> contributors = new ArrayList<String>();
		try {			
			PaginatedResults<WikiHeader> headerTree = synapse.getWikiHeaderTree(entityId, ObjectType.ENTITY);
			if(headerTree.getTotalNumberOfResults() > 0) {
				for(WikiHeader header : headerTree.getResults()) {
					WikiPageKey key = new WikiPageKey(entityId, ObjectType.ENTITY, header.getId());
					WikiPage page = synapse.getWikiPage(key);
					addUserToListWithinDateRange(start, end, contributors, page.getCreatedOn().getTime(), page.getCreatedBy());
					addUserToListWithinDateRange(start, end, contributors, page.getModifiedOn().getTime(), page.getModifiedBy());								
				}
			}
		} catch (JSONObjectAdapterException e) {
			throw new SynapseException(e);
		} catch (SynapseException e) {
			if(!(e instanceof SynapseNotFoundException)) throw e;
		}
		return contributors;
	}

	private static Collection<String> lookupProvenanceContributors(String entityId, Long start, Long end, Synapse synapse) throws SynapseException {
		List<String> contributors = new ArrayList<String>();
		
		try {
			Activity activity = synapse.getActivityForEntity(entityId);
			addUserToListWithinDateRange(start, end, contributors, activity.getCreatedOn().getTime(), activity.getCreatedBy());
			addUserToListWithinDateRange(start, end, contributors, activity.getModifiedOn().getTime(), activity.getModifiedBy());			
		} catch (SynapseException e) {
			if(!(e instanceof SynapseNotFoundException)) throw e;
		}
		
		return contributors;
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

	public static void initIdToEmailMap(String jdoUserGroupTableCsvPath) {
		// Load the csv file and process it into the map.
		File file = new File(jdoUserGroupTableCsvPath);
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

	private static void addUserToListWithinDateRange(Long start, Long end,
			List<String> contributors, long created, String principalId) {
		if ((start==null || start<=created) && (end==null || created<=end)) {
			String email = idToUser.get(principalId);
			if (email != null && !UsageMetricsUtils.isOmittedName(email) && !contributors.contains(email)) { 
				contributors.add(email);
			}
		}
	} 
	
}
