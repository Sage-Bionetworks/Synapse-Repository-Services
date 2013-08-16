package org.sagebionetworks.tool.migration.v3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.migration.IdList;
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

import com.thoughtworks.xstream.XStream;

/**
 * Stub implementation of synapse for testing. All data for the stack is stored
 * within this object.
 * 
 * @author jmhill
 * 
 */
public class StubSynapseAdministration implements SynapseAdministrationInt {

	Stack<StackStatus> statusHistory;
	String endpoint;
	LinkedHashMap<MigrationType, List<RowMetadata>> metadata;
	BackupRestoreStatus status;
	long statusSequence = 0;
	
	Stack<Long> currentChangeNumberStack = new Stack<Long>();
	Long maxChangeNumber = 100l;
	List<Long> replayChangeNumbersHistory = new LinkedList<Long>();
	List<Set<Long>> deleteRequestsHistory = new LinkedList<Set<Long>>();
	Set<Long> exceptionNodes = new HashSet<Long>();

	/**
	 * Create a new stub
	 */
	public StubSynapseAdministration(String endpoint) {
		// Start with a status of read/write
		StackStatus status = new StackStatus();
		status.setCurrentMessage("Synapse is read for read/write");
		status.setStatus(StatusEnum.READ_WRITE);
		statusHistory = new Stack<StackStatus>();
		statusHistory.push(status);
		this.endpoint = endpoint;
	}

	
	/*
	 * Methods that are not part of the interface.
	 */

	/**
	 * Create a clone of a JSONEntity.
	 * 
	 * @param toClone
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JSONEntity> T cloneJsonEntity(T toClone)
			throws JSONObjectAdapterException {
		if (toClone == null)
			throw new IllegalArgumentException("Clone cannot be null");
		// First go to JSON
		String json = EntityFactory.createJSONStringForEntity(toClone);
		return (T) EntityFactory.createEntityFromJSONString(json,
				toClone.getClass());
	}

	public Stack<Long> getCurrentChangeNumberStack() {
		return currentChangeNumberStack;
	}


	public void setCurrentChangeNumberStack(Stack<Long> currentChangeNumberStack) {
		this.currentChangeNumberStack = currentChangeNumberStack;
	}


	public Long getMaxChangeNumber() {
		return maxChangeNumber;
	}


	public void setMaxChangeNumber(Long maxChangeNumber) {
		this.maxChangeNumber = maxChangeNumber;
	}


	public List<Long> getReplayChangeNumbersHistory() {
		return replayChangeNumbersHistory;
	}


	/**
	 * Get the full history of status changes made to this stack.
	 * 
	 * @return
	 */
	public Stack<StackStatus> getStatusHistory() {
		return statusHistory;
	}

	/**
	 * The Map<MigrationType, List<RowMetadata>> used by this stub.
	 * 
	 * @return
	 */
	public Map<MigrationType, List<RowMetadata>> getMetadata() {
		return metadata;
	}

	/**
	 * Map<MigrationType, List<RowMetadata>> used by this stub.
	 * 
	 * @param metadata
	 */
	public void setMetadata(
			LinkedHashMap<MigrationType, List<RowMetadata>> metadata) {
		this.metadata = metadata;
	}
	
	public List<Set<Long>> getDeleteRequestsHistory() {
		return this.deleteRequestsHistory;
	}
	
	public Set<Long> getExceptionNodes() {
		return this.exceptionNodes;
	}
	
	public void setExceptionNodes(Set<Long> exceptionNodes) {
		this.exceptionNodes = exceptionNodes;
	}

	/*
	 * Methods that are part of the interface.
	 */

	@Override
	public StackStatus getCurrentStackStatus() throws SynapseException,
			JSONObjectAdapterException {
		return statusHistory.lastElement();
	}

	@Override
	public StackStatus updateCurrentStackStatus(StackStatus updated)
			throws JSONObjectAdapterException, SynapseException {
		if (updated == null)
			throw new IllegalArgumentException("StackStatus cannot be null");
		StackStatus status = cloneJsonEntity(updated);
		statusHistory.push(status);
		return status;
	}

	@Override
	public RowMetadataResult getRowMetadata(MigrationType migrationType,
			Long limit, Long offset) throws SynapseException,
			JSONObjectAdapterException {
		if (migrationType == null)
			throw new IllegalArgumentException("Type cannot be null");
		List<RowMetadata> list = this.metadata.get(migrationType);
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

	@Override
	public String getRepoEndpoint() {
		return this.endpoint;
	}

	@Override
	public MigrationTypeCounts getTypeCounts() throws SynapseException,
			JSONObjectAdapterException {
		// Get the counts for each type
		List<MigrationTypeCount> list = new LinkedList<MigrationTypeCount>();
		Iterator<Entry<MigrationType, List<RowMetadata>>> it = this.metadata
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

	private Long maxId(List<RowMetadata> vals) {
		Long m = vals.get(0).getId();
		for (RowMetadata v: vals) {
			if (v.getId() > m) {
				m = v.getId();
			}
		}
		return m;
	}

	@Override
	public MigrationTypeList getPrimaryTypes() throws SynapseException,
			JSONObjectAdapterException {
		// treat all types as primary
		List<MigrationType> list = new LinkedList<MigrationType>();
		Iterator<Entry<MigrationType, List<RowMetadata>>> it = this.metadata
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

	@Override
	public MigrationTypeCount deleteMigratableObject(
			MigrationType migrationType, IdList ids)
			throws JSONObjectAdapterException, SynapseException {
		// Get the type
		Set<Long> toDelete = new HashSet<Long>();
		toDelete.addAll(ids.getList());
		deleteRequestsHistory.add(toDelete);
		
		List<RowMetadata> newList = new LinkedList<RowMetadata>();
		List<RowMetadata> current = this.metadata.get(migrationType);
		long count = 0;
		for (RowMetadata row : current) {
			if (!toDelete.contains(row.getId())) {
				newList.add(row);
			} else {
				// Row should be deleted, should it raise an exception?
				if (exceptionNodes.contains(row.getId())) {
					throw new SynapseException("SynapseException on node " + row.getId());
				}
				count++;
			}
		}
		// Save the new list
		this.metadata.put(migrationType, newList);
		MigrationTypeCount mtc = new MigrationTypeCount();
		mtc.setCount(count);
		mtc.setType(migrationType);
		return mtc;
	}

	@Override
	public BackupRestoreStatus startBackup(MigrationType migrationType,
			IdList ids) throws JSONObjectAdapterException, SynapseException {
		// Create a tempFile that will contain the backup data.
		try {
			// Find the data in question and write it to a backup file
			Set<Long> toBackup = new HashSet<Long>();
			toBackup.addAll(ids.getList());
			List<RowMetadata> backupList = new LinkedList<RowMetadata>();
			List<RowMetadata> current = this.metadata.get(migrationType);
			for (RowMetadata row : current) {
				if (toBackup.contains(row.getId())) {
					backupList.add(row);
				}
			}
			File temp = writeBackupFile(backupList);
			status = new BackupRestoreStatus();
			status.setStatus(DaemonStatus.STARTED);
			status.setId(""+statusSequence++);
			status.setType(DaemonType.BACKUP);
			status.setBackupUrl(temp.getAbsolutePath().replace("\\", "/"));
			status.setProgresssCurrent(0l);
			status.setProgresssTotal(10l);
			return status;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Helper to write a backup file.
	 * @param backupList
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private File writeBackupFile(List<RowMetadata> backupList)
			throws IOException, FileNotFoundException {
		// write to a file
		File temp = File.createTempFile("tempBackupFile", ".tmp");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(temp);
			XStream xstream = new XStream();
			xstream.toXML(backupList, out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return temp;
	}

	@Override
	public BackupRestoreStatus startRestore(MigrationType migrationType, RestoreSubmission req) throws JSONObjectAdapterException,
			SynapseException {
		// First read the backup file
		List<RowMetadata> restoreList = readRestoreFile(req);
		// Build up the new list of values
		List<RowMetadata> current = this.metadata.get(migrationType);
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
		status = new BackupRestoreStatus();
		status.setStatus(DaemonStatus.STARTED);
		status.setType(DaemonType.RESTORE);
		status.setId(""+statusSequence++);
		status.setProgresssCurrent(0l);
		status.setProgresssTotal(10l);
		return status;

	}

	@Override
	public BackupRestoreStatus getStatus(String daemonId)
			throws JSONObjectAdapterException, SynapseException {
		// Change the status to finished
		status.setStatus(DaemonStatus.COMPLETED);
		status.setProgresssCurrent(9l);
		return status;
	}

	/**
	 * Read a restore file
	 * 
	 * @param req
	 * @return
	 */
	private List<RowMetadata> readRestoreFile(RestoreSubmission req) {
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

	@Override
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException, JSONObjectAdapterException {
		// Add this call to the history
		replayChangeNumbersHistory.add(startChangeNumber);
		FireMessagesResult result = new FireMessagesResult();
		long nextChangeNumber = -1;
		if(startChangeNumber + limit > maxChangeNumber){
			nextChangeNumber = -1;
		}else{
			nextChangeNumber = startChangeNumber + limit + 1;
		}
		result.setNextChangeNumber(nextChangeNumber);
		return result;
	}

	@Override
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException,
			JSONObjectAdapterException {
		FireMessagesResult result = new FireMessagesResult();
		// Pop a number off of the stack
		result.setNextChangeNumber(currentChangeNumberStack.pop());
		return result;
	}

}
