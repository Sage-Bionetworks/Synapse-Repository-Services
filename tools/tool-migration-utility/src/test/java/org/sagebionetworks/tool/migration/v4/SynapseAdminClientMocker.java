package org.sagebionetworks.tool.migration.v4;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.zip.CRC32;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.tool.migration.v3.SynapseAdminClientMockState;
import org.sagebionetworks.util.Closer;

import com.google.common.io.Closeables;
import com.thoughtworks.xstream.XStream;

/**
 * Helper for migration testing
 */
public class SynapseAdminClientMocker {
	
	/**
	 * Returns a mocked implementation of the SynapseAdminClient
	 * 
	 * All data for the stack is passed into the mock as a reference
	 * So changes due to method calls can be retrieved by the test by keeping a reference to the parameters
	 */
	public static SynapseAdminClient createMock(final SynapseAdminClientMockState state)
			throws Exception {
		// Start with a status of read/write
		StackStatus status = new StackStatus();
		status.setCurrentMessage("Synapse is read for read/write");
		status.setStatus(StatusEnum.READ_WRITE);
		state.statusHistory = new Stack<StackStatus>();
		state.statusHistory.push(status);
		
		SynapseAdminClient client = mock(SynapseAdminClient.class);
		
		when(client.deleteMigratableObject(any(MigrationType.class), any(IdList.class))).thenAnswer(new Answer<MigrationTypeCount>() {

			@Override
			public MigrationTypeCount answer(InvocationOnMock invocation)
					throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				IdList ids = (IdList) invocation.getArguments()[1];
				
				// Get the type
				Set<Long> toDelete = new HashSet<Long>();
				toDelete.addAll(ids.getList());
				state.deleteRequestsHistory.add(toDelete);
				
				List<RowMetadata> newList = new LinkedList<RowMetadata>();
				List<RowMetadata> current = state.metadata.get(migrationType);
				long count = 0;
				for (RowMetadata row : current) {
					if (!toDelete.contains(row.getId())) {
						newList.add(row);
					} else {
						// Row should be deleted, should it raise an exception?
						if (state.exceptionNodes.contains(row.getId())) {
							throw new SynapseClientException("SynapseException on node " + row.getId());
						}
						count++;
					}
				}
				// Save the new list
				state.metadata.put(migrationType, newList);
				MigrationTypeCount mtc = new MigrationTypeCount();
				mtc.setCount(count);
				mtc.setType(migrationType);
				return mtc;
			}
			
		});
		
		when(client.getCurrentStackStatus()).thenAnswer(new Answer<StackStatus>() {

			@Override
			public StackStatus answer(InvocationOnMock invocation)
					throws Throwable {
				return state.statusHistory.lastElement();
			}
			
		});
		
		when(client.updateCurrentStackStatus(any(StackStatus.class))).thenAnswer(new Answer<StackStatus>() {

			@Override
			public StackStatus answer(InvocationOnMock invocation)
					throws Throwable {
				StackStatus updated = (StackStatus) invocation.getArguments()[0];
				
				if (updated == null)
					throw new IllegalArgumentException("StackStatus cannot be null");
				StackStatus status = cloneJsonEntity(updated);
				state.statusHistory.push(status);
				return status;
			}
			
		});
		
		when(client.getTypeCounts()).thenAnswer(new Answer<MigrationTypeCounts>() {

			@Override
			public MigrationTypeCounts answer(InvocationOnMock invocation)
					throws Throwable {
				// Get the counts for each type
				List<MigrationTypeCount> list = new LinkedList<MigrationTypeCount>();
				Iterator<Entry<MigrationType, List<RowMetadata>>> it = state.metadata
						.entrySet().iterator();
				while (it.hasNext()) {
					Entry<MigrationType, List<RowMetadata>> entry = it.next();
					MigrationType type = entry.getKey();
					List<RowMetadata> values = entry.getValue();
					MigrationTypeCount count = new MigrationTypeCount();
					count.setCount(new Long(values.size()));
					count.setType(type);
					list.add(count);
				}
				MigrationTypeCounts result = new MigrationTypeCounts();
				result.setList(list);
				return result;
			}
			
		});
		
		when(client.getTypeCount(any(MigrationType.class))).thenAnswer(new Answer<MigrationTypeCount>() {
			@Override
			public MigrationTypeCount answer(InvocationOnMock invocation) throws Throwable {
				MigrationType t = (MigrationType) invocation.getArguments()[0];
				List<RowMetadata> l = state.metadata.get(t);
				
				MigrationTypeCount tc = new MigrationTypeCount();
				tc.setType(t);
				tc.setCount((long) l.size());
				long minId = Long.MAX_VALUE;
				long maxId = 0L;
				for (RowMetadata md: l) {
					long id = md.getId();
					if (id < minId) {
						minId = id;
					}
					if (id > maxId) {
						maxId = id;
					}
				}
				tc.setMinid(minId);
				tc.setMaxid(maxId);
				return tc;
			}
		});
		
		when(client.getPrimaryTypes()).thenAnswer(new Answer<MigrationTypeList>() {

			@Override
			public MigrationTypeList answer(InvocationOnMock invocation)
					throws Throwable {
				// treat all types as primary
				List<MigrationType> list = new LinkedList<MigrationType>();
				Iterator<Entry<MigrationType, List<RowMetadata>>> it = state.metadata
						.entrySet().iterator();
				while (it.hasNext()) {
					Entry<MigrationType, List<RowMetadata>> entry = it.next();
					MigrationType type = entry.getKey();
					list.add(type);
				}
				MigrationTypeList result = new MigrationTypeList();
				result.setList(list);
				return result;
			}
			
		});
		
		when(client.getMigrationTypes()).thenAnswer(new Answer<MigrationTypeList>() {
			@Override
			public MigrationTypeList answer(InvocationOnMock invocation) throws Throwable {
				MigrationTypeList res = new MigrationTypeList();
				// Retun types in correct order
				List<MigrationType> l = new LinkedList<MigrationType>();
				for (MigrationType t: MigrationType.values()) {
					if (state.metadata.keySet().contains(t)) {
						l.add(t);
					}
				}
				res.setList(l);
				return res;
			}
		});
		
		when(client.getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong())).thenAnswer(new Answer<MigrationRangeChecksum>() {
			@Override
			public MigrationRangeChecksum answer(InvocationOnMock invocation) throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				Long minId = (Long) invocation.getArguments()[2];
				Long maxId = (Long) invocation.getArguments()[3];

				CRC32 crc = new CRC32();
				List<RowMetadata> allRowMetadata = state.metadata.get(migrationType);
				for (RowMetadata rm: allRowMetadata) {
					if ((rm.getId() < minId) || (rm.getId() > maxId)) {
						continue;
					}
					String s = rm.getId().toString() + (rm.getEtag() == null ? "" : "@" + rm.getEtag());
					crc.update(s.getBytes());
				}
				MigrationRangeChecksum checksum = new MigrationRangeChecksum();
				checksum.setChecksum(crc.toString());
				checksum.setType(migrationType);
				checksum.setMinid(minId);
				checksum.setMaxid(maxId);
				return checksum;
			}
		});
		
		when(client.getChecksumForType(any(MigrationType.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				CRC32 crc = new CRC32();
				List<RowMetadata> allRowMetadata = state.metadata.get(migrationType);
				for (RowMetadata rm: allRowMetadata) {
					String s = rm.getId().toString() + (rm.getEtag() == null ? "" : "@" + rm.getEtag());
					crc.update(s.getBytes());
				}
				
				return crc.toString();
			}
		});
		
		
		when(client.getRepoEndpoint()).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return state.endpoint;
			}
			
		});
		
		when(client.getRowMetadata(any(MigrationType.class), anyLong(), anyLong())).thenAnswer(new Answer<RowMetadataResult>() {

			@Override
			public RowMetadataResult answer(InvocationOnMock invocation)
					throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				Long limit = (Long) invocation.getArguments()[1];
				Long offset = (Long) invocation.getArguments()[2];
				
				if (migrationType == null)
					throw new IllegalArgumentException("Type cannot be null");
				List<RowMetadata> list = state.metadata.get(migrationType);
				RowMetadataResult result = new RowMetadataResult();
				result.setTotalCount(new Long(list.size()));
				if (offset < list.size()) {
					long endIndex = Math.min(list.size(), offset + limit);
					List<RowMetadata> subList = list.subList(offset.intValue(),
							(int) endIndex);
					result.setList(subList);
				} else {
					result.setList(new LinkedList<RowMetadata>());
				}
				return result;
			}
			
		});
		
		when(client.getRowMetadataByRange(any(MigrationType.class), eq(60L), eq(64L), eq(5L), anyLong())
				).thenAnswer(new Answer<RowMetadataResult>() {

					@Override
					public RowMetadataResult answer(InvocationOnMock invocation)
							throws Throwable {
						MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
						Long minId = (Long) invocation.getArguments()[1];
						Long maxId = (Long) invocation.getArguments()[2];
						
						if (migrationType == null)
							throw new IllegalArgumentException("Type cannot be null");
						
						List<RowMetadata> list = state.metadata.get(migrationType);
						RowMetadataResult result = new RowMetadataResult();
						result.setTotalCount(new Long(list.size()));
						
						List<RowMetadata> resultList = new LinkedList<RowMetadata>();
						
						for (RowMetadata rm: list) {
							if ((rm.getId() >= minId) && (rm.getId() <= maxId)) {
								resultList.add(rm);
							}
						}
						
						result.setList(resultList);
						return result;
					}
				
			});
		
		when(client.getRowMetadataByRange(any(MigrationType.class), eq(50L), eq(54L), eq(5L), anyLong())
			).thenAnswer(new Answer<RowMetadataResult>() {

				@Override
				public RowMetadataResult answer(InvocationOnMock invocation)
						throws Throwable {
					MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
					Long minId = (Long) invocation.getArguments()[1];
					Long maxId = (Long) invocation.getArguments()[2];
					
					if (migrationType == null)
						throw new IllegalArgumentException("Type cannot be null");
					
					List<RowMetadata> list = state.metadata.get(migrationType);
					RowMetadataResult result = new RowMetadataResult();
					result.setTotalCount(new Long(list.size()));
					
					List<RowMetadata> resultList = new LinkedList<RowMetadata>();
					
					for (RowMetadata rm: list) {
						if ((rm.getId() >= minId) && (rm.getId() <= maxId)) {
							resultList.add(rm);
						}
					}
					
					result.setList(resultList);
					return result;
				}
			
		}).then(new Answer<RowMetadataResult>() {

			@Override
			public RowMetadataResult answer(InvocationOnMock invocation)
					throws Throwable {
				RowMetadataResult result = new RowMetadataResult();
				List<RowMetadata> resultList = new LinkedList<RowMetadata>();
				result.setList(resultList);
				return result;
			}
			
		});
		
		when(client.getRowMetadataByRange(any(MigrationType.class), eq(50L), eq(64L), eq(7L), anyLong())
			).thenAnswer(new Answer<RowMetadataResult>() {
					// 1st page: offset 0
					@Override
					public RowMetadataResult answer(InvocationOnMock invocation)
							throws Throwable {
						MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
						Long minId = (Long) invocation.getArguments()[1];
						Long maxId = (Long) invocation.getArguments()[2];
						Long batchSize = (Long) invocation.getArguments()[3];
						Long offset = (long) invocation.getArguments()[4];
						
						if (migrationType == null)
							throw new IllegalArgumentException("Type cannot be null");
						
						List<RowMetadata> list = state.metadata.get(migrationType);
						RowMetadataResult result = new RowMetadataResult();
						result.setTotalCount(new Long(list.size()));
						
						List<RowMetadata> resultList = new LinkedList<RowMetadata>();
						
						long currentOffset = 0;
						for (RowMetadata rm: list) {
							if ((currentOffset >= offset) && (rm.getId() >= minId) && (rm.getId() <= maxId)) {
								resultList.add(rm);
							}
							currentOffset++;
						}
						
						result.setList(resultList);
						return result;
					}
				
			}).thenAnswer(new Answer<RowMetadataResult>() {
				// 2nd page: offset 7
				@Override
				public RowMetadataResult answer(InvocationOnMock invocation)
						throws Throwable {
					MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
					Long minId = (Long) invocation.getArguments()[1];
					Long maxId = (Long) invocation.getArguments()[2];
					Long batchSize = (Long) invocation.getArguments()[3];
					Long offset = (long) invocation.getArguments()[4];
					
					if (migrationType == null)
						throw new IllegalArgumentException("Type cannot be null");
					
					List<RowMetadata> list = state.metadata.get(migrationType);
					RowMetadataResult result = new RowMetadataResult();
					result.setTotalCount(new Long(list.size()));
					
					List<RowMetadata> resultList = new LinkedList<RowMetadata>();
					
					long currentOffset = 7;
					for (RowMetadata rm: list) {
						if ((currentOffset >= offset) && (rm.getId() >= minId) && (rm.getId() <= maxId)) {
							resultList.add(rm);
						}
						currentOffset++;
					}
					
					result.setList(resultList);
					return result;
				}
			
			}).then(new Answer<RowMetadataResult>() {

				@Override
				public RowMetadataResult answer(InvocationOnMock invocation)
						throws Throwable {
					RowMetadataResult result = new RowMetadataResult();
					List<RowMetadata> resultList = new LinkedList<RowMetadata>();
					result.setList(resultList);
					return result;
				}
				
			});
			
		when(client.startBackup(any(MigrationType.class), any(IdList.class))).thenAnswer(new Answer<BackupRestoreStatus>() {

			@Override
			public BackupRestoreStatus answer(InvocationOnMock invocation)
					throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				IdList ids = (IdList) invocation.getArguments()[1];
				
				// Create a tempFile that will contain the backup data.
				try {
					// Find the data in question and write it to a backup file
					Set<Long> toBackup = new HashSet<Long>();
					toBackup.addAll(ids.getList());
					List<RowMetadata> backupList = new LinkedList<RowMetadata>();
					List<RowMetadata> current = state.metadata.get(migrationType);
					for (RowMetadata row : current) {
						if (toBackup.contains(row.getId())) {
							backupList.add(row);
						}
					}
					File temp = writeBackupFile(backupList);
					state.status = new BackupRestoreStatus();
					state.status.setStatus(DaemonStatus.STARTED);
					state.status.setId("" + state.statusSequence++);
					state.status.setType(DaemonType.BACKUP);
					state.status.setBackupUrl(temp.getAbsolutePath().replace("\\", "/"));
					state.status.setProgresssCurrent(0l);
					state.status.setProgresssTotal(10l);
					return state.status;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
		
		when(client.startRestore(any(MigrationType.class), any(RestoreSubmission.class))).thenAnswer(new Answer<BackupRestoreStatus>() {

			@Override
			public BackupRestoreStatus answer(InvocationOnMock invocation)
					throws Throwable {
				MigrationType migrationType = (MigrationType) invocation.getArguments()[0];
				RestoreSubmission req = (RestoreSubmission) invocation.getArguments()[1];
				
				// First read the backup file
				List<RowMetadata> restoreList = readRestoreFile(req);
				// Build up the new list of values
				List<RowMetadata> current = state.metadata.get(migrationType);
				for(RowMetadata toAdd: restoreList){
					// Is this already in the list
					boolean updated = false;
					for(RowMetadata existing: current){
						if(toAdd.getId().equals(existing.getId())){
							existing.setEtag(toAdd.getEtag());
							updated = true;
						}
					}
					// If not updated then add it
					if(!updated){
						current.add(toAdd);
					}
				}
				// Sort it to put it back in its natural order
				Collections.sort(current, new Comparator<RowMetadata>() {
					@Override
					public int compare(RowMetadata one, RowMetadata two) {
						Long oneL = one.getId();
						Long twoL = two.getId();
						return oneL.compareTo(twoL);
					}
				});
				state.status = new BackupRestoreStatus();
				state.status.setStatus(DaemonStatus.STARTED);
				state.status.setType(DaemonType.RESTORE);
				state.status.setId("" + state.statusSequence++);
				state.status.setProgresssCurrent(0l);
				state.status.setProgresssTotal(10l);
				return state.status;
			}
			
		});
		
		when(client.getStatus(anyString())).thenAnswer(new Answer<BackupRestoreStatus>() {

			@Override
			public BackupRestoreStatus answer(InvocationOnMock invocation)
					throws Throwable {
				// String daemonId = (String) invocation.getArguments()[0];
				
				// Change the status to finished
				state.status.setStatus(DaemonStatus.COMPLETED);
				state.status.setProgresssCurrent(9l);
				return state.status;
			}
			
		});
		
		when(client.fireChangeMessages(anyLong(), anyLong())).thenAnswer(new Answer<FireMessagesResult>() {

			@Override
			public FireMessagesResult answer(InvocationOnMock invocation)
					throws Throwable {
				Long startChangeNumber = (Long) invocation.getArguments()[0];
				Long limit = (Long) invocation.getArguments()[1];
				
				// Add this call to the history
				state.replayChangeNumbersHistory.add(startChangeNumber);
				FireMessagesResult result = new FireMessagesResult();
				long nextChangeNumber = -1;
				if(startChangeNumber + limit > state.maxChangeNumber){
					nextChangeNumber = -1;
				}else{
					nextChangeNumber = startChangeNumber + limit + 1;
				}
				result.setNextChangeNumber(nextChangeNumber);
				return result;
			}
			
		});
		
		when(client.getCurrentChangeNumber()).thenAnswer(new Answer<FireMessagesResult>() {

			@Override
			public FireMessagesResult answer(InvocationOnMock invocation)
					throws Throwable {
				FireMessagesResult result = new FireMessagesResult();
				// Pop a number off of the stack
				result.setNextChangeNumber(state.currentChangeNumberStack.pop());
				return result;
			}
			
		});

		return client;
	}

	/**
	 * Create a clone of a JSONEntity.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JSONEntity> T cloneJsonEntity(T toClone)
			throws JSONObjectAdapterException {
		if (toClone == null) {
			throw new IllegalArgumentException("Clone cannot be null");
		}
		// First go to JSON
		String json = EntityFactory.createJSONStringForEntity(toClone);
		return (T) EntityFactory.createEntityFromJSONString(json,
				toClone.getClass());
	}

	/**
	 * Helper to write a backup file
	 */
	private static File writeBackupFile(List<RowMetadata> backupList)
			throws IOException, FileNotFoundException {
		// write to a file
		File temp = File.createTempFile("tempBackupFile", ".tmp");
		FileOutputStream out = null;
		Writer zipWriter = null;
		try {
			out = new FileOutputStream(temp);
			XStream xstream = new XStream();
			zipWriter = new OutputStreamWriter(out, "UTF-8");
			xstream.toXML(backupList, zipWriter);
		} finally {
			Closer.closeQuietly(zipWriter, out);
		}
		return temp;
	}
	

	/**
	 * Read a restore file
	 */
	@SuppressWarnings("unchecked")
	private static List<RowMetadata> readRestoreFile(RestoreSubmission req) {
		try {
			File placeHolder = File.createTempFile("notUsed", ".tmp");
			req.getFileName();
			File temp = new File(placeHolder.getParentFile(), req.getFileName());
			if (!temp.exists())
				throw new RuntimeException("file does not exist: "
						+ temp.getAbsolutePath());
			// Read the temp file
			InputStream in = new FileInputStream(temp);
			try {
				XStream xstream = new XStream();
				return  (List<RowMetadata>) xstream.fromXML(in);
			} finally {
				in.close();
				placeHolder.delete();
				temp.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
