package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirementBackup;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
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
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeBackup clone = NodeSerializerUtil.readNodeBackup(reader);
		assertNotNull(clone);
		assertEquals(backup, clone);
		
	}
	
	@Test 
	public void testRoundTripPrincipalBackup() throws Exception {
		Collection<PrincipalBackup> pbs = new HashSet<PrincipalBackup>();
		PrincipalBackup pb = new PrincipalBackup();
		UserGroup ug = new UserGroup();
		ug.setId("101");
		ug.setIsIndividual(true);
		ug.setName("foo");
		ug.setCreationDate(new Date());
		pb.setUserGroup(ug);
		UserProfile up = new UserProfile();
		up.setOwnerId(ug.getId());
		up.setDisplayName("foo bar");
		pb.setUserProfile(up);
		pbs.add(pb);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		NodeSerializerUtil.writePrincipalBackups(pbs, baos);
		baos.close();
		String serialized = new String(baos.toByteArray());
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		Collection<PrincipalBackup> pbs2 = NodeSerializerUtil.readPrincipalBackups(bais);
		assertEquals(pbs, pbs2);
	}

	@Test 
	public void testRoundTripAccessRequirementBackup() throws Exception {
		Collection<PrincipalBackup> pbs = new HashSet<PrincipalBackup>();
		AccessRequirementBackup pb = new AccessRequirementBackup();

		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy("101");
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy("102");
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("103");
		rod.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		accessRequirement.setId(104L);

		TermsOfUseAccessApproval accessApproval = new TermsOfUseAccessApproval();
		accessApproval.setCreatedBy("101");
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy("102");
		accessApproval.setModifiedOn(new Date());
		accessApproval.setEtag("10");
		accessApproval.setAccessorId("100");
		accessApproval.setRequirementId(accessRequirement.getId());
		accessApproval.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		NodeSerializerUtil.writePrincipalBackups(pbs, baos);
		baos.close();
		String serialized = new String(baos.toByteArray());
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		Collection<PrincipalBackup> pbs2 = NodeSerializerUtil.readPrincipalBackups(bais);
		assertEquals(pbs, pbs2);
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
	public void testRoundTripNodeRevision() throws UnsupportedEncodingException {
		
		// Make some references
		Reference layer1 = new Reference();
		layer1.setTargetId("1");
		layer1.setTargetVersionNumber(99L);
		Reference layer2 = new Reference();
		layer2.setTargetId("2");
		Reference layer3 = new Reference();
		layer3.setTargetId("3");
		layer3.setTargetVersionNumber(42L);

		Set<Reference> code = new HashSet<Reference>(); // this one is empty
		Set<Reference> input = new HashSet<Reference>();
		input.add(layer1);
		input.add(layer2);
		Set<Reference> output = new HashSet<Reference>();
		output.add(layer3);
		
		Map<String, Set<Reference>> references = new HashMap<String, Set<Reference>>();
		references.put("code", code);
		references.put("input",input);
		references.put("output", output);
		
		// Create a node backup with all of the values.
		NodeRevisionBackup rev = new NodeRevisionBackup();
		rev.setNodeId("123");
		rev.setComment("I would like to comment!");
		rev.setLabel("0.6.12");
		rev.setModifiedBy("somebody");
		rev.setModifiedOn(new Date());
		rev.setRevisionNumber(new Long(12));
		rev.setReferences(references);
		
		NamedAnnotations named = new NamedAnnotations();
		Annotations annos = named.getAdditionalAnnotations();
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
		
		rev.setNamedAnnotations(named);
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeRevisionBackup clone = NodeSerializerUtil.readNodeRevision(reader);
		assertNotNull(clone);
		assertEquals(rev, clone);
		
	}
	
	@Test
	public void testRoundTripNodeRevisionRandom() throws UnsupportedEncodingException{
		// Create a node backup with all of the values.
		NodeRevisionBackup rev = RandomNodeRevisionUtil.generateRandom(489, 12);
		
		StringWriter writer = new StringWriter();
		NodeSerializerUtil.writeNodeRevision(rev, writer);
//		System.out.println(writer.toString());
		
		// Now read it back
		StringReader reader = new StringReader(writer.toString());
		NodeRevisionBackup clone = NodeSerializerUtil.readNodeRevision(reader);
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
	
	@Ignore // PLFM-651 breaks this test because the random stuff is brittle when you change the number of types in ObjectType
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
		InputStream in = NodeSerializerUtilTest.class.getClassLoader().getResourceAsStream("node-revisionV0.xml");
		assertNotNull("Failed to find:node-revisionV0.xml on the classpath", in);
		try{
			NodeRevisionBackup loaded = NodeSerializerUtil.readNodeRevision(in);
			assertNotNull(loaded);
			// This V0 had annotations
			assertNotNull(loaded.getAnnotations());
		}finally{
			in.close();
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
		node.setCreatedByPrincipalId(1L);
		node.setCreatedOn(new Date());
		node.setETag("213");
		node.setId("569");
		node.setModifiedByPrincipalId(2L);
		node.setModifiedOn(new Date());
		node.setNodeType(EntityType.folder.name());
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
		acl.setCreationDate(node.getCreatedOn());
		acl.setEtag(node.getETag());
		acl.setId(node.getId());
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		ra.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		ra.setPrincipalId(123L);
		acl.getResourceAccess().add(ra);
		ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		// Add all types
		for(ACCESS_TYPE type: ACCESS_TYPE.values()){
			ra.getAccessType().add(type);
		}
		ra.setPrincipalId(456L);
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
		NodeRevisionBackup revToSave = null;
		if(name.startsWith("node-backup")){
			backupToSave = RandomNodeBackupUtil.generateRandome(seed);
		}else if(name.startsWith("node-revision")){
			revToSave = RandomNodeRevisionUtil.generateRandom(seed, count);
		}else{
			throw new IllegalArgumentException("The file name for the first argument should either start with 'node-backup' for a NodeBackup or 'node-revision' for a NodeRevisionBackup");
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
