package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class IT099AsynchronousJobTest {

	
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	private List<Entity> entitiesToDelete;
	private List<FileHandle> filesToDelete;
	
	private static long MAX_WAIT_MS = 1000*60*5;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		try {
			adminSynapse.clearAllLocks();
		} catch (SynapseNotFoundException nfe) {
			// if not found, then just continue
		}
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException{
		adminSynapse.clearAllLocks();
		entitiesToDelete = new LinkedList<Entity>();
		filesToDelete = Lists.newArrayList();
	}
	
	@After
	public void after() throws Exception {
		for (Entity entity : Lists.reverse(entitiesToDelete)) {
			try {
				adminSynapse.deleteEntity(entity);
			} catch (Exception e) {}
		}
		for (FileHandle fh : filesToDelete) {
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
	public void testUploadCSVToTable() throws Exception{
		File temp = File.createTempFile("UploadCSVTest", ".csv");
		File tempUpdate = File.createTempFile("UpdateCSVTest", ".csv");
		try{
			// Create a table with some columns
			ColumnModel cm1 = new ColumnModel();
			cm1.setColumnType(ColumnType.INTEGER);
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
			
			final String tableId = table.getId();
			
			// Now create a CSV
			CSVWriter csv = new CSVWriter(new FileWriter(temp));
			final long rowCount = 2;
			try{
				// Write the header
				csv.writeNext(new String[] { cm1.getName(), cm2.getName() });
				// Write some rows
				for(int i=0; i<rowCount; i++){
					csv.writeNext(new String[]{""+i, "data"+i});
				}
			}finally{
				csv.flush();
				csv.close();
			}

			// Now upload this CSV as a file handle
			FileHandle fileHandle = synapse.multipartUpload(temp, null, false, false);
			filesToDelete.add(fileHandle);
			// Get a file preview
			UploadToTablePreviewRequest previewRequest = new UploadToTablePreviewRequest();
			previewRequest.setUploadFileHandleId(fileHandle.getId());
			
			final int expectedColumnsNumber = table.getColumnIds().size();
			
			AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableCSVUploadPreview, previewRequest, (UploadToTablePreviewResult uploadPreviewResult) -> {
				assertNotNull(uploadPreviewResult);
				assertNotNull(uploadPreviewResult.getSuggestedColumns());
				assertNotNull(uploadPreviewResult.getSampleRows());
				assertNotNull(uploadPreviewResult.getRowsScanned());
				assertEquals(expectedColumnsNumber, uploadPreviewResult.getSuggestedColumns().size());
				assertEquals(rowCount, uploadPreviewResult.getSampleRows().size());
				assertEquals(new Long(rowCount), uploadPreviewResult.getRowsScanned());
			}, MAX_WAIT_MS);
			

			UploadToTableRequest uploadRequest = new UploadToTableRequest();
			uploadRequest.setTableId(tableId);
			uploadRequest.setUploadFileHandleId(fileHandle.getId());
			
			AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableCSVUpload, uploadRequest, (UploadToTableResult uploadResult) -> {
				assertNotNull(uploadResult.getEtag());
				assertEquals(rowCount, uploadResult.getRowsProcessed().longValue());				
			}, MAX_WAIT_MS);			

			// Wait for the table to be ready
			String sql = "select * from "+table.getId();
			
			RowSet results = waitForQuery(sql, tableId, (rowSet) -> {				
				assertEquals(rowCount, rowSet.getRows().size());
			});

			assertCount(sql, tableId, rowCount);
			
			DownloadFromTableRequest downloadRequest = new DownloadFromTableRequest();
			downloadRequest.setEntityId(tableId);
			downloadRequest.setSql(sql);
			downloadRequest.setWriteHeader(true);
			downloadRequest.setIncludeRowIdAndRowVersion(true);
			
			DownloadFromTableResult downloadResult = AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableCSVDownload, downloadRequest, (DownloadFromTableResult result) -> {
				assertNotNull(result);
			}, MAX_WAIT_MS).getResponse();			

			// Now download the file
			URL url = synapse.getFileHandleTemporaryUrl(downloadResult.getResultsFileHandleId());
			assertNotNull(url);
			File temp2 = File.createTempFile("downloadTemp", ".csv");
			CSVReader reader = null;
			List<String[]> downloadCSV = null;
			try {
				synapse.downloadFromFileHandleTemporaryUrl(downloadResult.getResultsFileHandleId(), temp2);
				reader = new CSVReader(new FileReader(temp2));
				downloadCSV = reader.readAll();
			} finally {
				if (reader != null) {
					reader.close();
				}
				temp2.delete();
			}
			assertNotNull(downloadCSV);
			// Should match the data from the results.
			assertEquals(results.getRows().size() + 1, downloadCSV.size());
			// Check the ids
			for (int i = 0; i < results.getRows().size(); i++) {
				Row row = results.getRows().get(i);
				String[] row2 = downloadCSV.get(i + 1);
				assertEquals(row.getRowId().toString(), row2[0]);
				assertEquals(row.getVersionNumber().toString(), row2[1]);
				// The CSV includes two extra columns (rowId and version)
				assertEquals(row.getValues().size(), row2.length - 2);
				// Check the first column
				assertEquals(row.getValues().get(0), row2[2]);
			}
			
			// In PLFM-2979, updating a table by uploading a CSV file fails
			// So here we add an integration test.
			// Now create a CSV
			csv = new CSVWriter(new FileWriter(tempUpdate));
			try{
				// this duplicates the first file upload but adds the row id
				// Write the header
				csv.writeNext(new String[]{"ROW_ID", "ROW_VERSION",cm1.getName(), cm2.getName()});
				// Write some rows
				for(int i=0; i<rowCount; i++){
					Row row = results.getRows().get(i);
					long j = rowCount-i;
					csv.writeNext(new String[]{row.getRowId().toString(), row.getVersionNumber().toString(),""+j, "data"+j});
				}
			}finally{
				csv.flush();
				csv.close();
			}
			
			// Now upload this CSV as a file handle
			fileHandle = synapse.multipartUpload(tempUpdate, null, false, false);
			filesToDelete.add(fileHandle);
			
			// We now have enough to apply the data to the table
			
			uploadRequest = new UploadToTableRequest();
			uploadRequest.setTableId(tableId);
			uploadRequest.setUploadFileHandleId(fileHandle.getId());
			uploadRequest.setUpdateEtag(downloadResult.getEtag());
			
			AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableCSVUpload, uploadRequest, (UploadToTableResult uploadResult) -> {
				assertNotNull(uploadResult.getEtag());
				assertEquals(rowCount, uploadResult.getRowsProcessed().longValue());				
			}, MAX_WAIT_MS);		
			
			// Wait for the table to be ready
			waitForQuery(sql, tableId, (updateResults) ->{				
				assertEquals(rowCount, updateResults.getRows().size());
				// since we updated the table this result should be different
				assertFalse(updateResults.getRows().equals(results.getRows()));
				assertFalse(results.getEtag().equals(updateResults.getEtag()));
			});

			assertCount(sql, tableId, rowCount);
			
		} finally{
			temp.delete();
			tempUpdate.delete();
		}
	}
	
	private RowSet waitForQuery(String sql, final String tableId, Consumer<RowSet> resultsConsumer) throws Exception {
		return AsyncJobHelper.assertQueryBundleResults(synapse, tableId, sql, null, null, SynapseClient.QUERY_PARTMASK, (bundle) ->{
			resultsConsumer.accept(bundle.getQueryResult().getQueryResults());
		}, MAX_WAIT_MS).getResponse().getQueryResult().getQueryResults();
	}

	private void assertCount(String sql, final String tableId, Long count) throws Exception {
		AsyncJobHelper.assertQueryBundleResults(synapse, tableId, sql, null, null, SynapseClient.COUNT_PARTMASK, (results) -> {
			assertEquals(count, results.getQueryCount());
		}, MAX_WAIT_MS);
	}
}
