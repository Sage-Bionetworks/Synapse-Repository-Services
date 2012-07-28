package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;

/**
 * This class drives the backup and restoration process.
 * 
 * @author jmhill
 *
 */
public class NodeBackupDriverImpl implements NodeBackupDriver {

	private static final String REVISIONS_FOLDER = "revisions";

	private static final String XML_FILE_SUFFIX = ".xml";

	private static final String PATH_DELIMITER = "/";

	private static final String NODE_XML_FILE = "node.xml";

	static private Log log = LogFactory.getLog(NodeBackupDriverImpl.class);

	@Autowired
	NodeBackupManager backupManager;
	@Autowired
	NodeSerializer nodeSerializer;
	@Autowired
	MigrationDriver migrationDriver;


	/**
	 * Used by Spring
	 */
	public NodeBackupDriverImpl() {
		
	}
	
	/**
	 * Used by unit tests.
	 * 
	 * @param nodeSource
	 */
	public NodeBackupDriverImpl(NodeBackupManager backupManager,MigrationDriver migrationDriver ) {
		super();
		this.backupManager = backupManager;
		this.nodeSerializer = new NodeSerializerImpl();
		this.migrationDriver = migrationDriver;
	}

	@Override
	public boolean writeBackup(File destination, Progress progress, Set<String> entitiesToBackup) throws IOException, DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file dose not exist: "
							+ destination.getAbsolutePath());
		if(progress == null) throw new IllegalArgumentException("Progress cannot be null");
		// If the entitiesToBackup is null then include the root
		List<String> listToBackup = new ArrayList<String>();
		boolean isRecursive = false;
		if(entitiesToBackup == null){
			// Just add the root
			isRecursive = true;
			listToBackup.add(backupManager.getRootId());
		}else{
			// Add all of the entites from the set.
			isRecursive = false;
			listToBackup.addAll(entitiesToBackup);
		}
		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a backup to file: " + destination.getAbsolutePath());
		progress.setTotalCount(backupManager.getTotalNodeCount());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing nodes:");
			// First write the root node as its own entry
			for(String idToBackup: listToBackup){
				// Recursively write each node.
				progress.appendLog(idToBackup);
				NodeBackup backup = backupManager.getNode(idToBackup);
				if(backup == null) throw new IllegalArgumentException("Cannot backup node: "+idToBackup+" because it does not exists");
				writeBackupNode(zos, backup, "", progress, isRecursive);
			}
			zos.close();
			progress.appendLog("Finished processing nodes.");
		} finally {
			if (fos != null) {
				fos.flush();
				fos.close();
			}
		}
		return true;
	}

	/**
	 * This is a recursive method that will write the full tree of node data to
	 * the zip file.
	 * 
	 * @param zos
	 * @param backup
	 * @param path
	 * @throws IOException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws InterruptedException 
	 */
	private boolean writeBackupNode(ZipOutputStream zos, NodeBackup backup,
			String path, Progress progress, boolean isRecurisive) throws IOException, NotFoundException, DatastoreException, InterruptedException {
		if (backup == null)
			throw new IllegalArgumentException("NodeBackup cannot be null");
		if (backup.getNode() == null)
			throw new IllegalArgumentException("NodeBackup.node cannot be null");
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		// Since this could be called in a tight loop, we need to be
		// CPU friendly
		Thread.yield();
		path = path + node.getId() + PATH_DELIMITER;
		ZipEntry entry = new ZipEntry(path + NODE_XML_FILE);
		zos.putNextEntry(entry);
		// Write this node
		NodeSerializerUtil.writeNodeBackup(backup, zos);
		// Now write all revisions of this node.
		writeRevisions(zos, backup, path);
		progress.setMessage(backup.getNode().getName());
		progress.incrementProgress();
		if(log.isTraceEnabled()){
			log.trace(progress.toString());			
		}
		// Check for termination.
		checkForTermination(progress);
		if(isRecurisive){
			// now write each child
			List<String> childList = backup.getChildren();
			if (childList != null) {
				for (String childId : childList) {
					NodeBackup child = backupManager.getNode(childId);
					writeBackupNode(zos, child, path, progress, isRecurisive);
				}
			}			
		}
		return true;
	}

	public static void checkForTermination(Progress progress)
			throws InterruptedException {
		// Between each node check to see if we should terminate
		if(progress.shouldTerminate()){
			throw new InterruptedException("Backup terminated by the user");
		}
	}

	/**
	 * Add a single revision
	 * 
	 * @param zos
	 * @param backup
	 * @param path
	 * @throws IOException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	private void writeRevisions(ZipOutputStream zos, NodeBackup backup,
			String path) throws IOException, NotFoundException, DatastoreException {
		if (backup == null)
			throw new IllegalArgumentException("NodeBackup cannot be null");
		if (backup.getNode() == null)
			throw new IllegalArgumentException("NodeBackup.node cannot be null");
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		List<Long> revList = backup.getRevisions();
		if (revList != null) {
			for (Long revId : revList) {
				ZipEntry entry = new ZipEntry(path + REVISIONS_FOLDER
						+ PATH_DELIMITER + revId + XML_FILE_SUFFIX);
				zos.putNextEntry(entry);
				NodeRevisionBackup rev = backupManager.getNodeRevision(node.getId(),	revId);
				if (rev == null)
					throw new RuntimeException(
							"Cannot find a revision for node.id: "
									+ node.getId() + " revId:" + revId);
				if(!NodeRevisionBackup.CURRENT_XML_VERSION.equals(rev.getXmlVersion())){
					throw new RuntimeException("Cannot write a NodeRevisionBackup that is not set to the current xml version.  Expected version: "+NodeRevisionBackup.CURRENT_XML_VERSION+" but was "+rev.getXmlVersion());
				}
				NodeSerializerUtil.writeNodeRevision(rev, zos);
			}
		}
	}

	/**
	 * Restore from the backup.
	 * @throws InterruptedException 
	 */
	@Override
	public boolean restoreFromBackup(File source, Progress progress) throws IOException, InterruptedException {
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
			// We need to map the node type to the node id.
			EntityType nodeType = null;
			NodeBackup backup = null;
			List<NodeRevisionBackup> revisions = null;
			ZipEntry entry;
			progress.appendLog("Processing nodes:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				checkForTermination(progress);
				// What is the name of this entry
//				log.info("Writing entry: "+entry.getName());
				// Is this a node or a revision?
				if(isNodeBackupFile(entry.getName())){
					// Push the current data
					if(backup != null){
						createOrUpdateNodeWithRevisions(backup, revisions);
						// clear the current data
						backup = null;
						
					}
					// This is a backup file.
					backup = nodeSerializer.readNodeBackup(zin);
					// Append this id to the log.
					progress.appendLog(backup.getNode().getId());
					revisions = new ArrayList<NodeRevisionBackup>();
					try{
						nodeType = EntityType.valueOf(backup.getNode().getNodeType());
					}catch(IllegalArgumentException e){
						// This was likely a deleted entity type.
						nodeType = EntityType.unknown;
						backup = null;
						// for now skip unknown types
						continue;
					}
					
					migrationDriver.migrateNodePrincipals(backup);
					
					// Are we restoring the root node?
					if(backup.getNode().getParentId() == null){
						// This node is a root.  Does it match the current root?
						String currentRootId = getCurrentRootId();
						if(!backup.getNode().getId().equals(currentRootId)){
							// We are being asked to restore a root node but we already have one.
							// Since the current root does not match the ID of the root we were given
							// we must clear all data and start with a clean database
							backupManager.clearAllData();
						}
					}
				}else if(isNodeRevisionFile(entry.getName())){
					// Skip unknown types.
					if(EntityType.unknown == nodeType) continue;
					if(backup == null) throw new IllegalArgumentException("Found a revsions without a matching entity.");
					if(revisions == null) throw new IllegalArgumentException("Found a revisoin without any matching entity");

					// This is a revision file.
					NodeRevisionBackup revision = NodeSerializerUtil.readNodeRevision(zin);
					// Add this to the list
					// Migrate the revision to the current version
					nodeType = migrationDriver.migrateToCurrentVersion(revision, nodeType);
					// nodeType is changed as needed
					backup.getNode().setNodeType(nodeType.name());
					// Add this to the list of revisions to be processed
					revisions.add(revision);
				}else{
					throw new IllegalArgumentException("Did not recongnize file name: "+entry.getName());
				}
				progress.incrementProgressBy(entry.getCompressedSize());
				if(log.isTraceEnabled()){
					log.trace(progress.toString());			
				}
				// This is run in a tight loop so to be CPU friendly we should yield
				
				Thread.yield();
			}
			progress.appendLog("Finished processing nodes.");
			if(backup != null){
				// do the final backup
				createOrUpdateNodeWithRevisions(backup, revisions);
			}
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}
	
	/**
	 * Get the ID of the current root node.
	 * @return
	 */
	String getCurrentRootId(){
		try {
			NodeBackup currentRoot = backupManager.getRoot();
			return currentRoot.getNode().getId();
		} catch (DatastoreException e) {
			// There is something wrong
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			// This just means the current root does not exist
			return null;
		}
	}
	
	/**
	 * Is this a node file.
	 * @param name
	 * @return
	 */
	public static boolean isNodeBackupFile(String name){
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		return name.endsWith(NODE_XML_FILE);
	}
	
	/**
	 * Is this a revision file
	 * @param name
	 * @return
	 */
	public static boolean isNodeRevisionFile(String name){
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		int index = name.indexOf(REVISIONS_FOLDER);
		return index >= 0;
	}
	
	/**
	 * Create or update the node with deadlock detection.
	 * @param backup
	 * @param revisions
	 * @throws InterruptedException
	 */
	void createOrUpdateNodeWithRevisions(NodeBackup backup, List<NodeRevisionBackup> revisions) throws InterruptedException{
		// This can deadlock (see PLFM-1341)
		try{
			backupManager.createOrUpdateNodeWithRevisions(backup, revisions);
		}catch (DeadlockLoserDataAccessException e){
			// Try again
			Thread.sleep(100);
			//Try once more.  If it fails again then the exception is thrown.
			backupManager.createOrUpdateNodeWithRevisions(backup, revisions);
		}
	}
}
