package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementBackup;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessRequirementBackupDriver implements GenericBackupDriver {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	
	public AccessRequirementBackupDriver() {}
	
	public AccessRequirementBackupDriver(AccessRequirementDAO accessRequirementDAO, AccessApprovalDAO accessApprovalDAO) {
		this.accessRequirementDAO = accessRequirementDAO;
		this.accessApprovalDAO = accessApprovalDAO;
	}

	static private Log log = LogFactory.getLog(AccessRequirementBackupDriver.class);
	
	private static final String ZIP_ENTRY_SUFFIX = ".xml";
	
	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> arsToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file does not exist: "
							+ destination.getAbsolutePath());

		// get the UserGroups, UserProfiles for the given IDs
		Set<String> arIds = null;
		if (arsToBackup==null) {
			// get all the ids in the system
			arIds = new HashSet<String>(accessRequirementDAO.getIds());
		} else {
			arIds = new HashSet<String>(arsToBackup);
		}

		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a backup to file: " + destination.getAbsolutePath());
		progress.setTotalCount(arIds.size());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing nodes:");
			for(String idToBackup: arIds){
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				AccessRequirement accessRequirement = accessRequirementDAO.get(idToBackup);
				List<AccessApproval> aas = accessApprovalDAO.getForAccessRequirement(idToBackup);
				AccessRequirementBackup backup = new AccessRequirementBackup();
				backup.setAccessRequirement(accessRequirement);
				backup.setAccessApprovals(aas);
				
				ZipEntry entry = new ZipEntry(idToBackup+ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeAccessRequirementBackup(backup, zos);
				progress.incrementProgress();
				if(progress.shouldTerminate()){
					throw new InterruptedException("Access Requirement Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing access requirement.");
		} finally {
			if (fos != null) {
				fos.flush();
				fos.close();
			}
		}
		return true;
	}

	@Override
	public boolean restoreFromBackup(File source, Progress progress)
			throws IOException, InterruptedException, DatastoreException, NotFoundException, InvalidModelException, ConflictingUpdateException {
		if(source == null) throw new IllegalArgumentException("Source file cannot be null");
		if(!source.exists()) throw new IllegalArgumentException("Source file dose not exist: "+source.getAbsolutePath());
		if(progress == null) throw new IllegalArgumentException("Progress cannot be null");
		FileInputStream fis = new FileInputStream(source);
		try{
			log.info("Restoring: "+source.getAbsolutePath());
			progress.appendLog("Restoring: "+source.getAbsolutePath());
			// First clear all data
			ZipInputStream zin = new  ZipInputStream(new BufferedInputStream(fis));
			progress.setMessage("Reading: "+source.getAbsolutePath());
			progress.setTotalCount(source.length());

			ZipEntry entry;
			progress.appendLog("Processing access requirements:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if(progress.shouldTerminate()){
					throw new InterruptedException("Access Requirement restoration terminated by the user.");
				}
				
				// This is a backup file.
				AccessRequirementBackup backup = NodeSerializerUtil.readAccessRequirementBackup(zin);
				
				createOrUpdateAccessRequirementAndApprovals(backup);
				
				// Append this id to the log.
				progress.appendLog(backup.getAccessRequirement().getId().toString());
				
				progress.incrementProgressBy(entry.getCompressedSize());
				if(log.isTraceEnabled()){
					log.trace(progress.toString());			
				}
				// This is run in a tight loop so to be CPU friendly we should yield
				
				Thread.yield();
			}
			progress.appendLog("Finished processing nodes.");
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}
	
	private void createOrUpdateAccessRequirementAndApprovals(AccessRequirementBackup backup) throws DatastoreException, NotFoundException, InvalidModelException, ConflictingUpdateException {
		// create the access requirement
		AccessRequirement ar = backup.getAccessRequirement();
		AccessRequirement arFromSystem = null;
		try {
			arFromSystem = accessRequirementDAO.get(ar.getId().toString());
		} catch (NotFoundException e) {
			arFromSystem = null;
		}
		if (null==arFromSystem) {
			ar = accessRequirementDAO.create(ar);
		} else {
			// Update only when backup is different from the current system
			if (!arFromSystem.getEtag().equals(ar.getEtag())) {
				ar = accessRequirementDAO.updateFromBackup(ar);
			}
		}
		
		// create the access approvals
		for (AccessApproval aa : backup.getAccessApprovals()) {
			if (!ar.getId().equals(aa.getRequirementId())) throw new 
				IllegalStateException("AccessApproval references requirement "+aa.getRequirementId()+
						", the ID in the backup file is "+backup.getAccessRequirement().getId()+
						", and the ID after restoration is "+ar.getId());
			AccessApproval aaFromSystem = null;
			try {
				aaFromSystem = accessApprovalDAO.get(aa.getId().toString());
			} catch (NotFoundException e) {
				aaFromSystem = null;
			}
			if (null==aaFromSystem) {
				accessApprovalDAO.create(aa);
			} else {
				// Update only when backup is different from the current system
				if (!arFromSystem.getEtag().equals(ar.getEtag())) {
					accessApprovalDAO.updateFromBackup(aa);
				}
			}
		}
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		accessRequirementDAO.delete(id);
	}

}
