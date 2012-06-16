package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.SchemaCache;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;

public class PrincipalBackupDriver implements NodeBackupDriver {
	public static final String PRINCIPAL_XML_FILE = "principals.xml";
	
	static private Log log = LogFactory.getLog(PrincipalBackupDriver.class);
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserProfileDAO userProfileDAO;
	
	@Autowired
	NodeBackupManager backupManager;
	
	public PrincipalBackupDriver() {}
	
	// for testing
	public PrincipalBackupDriver(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
	}
	

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> principalsToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file dose not exist: "
							+ destination.getAbsolutePath());

		// get the UserGroups, UserProfiles for the given IDs
		List<PrincipalBackup> backups = new ArrayList<PrincipalBackup>();
		Set<String> principalIds = new HashSet<String>(principalsToBackup);
		// get all the groups
		Collection<UserGroup> groups = userGroupDAO.getAll(false);
		
		progress.setTotalCount(principalsToBackup.size());
		long currentIndex = 0L;

		for (UserGroup g : groups) {
			if (!principalIds.contains(g.getId())) continue;
			PrincipalBackup pb = new PrincipalBackup();
			pb.setUserGroup(g);
			pb.setUserProfile(null); // no user profile for a multiuser group
			backups.add(pb);
			progress.setCurrentIndex(++currentIndex);
		}
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		Collection<UserGroup> users = userGroupDAO.getAll(true);
		for (UserGroup u : users) {
			if (!principalIds.contains(u.getId())) continue;
			PrincipalBackup pb = new PrincipalBackup();
			pb.setUserGroup(u);
			UserProfile userProfile = userProfileDAO.get(u.getId(), schema);
			pb.setUserProfile(userProfile);
			backups.add(pb);
			progress.setCurrentIndex(++currentIndex);
		}
		
		// serialize and write to 'destination'
		log.info("Starting a principal backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a principal backup to file: " + destination.getAbsolutePath());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			Thread.yield();
			ZipEntry entry = new ZipEntry(PRINCIPAL_XML_FILE);
			zos.putNextEntry(entry);
			NodeSerializerUtil.writePrincipalBackups(backups, zos);
			zos.close();
			progress.appendLog("Finished processing principals.");
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
			throws IOException, InterruptedException {
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
			progress.appendLog("Processing principals:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				NodeBackupDriverImpl.checkForTermination(progress);

				Collection<PrincipalBackup> principalBackups = NodeSerializerUtil.readPrincipalBackups(zin);
				createOrUpdatePrincipals(principalBackups, progress);
				
				Thread.yield();
			}
			progress.appendLog("Finished processing principals.");

		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}
	
	private void createOrUpdatePrincipals(Collection<PrincipalBackup> principalBackups, Progress progress) {
		try {
		    ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
			for (PrincipalBackup pb : principalBackups) {
				UserGroup srcUserGroup = pb.getUserGroup();
				String id = srcUserGroup.getId();
				boolean isIndividual = srcUserGroup.getIsIndividual();
				if (isIndividual && pb.getUserProfile()==null) throw new IllegalArgumentException("No user profile for individual group "+id);
				if (!isIndividual && pb.getUserProfile()!=null) throw new IllegalArgumentException("Unexpected user profile for multi-user group "+id+" "+pb.getUserProfile().getDisplayName());

				// get the UserGroup with the matching ID
				UserGroup idMatchingUserGroup = null;
				try {
					idMatchingUserGroup = userGroupDAO.get(id);
				} catch (NotFoundException nfe) {
					idMatchingUserGroup = null;
				}

				// get the UserGroup with the matching name (or return null)
				UserGroup nameMatchingUserGroup = userGroupDAO.findGroup(srcUserGroup.getName(), isIndividual);
				
				boolean exists = false;
				if (idMatchingUserGroup==null) {
					if (nameMatchingUserGroup == null) {
						// UserGroup doesn't exist and will be created
						exists = false;
					} else {
						migrateUserGroup(nameMatchingUserGroup, srcUserGroup, progress);
						exists = true;
					}
				} else {
					if (nameMatchingUserGroup == null) {
						// need to delete the erroneous idMatchingUserGroup
						deleteUserGroup(idMatchingUserGroup);
						exists = false;
					} else {
						if (idMatchingUserGroup.equals(nameMatchingUserGroup)) {
							// we're good-to-go
							exists = true;
						} else {
							// need to delete the erroneous idMatchingUserGroup
							deleteUserGroup(idMatchingUserGroup);
							migrateUserGroup(nameMatchingUserGroup, srcUserGroup, progress);
							exists = true;
						}
					}
				}

				
				if (exists) {
	 				userGroupDAO.update(srcUserGroup);
	 			} else {
	 				userGroupDAO.create(srcUserGroup);
	 			}
				if (!isIndividual) continue;
				// now, for individuals, we also migrate the user profile
				UserProfile dstUserProfile = null;
				try {
					dstUserProfile = userProfileDAO.get(id, schema);
					exists = true;
				} catch (NotFoundException e) {
					exists = false;
				}
				UserProfile srcUserProfile = pb.getUserProfile();
				if (exists) {
					srcUserProfile.setEtag(dstUserProfile.getEtag());
					userProfileDAO.update(srcUserProfile, schema);
				} else {
					userProfileDAO.create(srcUserProfile, schema);
				}
			}
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (InvalidModelException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		} catch (ConflictingUpdateException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void deleteUserGroup(UserGroup ug) throws DatastoreException, NotFoundException {
		if (ug.getIsIndividual()) {
			userProfileDAO.delete(ug.getId());
		}
		userGroupDAO.delete(ug.getId());
	}
	
	/**
	 * When we have a UserGroup in the system which has the name that *we* want to
	 * use under a different principal ID we proceed as follows:
	 * (1) change the name of the existing user group;
	 * (2) create the desired group
	 * (3) update the dependent ResourceAccess, Entity, and Revision objects to the new UserGroup
	 * (4) delete the original UserGroup
	 * 
	 * @param nameMatchingUserGroup
	 * @param id
	 * @throws ConflictingUpdateException 
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	private void migrateUserGroup(UserGroup nameMatchingUserGroup, UserGroup srcUserGroup, Progress progress) throws DatastoreException, InvalidModelException, NotFoundException, ConflictingUpdateException {
		String name = nameMatchingUserGroup.getName();
		if (!name.equals(srcUserGroup.getName())) throw new IllegalStateException(name+" differs from "+srcUserGroup.getName());
	
		// change the name of the existing group
		nameMatchingUserGroup.setName(nameMatchingUserGroup.getName()+"_"+System.currentTimeMillis());
		userGroupDAO.update(nameMatchingUserGroup);
		
		// create the desired group under the desired ID
		String id = userGroupDAO.create(srcUserGroup);
		if (!id.equals(srcUserGroup.getId())) throw new IllegalStateException("srcUserGroup.getId()="+srcUserGroup.getId()+" id="+id);
		
		// address foreign key problems
		backupManager.clearAllData();
		progress.appendLog("Cleared data to remove foreign key constraints while migrating principals.");
		
		// delete the original UserGroup
		try {
			deleteUserGroup(nameMatchingUserGroup);
		} catch (Exception e) {
			progress.appendLog("Encountered exception deleting user group "+nameMatchingUserGroup.getId()+" "+nameMatchingUserGroup.getName());
			progress.appendLog(e.getMessage());
		}
	}


}
