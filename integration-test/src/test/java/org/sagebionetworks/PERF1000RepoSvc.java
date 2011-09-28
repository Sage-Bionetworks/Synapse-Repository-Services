package org.sagebionetworks;

import java.io.BufferedWriter;

import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.httpclient.HttpException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseServiceException;
import org.sagebionetworks.client.SynapseUserException;

/**
 * Run this performance test to ensure there's no performance regression
 * 
 * @author deflaux
 */
public class PERF1000RepoSvc {
	
	private static Synapse synapse = null;
    
        private static enum EntityType {PROJECT, DATASET, LAYER, LOCATION};
    //    private static EnumMap<EntityType, JSONObject> entityTypeMetadata;
     
        private static int maxNumProjects = 64;
        private static int maxNumDatasets = 64;
        private static File tempResultFile;
        // Need to get from stackConfiguration
        private static String resultFilename;
        
        
        private static void cleanup(int rootProjectId) throws Exception {
            if (rootProjectId > 0) {
                synapse.deleteEntity("/project/" + rootProjectId);
            } else {
                int np = 1;
                while (np > 0) {
                    JSONObject rs = synapse.getEntity("/project?limit=10");
                    np = rs.getInt("totalNumberOfResults");
                    JSONArray projects = rs.getJSONArray("results");
                    for (int p = 0; p < projects.length(); p++) {
                        synapse.deleteEntity(projects.getJSONObject(p).getString("uri"));
                    }
                }
            }
        }
        
        private static JSONObject addAnnotations(JSONObject entity, int numAnnotations) throws Exception {
            JSONObject annotationsSpec = new JSONObject();
            JSONObject stringAnnotationsSpec = new JSONObject();
            JSONObject longAnnotationsSpec = new JSONObject();
            JSONObject doubleAnnotationsSpec = new JSONObject();

            for (int i = 0; i < numAnnotations; i++) {
                stringAnnotationsSpec.put("stringAnnotation_" + i, new JSONArray().put("stringAnnotation value"));
                longAnnotationsSpec.put("longAnnotation_" + i, i);
                doubleAnnotationsSpec.put("doubleAnnotation_" + i, Math.PI * i);
            }            
            
            annotationsSpec.put("stringAnnotations", stringAnnotationsSpec);
//            annotationsSpec.put("longAnnotations", longAnnotationsSpec);
//            annotationsSpec.put("doubleAnnotations", doubleAnnotationsSpec);
            
            JSONObject updatedEntity = synapse.updateEntity(entity.getString("annotations"), annotationsSpec);
            return updatedEntity;
        }
        
        private static int[] populate(EntityType entityType, int numEntities, int startId, int parentId, int numAnnotations) throws Exception {
            JSONObject entitySpec = new JSONObject();
            JSONObject entity;
            String uri = null;
            int createdEntityIds[] = new int[numEntities];
            
            for (int i = 0; i < numEntities; i++) {
                int namePostfix = i + startId;
                switch (entityType) {
                    case PROJECT:
                        entitySpec.put("name", "PerfTest_Project_" + namePostfix);
                        entitySpec.put("description", "Project created for performance test.");
                        // Change once I figure out how to map enums to array of strings
                        uri = "/project";
                        break;
                    case DATASET:
                        entitySpec.put("name", "PerfTest_Dataset_" + namePostfix);
                        entitySpec.put("description", "Dataset created for performance test.");
                        uri = "/dataset";
                        break;
                    case LAYER:
                        entitySpec.put("name", "PerfTest_Layer_" + namePostfix);
                        entitySpec.put("description", "Layer created for performance test.");
                        uri = "/layer";
                        break;
                    case LOCATION:
                        entitySpec.put("name", "PerfTest_Location_" + namePostfix);
                        entitySpec.put("description", "Location created for performance test.");
                        uri = "/location";
                        break;
                    default:
                        break;
                }
                if (parentId > 0) {
                    entitySpec.put("parentId", parentId);
                }
                entity = synapse.createEntity(uri, entitySpec);
                createdEntityIds[i] = entity.getInt("id");
                addAnnotations(entity, numAnnotations);
            }
            return createdEntityIds;
        }
        
        private static int[] populate(EntityType entityType, int numEntities, int startId, int numAnnotations) throws Exception {
            return populate(entityType, numEntities, startId, -1, numAnnotations);
        }
        
        private static int[] populate(EntityType entityType, int numEntities, int startId, int[] parentIds, int numAnnotations) throws Exception {
            int[] createdEntityIds = new int[numEntities];
            
            for (int i = 0; i < numEntities; i++) {
                int parentId = parentIds[i];
                createdEntityIds[i] = populate(entityType, 1, startId, parentId, numAnnotations)[0];
            }
            return createdEntityIds;
        }

        private static JSONObject doCRUD(EntityType entityType, int parentId) throws Exception {
            JSONObject entitySpec = new JSONObject();
            JSONObject entity;
            String uri = null;
            long startTime, elapsedTimeCreate, elapsedTimeRead, elapsedTimeUpdate, elapsedTimeDelete;
            
            switch (entityType) {
                case PROJECT:
                    entitySpec.put("name", "PerfTest_Project");
                    entitySpec.put("description", "Project created for performance test.");
                    // Change once I figure out how to map enums to array of strings
                    uri = "/project";
                    break;
                case DATASET:
                    entitySpec.put("name", "PerfTest_Dataset");
                    entitySpec.put("description", "Dataset created for performance test.");
                    uri = "/dataset";
                    break;
                case LAYER:
                    entitySpec.put("name", "PerfTest_Layer");
                    entitySpec.put("description", "Layer created for performance test.");
                    uri = "/layer";
                    break;
                case LOCATION:
                    entitySpec.put("name", "PerfTest_Location");
                    entitySpec.put("description", "Location created for performance test.");
                    uri = "/location";
                    break;
                default:
                    break;
            }
            if (parentId > 0) {
                entitySpec.put("parentId", parentId);
            }
            // Actual test
            elapsedTimeCreate = elapsedTimeRead = elapsedTimeUpdate = elapsedTimeDelete = 0;
            for (int n =1; n < 10; n++) {
                // Create new entitity
                startTime = System.nanoTime();
                entity = synapse.createEntity(uri, entitySpec);
                assertTrue(null != entity);
                elapsedTimeCreate += System.nanoTime() - startTime;
                startTime = System.nanoTime();
                // Read entity
                entity = synapse.getEntity(entity.getString("uri"));
                elapsedTimeRead += System.nanoTime() - startTime;
                // Update entity
                entitySpec.put("description", "Updated description");
                entity = synapse.updateEntity(entity.getString("uri"), entitySpec);
                elapsedTimeUpdate += System.nanoTime() - startTime;
                // Query entity
                
                // Delete entity
                startTime = System.nanoTime();
                synapse.deleteEntity(entity.getString("uri"));
                elapsedTimeDelete += System.nanoTime() - startTime;
            }
            elapsedTimeCreate = elapsedTimeCreate / 10;
            elapsedTimeRead = elapsedTimeRead / 10;
            elapsedTimeUpdate = elapsedTimeUpdate / 10;
            elapsedTimeDelete = elapsedTimeDelete / 10;

            JSONObject result = new JSONObject();
            result.put("elapsedTimeCreate", elapsedTimeCreate);
            result.put("elapsedTimeRead", elapsedTimeRead);
            result.put("elapsedTimeUpdate", elapsedTimeUpdate);
            result.put("elapsedTimeDelete", elapsedTimeDelete);
            return result;
        }
        
        private static void logResults(String testName, JSONObject results) throws Exception {
            BufferedWriter out = new BufferedWriter(new FileWriter(tempResultFile, true));
            
            JSONArray res;
            res = results.getJSONArray("INSERT");
            out.write(testName + "\tI");
            for (int i = 0; i < res.length(); i++) {                
                out.write("\t" + res.getLong(i)/1000000);
            }
            out.write("\n");
            res = results.getJSONArray("READ");
            out.write(testName + "\tR");
            for (int i = 0; i < res.length(); i++) {                
                out.write("\t" + res.getLong(i)/1000000);
            }
            out.write("\n");
            res = results.getJSONArray("UPDATE");
            out.write(testName + "\tU");
            for (int i = 0; i < res.length(); i++) {                
                out.write("\t" + res.getLong(i)/1000000);
            }
            out.write("\n");
            res = results.getJSONArray("DELETE");
            out.write(testName + "\tD");
            for (int i = 0; i < res.length(); i++) {                
                out.write("\t" + res.getLong(i)/1000000);
            }
            out.write("\n");
//            res = results.getJSONArray("QUERY");
//            out.write(testName + "\tQ");
//            for (int i = 0; i < res.length(); i++) {                
//                out.write("\t" + res.getLong(i)/1000000);
//            }
//            out.write("\n");
            out.close();
        }
        
        private static void uploadResults() throws Exception {
//            Synapse conn = new Synapse();
//            conn.setAuthEndpoint("https://auth-staging.sagebase.org/auth/v1");
//            conn.setRepositoryEndpoint("https://repo-staging.sagebase.org/repo/v1");
//            conn.login(StackConfiguration.getIntegrationTestUserOneName(), StackConfiguration.getIntegrationTestUserOnePassword());
//            
//            // Get result file, append and send back
//            JSONObject layer = conn.getEntity("/layer/16856");
//            JSONObject location = conn.uploadLayerToSynapse(layer, tempResultFile);                  

        }
        
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration.getAuthenticationServicePublicEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(), StackConfiguration.getIntegrationTestUserOnePassword());
                // Can't save to Synapse because of cross stack issue
                // Save all files to local directory before I figure out how to save to arbitrary S3 bucket
                // Get temp directory for result file
                // TODO: Move to utils and add exception handling
//                File tmp = File.createTempFile("res", "tmp");
//                tmp.delete();
//                tmp.mkdir();
                
                String tmp = "/Users/xaviers/integrated_perf_results/";
                DateTime dt = new DateTime();
                DateTimeFormatter dtf = DateTimeFormat.shortDate();
                resultFilename = "results__" + dtf.print(dt).replaceAll("/", "_") + ".txt";
                tempResultFile = new File(tmp + File.separator + resultFilename);
	}

	/**
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@AfterClass
	public static void afterClass() throws Exception {
//            uploadResults();
//            tempResultFile.delete();
        }
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testCRUDIndependentProjects() throws Exception {            
            JSONObject result;
            JSONObject allResults = new JSONObject("{\"INSERT\":[],\"READ\":[],\"UPDATE\":[],\"DELETE\":[]}");
            int createdProjIds[];
            
            for (int numProjects = 1; numProjects <= maxNumProjects; numProjects *= 2) {
                createdProjIds = populate(EntityType.PROJECT, (numProjects == 1 ? numProjects : numProjects / 2), numProjects / 2 + 1, 16);
                result = doCRUD(EntityType.PROJECT, -1);
                allResults.getJSONArray("INSERT").put(result.getLong("elapsedTimeCreate"));
                allResults.getJSONArray("READ").put(result.getLong("elapsedTimeRead"));
                allResults.getJSONArray("UPDATE").put(result.getLong("elapsedTimeUpdate"));
                allResults.getJSONArray("DELETE").put(result.getLong("elapsedTimeDelete"));                
            }
            logResults("testCRUDIndependentProjects", allResults);
            cleanup(-1);
        }
        
        @Test
        public void testCRUDDatasetsSingleProject() throws Exception {
            JSONObject result;
            JSONObject allResults = new JSONObject("{\"INSERT\":[],\"READ\":[],\"UPDATE\":[],\"DELETE\":[]}");
            int rootProjectId;
            int createdDatasetIds[];
            
            rootProjectId = populate(EntityType.PROJECT, 1, 0, 16)[0];
            for (int numDatasets = 1; numDatasets <= maxNumDatasets; numDatasets *= 2) {
                createdDatasetIds = populate(EntityType.DATASET, (numDatasets == 1 ? numDatasets : numDatasets / 2), numDatasets / 2 + 1, rootProjectId, 16);
                result = doCRUD(EntityType.DATASET, rootProjectId);
                allResults.getJSONArray("INSERT").put(result.getLong("elapsedTimeCreate"));
                allResults.getJSONArray("READ").put(result.getLong("elapsedTimeRead"));
                allResults.getJSONArray("UPDATE").put(result.getLong("elapsedTimeUpdate"));
                allResults.getJSONArray("DELETE").put(result.getLong("elapsedTimeDelete"));                
            }
            logResults("testCRUDDatasetsSingleProject", allResults);
            cleanup(-1);
        }
        
//        @Test
//        public void testQueryAnnotations() throws Exception {
//            JSONObject result, rs;
//            JSONObject allResults = new JSONObject("{\"INSERT\":[],\"READ\":[],\"UPDATE\":[],\"DELETE\":[],\"QUERY\":[]}");
//            int rootProjectId;
//            long startTime, queryTime;        
//            
//            for (int numAnnotations = 1; numAnnotations <= 1024; numAnnotations *=2) {
//                rootProjectId = populate(EntityType.PROJECT, 1, 0, numAnnotations)[0];
//                startTime = System.nanoTime();
//                rs = synapse.query("select * from project where name == \"PerfTest_Project_0\"");
//                queryTime = System.nanoTime() - startTime;
//                allResults.getJSONArray("QUERY").put(queryTime);
//                cleanup(rootProjectId);
//            }
//            logResults("testQueryAnnotations", allResults);
//        }

}
