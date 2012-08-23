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
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;

public class PrincipalBackupDriver implements GenericBackupDriver {
	public static final String PRINCIPAL_XML_FILE = "principals.xml";
	
	static private Log log = LogFactory.getLog(PrincipalBackupDriver.class);
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserProfileDAO userProfileDAO;
	
	@Autowired
	UserManager userManager;
	
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
					"Destination file does not exist: "
							+ destination.getAbsolutePath());

		// get the UserGroups, UserProfiles for the given IDs
		List<PrincipalBackup> backups = new ArrayList<PrincipalBackup>();
		Set<String> principalIds = null;
		boolean migrateEverything = false;
		if (principalsToBackup==null) {
			migrateEverything = true;
		} else {
			principalIds = new HashSet<String>(principalsToBackup);
		}
		// get all the groups
		Collection<UserGroup> groups = userGroupDAO.getAll(false);
		// get all the users
		Collection<UserGroup> users = userGroupDAO.getAll(true);
		
		int total = 0;
		if (migrateEverything) {
			total = groups.size() + users.size();
		} else {
			total = principalIds.size();
		}
		progress.setTotalCount(total);
		
		long currentIndex = 0L;

		for (UserGroup g : groups) {
			if (!migrateEverything && !principalIds.contains(g.getId())) continue;
			PrincipalBackup pb = new PrincipalBackup();
			pb.setUserGroup(g);
			pb.setUserProfile(null); // no user profile for a multiuser group
			backups.add(pb);
			progress.setCurrentIndex(++currentIndex);
		}
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		for (UserGroup u : users) {
			if (!migrateEverything && !principalIds.contains(u.getId())) continue;
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
						// This is a pretty bad state:  There is a group with my name but some other ID.
						// This could happen for bootstrapped groups, but should not happen for any other groups.
						// For bootstrapped groups, we clean up the database, delete the offending group and start over.
						// For other groups, we can only throw an exception.
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
							// As above, this is a pretty bad state:  There is one group using my ID and another group using my name.
							// We try to recover by deleting the group who has appropriated my ID and then cleaning the database,
							// deleting the group using my name and starting over.  
							//
							// need to delete the erroneous idMatchingUserGroup
							deleteUserGroup(idMatchingUserGroup);
							// now clean up the database and start over
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
					// is an update needed or are the profiles the same?  check the etags to answer
					if (!srcUserProfile.getEtag().equals(dstUserProfile.getEtag())) {
						srcUserProfile.setEtag(dstUserProfile.getEtag());
						userProfileDAO.update(srcUserProfile, schema);
					}
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
	
	// we delete groups via the UserManager.deletePrincipal, rather than UserGroupDAO.delete()
	// to ensure that the user is removed from the UserManager userInfo cache
	private void deleteUserGroup(UserGroup ug) throws DatastoreException, NotFoundException {
		userManager.deletePrincipal(ug.getName());
	}
	
	/**
	 * When we have a UserGroup in the system which has the name that *we* want to
	 * use under a different principal ID we proceed as follows:
	 * (1) change the name of the existing user group;
	 * (2) create the desired group
	 * (3) delete entities (to avoid foreign key violations in the next step).
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
		
		// address foreign key problems:
		// bootstrapped entities referred to bootstrapped users, so they have to be deleted
		if (isBootstrappedPrincipal(name)) {
			backupManager.clearAllData();
			progress.appendLog("Cleared data to remove foreign key constraints while migrating principals.");
		}
		
		// delete the original UserGroup
		try {
			deleteUserGroup(nameMatchingUserGroup);
		} catch (Exception e) {
			progress.appendLog("Encountered exception deleting user group "+nameMatchingUserGroup.getId()+" "+nameMatchingUserGroup.getName());
			progress.appendLog(e.getMessage());
		}
		
		// create the desired group under the desired ID
		String id = userGroupDAO.create(srcUserGroup);
		if (!id.equals(srcUserGroup.getId())) throw new IllegalStateException("srcUserGroup.getId()="+srcUserGroup.getId()+" id="+id);
		
	}
	
	public static boolean isBootstrappedPrincipal(String name) {
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(name)) return true;
		boolean foundit = false;
		for (DEFAULT_GROUPS g : DEFAULT_GROUPS.values()) {
			if (g.name().equals(name)) foundit=true;
		}
		return foundit;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		UserGroup ug = userGroupDAO.get(id);
		String name = ug.getName();
		// we apply the heuristic of simply disabling principal deletion:
		log.warn("Deletion of "+name+" id: "+id+" requested but skipped.");
	}


}
