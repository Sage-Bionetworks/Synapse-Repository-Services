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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
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
				createOrUpdatePrincipals(principalBackups);
				
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
	
	private void createOrUpdatePrincipals(Collection<PrincipalBackup> principalBackups) {
		try {
		    ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
			for (PrincipalBackup pb : principalBackups) {
				String id = pb.getUserGroup().getId();
				boolean isIndividual = pb.getUserGroup().getIsIndividual();
				if (isIndividual && pb.getUserProfile()==null) throw new IllegalArgumentException("No user profile for individual group "+id);
				if (!isIndividual && pb.getUserProfile()!=null) throw new IllegalArgumentException("Unexpected user profile for multi-user group "+id+" "+pb.getUserProfile().getDisplayName());
				boolean exists = false;
				UserGroup dstUserGroup = null;
				try {
					dstUserGroup = userGroupDAO.get(id);
					exists = true;
				} catch (NotFoundException nfe) {
					exists = false;
				}
				UserGroup srcUserGroup = pb.getUserGroup();
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

}
