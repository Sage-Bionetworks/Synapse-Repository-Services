package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriverImpl;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

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
	// For now we can just create one of these.  We might need to make beans in the future.
	MigrationDriver migrationDriver = new MigrationDriverImpl();


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
	public NodeBackupDriverImpl(NodeBackupManager backupManager) {
		super();
		this.backupManager = backupManager;
	}

	@Override
	public boolean writeBackup(File destination, Progress progress) throws IOException, DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file dose not exist: "
							+ destination.getAbsolutePath());
		if(progress == null) throw new IllegalArgumentException("Progress cannot be null");
		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.setTotalCount(backupManager.getTotalNodeCount());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			// First write the root node as its own entry
			NodeBackup rootBackup = backupManager.getRoot();
			if (rootBackup == null)
				throw new RuntimeException(
						"Cannot create a backup because the root node is null");
			// Recursively write each node.
			writeBackupNode(zos, rootBackup, "", progress);
			zos.close();
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
			String path, Progress progress) throws IOException, NotFoundException, DatastoreException, InterruptedException {
		if (backup == null)
			throw new IllegalArgumentException("NodeBackup cannot be null");
		if (backup.getNode() == null)
			throw new IllegalArgumentException("NodeBackup.node cannot be null");
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		path = path + node.getId() + PATH_DELIMITER;
		ZipEntry entry = new ZipEntry(path + NODE_XML_FILE);
		zos.putNextEntry(entry);
		// Write this node
		NodeSerializerUtil.writeNodeBackup(backup, zos);
		// Now write all revisions of this node.
		writeRevisions(zos, backup, path);
		progress.setMessage(backup.getNode().getName());
		progress.incrementProgress();
		log.info(progress.toString());
		// Check for termination.
		checkForTermination(progress);
		// now write each child
		List<String> childList = backup.getChildren();
		if (childList != null) {
			for (String childId : childList) {
				NodeBackup child = backupManager.getNode(childId);
				writeBackupNode(zos, child, path, progress);
			}
		}
		return true;
	}

	public void checkForTermination(Progress progress)
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
			// First clear all data
			backupManager.clearAllData();
			ZipInputStream zin = new  ZipInputStream(new BufferedInputStream(fis));
			progress.setMessage("Reading: "+source.getAbsolutePath());
			progress.setTotalCount(source.length());
			// We need to map the node type to the node id.
			EntityType nodeType = null;
			ZipEntry entry;
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				checkForTermination(progress);
				// What is the name of this entry
//				log.info("Writing entry: "+entry.getName());
				// Is this a node or a revision?
				if(isNodeBackupFile(entry.getName())){
					// This is a backup file.
					NodeBackup backup = NodeSerializerUtil.readNodeBackup(zin);
					nodeType = EntityType.valueOf(backup.getNode().getNodeType());
					backupManager.createOrUpdateNode(backup);
				}else if(isNodeRevisionFile(entry.getName())){
					// This is a revision file.
					NodeRevisionBackup revision = NodeSerializerUtil.readNodeRevision(zin);
					// Migrate the revision to the current version
					revision = migrationDriver.migrateToCurrentVersion(revision, nodeType);
					backupManager.createOrUpdateRevision(revision);
				}else{
					throw new IllegalArgumentException("Did not recongnize file name: "+entry.getName());
				}
				progress.incrementProgressBy(entry.getCompressedSize());
				log.info(progress.toString());
			}
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
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
}
