package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import au.com.bytecode.opencsv.CSVWriter;

public class IT099AsynchronousJobTest {

	
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	private List<Entity> entitiesToDelete;
	private List<S3FileHandle> filesToDelete;
	
	private static long MAX_QUERY_TIMEOUT_MS = 1000*60*5;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(new StackConfiguration().getTableEnabled());
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before(){
		entitiesToDelete = new LinkedList<Entity>();
		filesToDelete = new LinkedList<S3FileHandle>();
	}
	
	@After
	public void after() throws Exception {
		for (Entity entity : entitiesToDelete) {
			try {
				adminSynapse.deleteAndPurgeEntity(entity);
			} catch (Exception e) {}
		}
		for(S3FileHandle fh: filesToDelete){
			try {
				adminSynapse.deleteFileHandle(fh.getId());
			} catch (Exception e) {}
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (Exception e) { }
	}
	
	@Test
	public void testUploadCSVToTable() throws IOException, SynapseException, JSONObjectAdapterException{
		File temp = File.createTempFile("UploadCSVTest", ".csv");
		try{
			// Create a table with some columns
			ColumnModel cm1 = new ColumnModel();
			cm1.setColumnType(ColumnType.LONG);
			cm1.setName("sampleLong");
			cm1 = synapse.createColumnModel(cm1);
			// String
			ColumnModel cm2 = new ColumnModel();
			cm2.setColumnType(ColumnType.STRING);
			cm2.setMaximumSize(10L);
			cm2.setName("sampleString");
			cm2 = synapse.createColumnModel(cm2);
			
			// Now create a table entity with this column model
			// Create a project to contain it all
			Project project = new Project();
			project.setName(UUID.randomUUID().toString());
			project = synapse.createEntity(project);
			assertNotNull(project);
			entitiesToDelete.add(project);
			
			// now create a table entity
			TableEntity table = new TableEntity();
			table.setName("Table");
			List<String> idList = new LinkedList<String>();
			idList.add(cm1.getId());
			idList.add(cm2.getId());
			table.setColumnIds(idList);
			table.setParentId(project.getId());
			table = synapse.createEntity(table);
			entitiesToDelete.add(table);
			
			// Now create a CSV
			CSVWriter csv = new CSVWriter(new FileWriter(temp));
			try{
				// Write the header
				csv.writeNext(new String[]{cm1.getName(), cm2.getName()});
				int rows = 2;
				// Write some rows
				for(int i=0; i<rows; i++){
					csv.writeNext(new String[]{""+i, "data"+i});
				}
			}finally{
				csv.flush();
				csv.close();
			}
			// Now upload this CSV as a file handle
			S3FileHandle fileHandle = synapse.createFileHandle(temp, "text/csv");
			filesToDelete.add(fileHandle);
			// We now have enough to apply the data to the table
			AsynchUploadJobBody body = new AsynchUploadJobBody();
			body.setTableId(table.getId());
			body.setUploadFileHandleId(fileHandle.getId());
			AsynchronousJobStatus status = synapse.startAsynchronousJob(body);
			assertNotNull(status);
			assertNotNull(status.getJobId());
			assertEquals(body, status.getJobBody());
			// Now make sure we can get the status
			status = synapse.getAsynchronousJobStatus(status.getJobId());
			assertNotNull(status);
			assertNotNull(status.getJobId());
			assertEquals(body, status.getJobBody());
			
		}finally{
			temp.delete();
		}
	}
	
}
