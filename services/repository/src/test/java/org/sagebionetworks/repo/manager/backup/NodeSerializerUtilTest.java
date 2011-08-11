package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.util.RandomNodeBackupUtil;
import org.sagebionetworks.repo.model.util.RandomNodeRevisionUtil;

/**
 * Unit test for the NodeSerializerUtil.
 * @author jmhill
 *
 */
public class NodeSerializerUtilTest {

	@Test
	public void testRoundTripNodeBackup(){
		// Create a node backup with all of the values.
		NodeBackup backup = createNodeBackup();
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeBackup(backup, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeBackup clone = NodeSerializerUtil.readNodeBackup(reader);
		assertNotNull(clone);
		assertEquals(backup, clone);
		
	}
	
	@Test
	public void testRoundTripNodeBackupRandom(){
		// Create a node backup with all of the values.
		NodeBackup backup = RandomNodeBackupUtil.generateRandome(443);
		NodeBackup temp = RandomNodeBackupUtil.generateRandome(443);
		assertEquals(backup, temp);
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeBackup(backup, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeBackup clone = NodeSerializerUtil.readNodeBackup(reader);
		assertNotNull(clone);
		assertEquals(backup, clone);
		
	}
	
	@Test
	public void testRoundTripNodeRevision() throws UnsupportedEncodingException{
		// Create a node backup with all of the values.
		NodeRevision rev = new NodeRevision();
		rev.setNodeId("123");
		rev.setComment("I would like to comment!");
		rev.setLabel("0.6.12");
		rev.setModifiedBy("somebody");
		rev.setModifiedOn(new Date());
		rev.setRevisionNumber(new Long(12));
		
		Annotations annos = new Annotations();
		annos.addAnnotation("stringOne", "one");
		annos.addAnnotation("stringOne", "two");
		annos.addAnnotation("stringTwo", "three");
		annos.addAnnotation("dateAnno", new Date());
		annos.addAnnotation("dateAnno3", new Date(3));
		annos.addAnnotation("double", new Double(23.5));
		annos.addAnnotation("long", new Long(1));
		annos.addAnnotation("long3", new Long(4));
		annos.addAnnotation("blob", "Convert this String to a blob please!".getBytes("UTF-8"));
		annos.addAnnotation("blob5", "Convert me too!".getBytes("UTF-8"));
		
		rev.setAnnotations(annos);
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeRevision clone = NodeSerializerUtil.readNodeRevision(reader);
		assertNotNull(clone);
		assertEquals(rev, clone);
		
	}
	
	@Test
	public void testRoundTripNodeRevisionRandom() throws UnsupportedEncodingException{
		// Create a node backup with all of the values.
		NodeRevision rev = RandomNodeRevisionUtil.generateRandom(489, 12);
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeRevision clone = NodeSerializerUtil.readNodeRevision(reader);
		assertNotNull(clone);
		assertEquals(rev, clone);
		
	}

	/**
	 * These files contain old version of the NodeBackup xml.  We must always be able to read these 
	 * old xml files.
	 */
	public static final FileDetails[] PREVIOUS_NODE_BACKUP_FILES = new FileDetails[]{
		new FileDetails("node-backupV0.xml",342 ,54)
	};
	
	@Test
	public void testLoadOldNodeBakupVersions() throws IOException{
		// Make sure we can load all pervious versions of the node backup object
		for(FileDetails previousFile: PREVIOUS_NODE_BACKUP_FILES){
			// Load the node from the file 
			InputStream in = NodeSerializerUtilTest.class.getClassLoader().getResourceAsStream(previousFile.getFileName());
			assertNotNull("Failed to find:"+previousFile.getFileName()+" on the classpath", in);
			try{
				NodeBackup loaded = NodeSerializerUtil.readNodeBackup(in);
				// Now make sure the loaded backup matches the randomly generated object
				NodeBackup  fromSeed = RandomNodeBackupUtil.generateRandome(previousFile.getRandomSeed());
				assertEquals(fromSeed, loaded);
			}finally{
				in.close();
			}
		}
	}
	
	/**
	 * These files contain old version of the NodeBackup xml.  We must always be able to read these 
	 * old xml files.
	 */
	public static final FileDetails[] PREVIOUS_NODE_REVISION_FILES = new FileDetails[]{
		new FileDetails("node-revisionV0.xml",932 ,54)
	};
	
	@Test
	public void testLoadOldNodeRevisionVersions() throws IOException{
		// Make sure we can load all pervious versions of the node backup object
		for(FileDetails previousFile: PREVIOUS_NODE_REVISION_FILES){
			// Load the node from the file 
			InputStream in = NodeSerializerUtilTest.class.getClassLoader().getResourceAsStream(previousFile.getFileName());
			assertNotNull("Failed to find:"+previousFile.getFileName()+" on the classpath", in);
			try{
				NodeRevision loaded = NodeSerializerUtil.readNodeRevision(in);
				// Now make sure the loaded backup matches the randomly generated object
				NodeRevision  fromSeed = RandomNodeRevisionUtil.generateRandom(previousFile.getRandomSeed(), previousFile.getCount());
				assertEquals(fromSeed, loaded);
			}finally{
				in.close();
			}
		}
	}

	/**
	 * Helper to create a node backup with all of the values.
	 * @return
	 */
	private NodeBackup createNodeBackup() {
		Node node = new Node();
		node.setName("NodeSerializerImplTest.roundTrip");
		node.setDescription("This is a description");
		node.setCreatedBy("me");
		node.setCreatedOn(new Date());
		node.setETag("213");
		node.setId("569");
		node.setModifiedBy("you");
		node.setModifiedOn(new Date());
		node.setNodeType(ObjectType.folder.name());
		node.setParentId("13");
		node.setVersionComment("Some version comment");
		node.setVersionLabel("The version label");
		node.setVersionNumber(new Long(7));
		
		List<String> children = new ArrayList<String>();
		children.add(new Long(991).toString());
		children.add(new Long(993).toString());
		
		List<Long> revisions = new ArrayList<Long>();
		revisions.add(new Long(1));
		revisions.add(node.getVersionNumber());
		
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy(node.getCreatedBy());
		acl.setCreationDate(node.getCreatedOn());
		acl.setEtag(node.getETag());
		acl.setId(node.getId());
		acl.setModifiedBy(node.getModifiedBy());
		acl.setModifiedOn(node.getModifiedOn());
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		ra.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		ra.setGroupName("someGroupName");
		acl.getResourceAccess().add(ra);
		ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		// Add all types
		for(ACCESS_TYPE type: ACCESS_TYPE.values()){
			ra.getAccessType().add(type);
		}
		ra.setGroupName("gomerPile");
		acl.getResourceAccess().add(ra);
		
		// Create the backup
		return new NodeBackup(node, acl, node.getId(), revisions, children);
	}
	
	/**
	 * This main method is used to create a blob of the current version of annotations.
	 * Each time we change the annotations object, we should create a new version and add it to the
	 * files to test.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{	
		// There should be three args
		if(args == null || args.length != 3) throw new IllegalArgumentException("This utility requires three arguments: 0=filname, 1=randomSeed, 2=count");
		String name = args[0];
		long seed;
		int count;
		try{
			seed = Long.parseLong(args[1]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The second argument should be a long representing the random seed to use.", e);
		}
		try{
			count = Integer.parseInt(args[2]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The thrid argument should be the number of annotations to used", e);
		}
		NodeBackup backupToSave = null;
		NodeRevision revToSave = null;
		if(name.startsWith("node-backup")){
			backupToSave = RandomNodeBackupUtil.generateRandome(seed);
		}else if(name.startsWith("node-revision")){
			revToSave = RandomNodeRevisionUtil.generateRandom(seed, count);
		}else{
			throw new IllegalArgumentException("The file name for the first argument should either start with 'node-backup' for a NodeBackup or 'node-revision' for a NodeRevision");
		}

		// Now create the output file
		File outputFile = new File("src/test/resources/"+name);
		System.out.println("Creating file: "+outputFile.getAbsolutePath());
		if(outputFile.exists()){
			outputFile.delete();
		}
		outputFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(outputFile);
		try{
			// Write this blob to the file
			BufferedOutputStream buffer = new BufferedOutputStream(fos);
			// First create the blob
			
			if(backupToSave != null){
				NodeSerializerUtil.writeNodeBackup(backupToSave, buffer);
			}else if(revToSave != null){
				NodeSerializerUtil.writeNodeRevision(revToSave, buffer);
			}else{
				throw new Exception("Both backupToSave and revToSave cannot be null");
			}
			System.out.println("Finished building File: "+outputFile.getAbsolutePath());
			buffer.flush();
			fos.flush();
		}finally{
			fos.close();
		}
	}
	
	/**
	 * Simple data structure for holding blob file data.
	 * 
	 * @author jmhill
	 *
	 */
	public static class FileDetails {
		
		String fileName;
		long randomSeed;
		int count;
		
		
		public FileDetails(String fileName, long randomSeed, int count) {
			super();
			this.fileName = fileName;
			this.randomSeed = randomSeed;
			this.count = count;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		public long getRandomSeed() {
			return randomSeed;
		}
		public void setRandomSeed(long randomSeed) {
			this.randomSeed = randomSeed;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		@Override
		public String toString() {
			return "BlobData [fileName=" + fileName + ", randomSeed="
					+ randomSeed + ", count=" + count + "]";
		}
		
	}
}
