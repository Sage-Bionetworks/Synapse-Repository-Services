package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

import com.google.common.collect.Lists;

/**
 * Test to convert from JDO to DTO
 * @author jmhill
 *
 */
public class NodeUtilsTest {
	
	private Long createdById;
	
	@Before 
	public void before() {
		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}

	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException {
		Node node = new Node();
		node.setName("myName");
		node.setId(KeyFactory.keyToString(101L));
		// This is an update round trip and the type cannot be changed on an update.
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedByPrincipalId(createdById);
		node.setETag("1013");
		node.setCreatedOn(new Date(System.currentTimeMillis()+99));
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		// Set the version information
		node.setVersionComment("This is the first version of this object");
		node.setVersionLabel("1.0.1");
		node.setReference(new Reference());
		node.setActivityId("1234");
		node.setFileHandleId("9999888777");
		List<String> columnIds = new LinkedList<String>();
		columnIds.add("2");
		columnIds.add("1");
		node.setColumnModelIds(columnIds);
		node.setScopeIds(Lists.newArrayList("8","9"));
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		DBORevision jdoRev = new DBORevision();
		NodeUtils.updateFromDto(node, jdoNode, jdoRev, false);
		assertEquals("The user cannot change an eTag.", null, jdoNode.getEtag());
		// Set it to make sure the copy works
		jdoNode.seteTag("1013");
		
		// Make a copy form the jdo
		Node copy = NodeUtils.copyFromJDO(jdoNode, jdoRev);
		assertNotNull(copy);
		assertNotNull(copy.getFileHandleId());
		// It should match
		assertEquals(node, copy);
	}
	
	@Test
	public void testExcessivelyLongComment() throws DatastoreException, InvalidModelException {
		Node node = new Node();
		node.setModifiedByPrincipalId(createdById);
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<DBORevision.MAX_COMMENT_LENGTH+1; i++) sb.append(" ");
		node.setVersionComment(sb.toString());
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		
		DBORevision jdoRev = new DBORevision();
		try {
			NodeUtils.updateFromDto(node, jdoNode, jdoRev, false);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testJDOParentId() throws DatastoreException{
		DBONode parent = new DBONode();
		parent.setId(new Long(123));
		DBONode child = new DBONode();
		child.setName("name");
		child.setParentId(parent.getId());
		child.setCreatedOn(System.currentTimeMillis());
		child.setCreatedBy(createdById);
		// Make sure the parent id goes to the child
		DBORevision rev = new DBORevision();
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setRevisionNumber(new Long(21));
		Node dto = NodeUtils.copyFromJDO(child, rev);
		assertNotNull(dto);
		assertEquals(KeyFactory.keyToString(parent.getId()), dto.getParentId());
		assertEquals(new Long(21), dto.getVersionNumber());
	}

	@Test
	public void testJdoIsLatestVersion() throws DatastoreException {
		long currentRevisionNumber = 5L;

		DBONode dbo = new DBONode();
		dbo.setId(123L);
		dbo.setCreatedOn(System.currentTimeMillis());
		dbo.setCreatedBy(createdById);
		dbo.setCurrentRevNumber(currentRevisionNumber);

		DBORevision rev = new DBORevision();
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setRevisionNumber(currentRevisionNumber);

		// Call under test - positive
		Node dto = NodeUtils.copyFromJDO(dbo, rev);
		assertNotNull(dto);
		assertTrue(dto.getIsLatestVersion());

		rev.setRevisionNumber(currentRevisionNumber - 1);
		// Call under test - negative
		dto = NodeUtils.copyFromJDO(dbo, rev);
		assertNotNull(dto);
		assertFalse(dto.getIsLatestVersion());
	}

	@Test
	public void nullNode() {
		assertFalse(NodeUtils.isValidNode(null));
	}
	
	@Test
	public void validNode() {
		Node node = createValidNode();
		assertTrue(NodeUtils.isValidNode(node));
	}
		
	private Node createValidNode() {
		Node node = new Node();
		node.setCreatedByPrincipalId(123L);
		node.setCreatedOn(new Date());
		node.setETag("etag");
		node.setId("456");
		node.setModifiedByPrincipalId(123L);
		node.setModifiedOn(new Date());
		node.setName("new node");
		node.setNodeType(EntityType.folder);
		return node;
	}
	
	@Test
	public void testNodeWithNullName(){
		Node node = createValidNode();
		node.setName(null);
		assertFalse(NodeUtils.isValidNode(node));
	}

	@Test
	public void testNodeWithCreatedByNull(){
		Node node = createValidNode();
		node.setCreatedByPrincipalId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithCreatedOnNull(){
		Node node = createValidNode();
		node.setCreatedOn(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithModifiedByNull(){
		Node node = createValidNode();
		node.setModifiedByPrincipalId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithModifiedOnNull(){
		Node node = createValidNode();
		node.setModifiedOn(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithETagNull(){
		Node node = createValidNode();
		node.setETag(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithIdNull(){
		Node node = createValidNode();
		node.setId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithNodeTypeNull(){
		Node node = createValidNode();
		node.setNodeType(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	
	@Test
	public void testIdListToBytesAndBytesToIdList(){
		List<String> idList = Lists.newArrayList("syn123", "456");
		byte[] bytes = NodeUtils.createByteForIdList(idList);
		List<String> results = NodeUtils.createIdListFromBytes(bytes);
		List<String> expected = Lists.newArrayList("123","456");
		assertEquals(expected, results);
	}
	
	@Test
	public void testIdListToBytesAndBytesToIdListEmpty(){
		List<String> idList = Lists.newArrayList();
		byte[] bytes = NodeUtils.createByteForIdList(idList);
		List<String> results = NodeUtils.createIdListFromBytes(bytes);
		List<String> expected = Lists.newArrayList();
		assertEquals(expected, results);
	}
	
	@Test
	public void testIsProjectOrFolder(){
		for(EntityType type: EntityType.values()){
			if(EntityType.project.equals(type)){
				assertTrue(NodeUtils.isProjectOrFolder(type));
			}else if(EntityType.folder.equals(type)){
				assertTrue(NodeUtils.isProjectOrFolder(type));
			}else{
				assertFalse(NodeUtils.isProjectOrFolder(type));
			}
		}
	}
	
	@Test
	public void testIsRootEntityId(){
		String rootId = StackConfigurationSingleton.singleton().getRootFolderEntityId();
		assertTrue(NodeUtils.isRootEntityId(rootId));
		assertFalse(NodeUtils.isRootEntityId(rootId+"1"));
		Long rootLong = KeyFactory.stringToKey(rootId);
		assertTrue(NodeUtils.isRootEntityId(""+rootLong));
		assertFalse(NodeUtils.isRootEntityId(""+rootLong+1));
	}
	
	@Test
	public void testIsRootEntityIdWithSyn(){
		String rootId = StackConfigurationSingleton.singleton().getRootFolderEntityId();
		String rootWithSyn = KeyFactory.keyToString(KeyFactory.stringToKey(rootId));
		assertTrue(NodeUtils.isRootEntityId(rootWithSyn));
	}
	
	@Test
	public void testTranslateAlias() {
		assertEquals(null, NodeUtils.translateAlias(null));
		assertEquals(null, NodeUtils.translateAlias(""));
		assertEquals("anAlias", NodeUtils.translateAlias("anAlias"));
	}
	
	@Test
	public void testTranslateActivityId() {
		assertEquals(null, NodeUtils.translateActivityId(null));
		assertEquals(null, NodeUtils.translateActivityId("-1"));
		assertEquals(new Long(123), NodeUtils.translateActivityId("123"));
	}
	
	@Test
	public void testTranslateNodeId() {
		assertEquals(null, NodeUtils.translateNodeId(null));
		assertEquals(new Long(123), NodeUtils.translateNodeId("syn123"));
		assertEquals(new Long(456), NodeUtils.translateNodeId("456"));
	}
	
	@Test
	public void testTranslateFileHandleId() {
		assertEquals(null, NodeUtils.translateFileHandleId(null));
		assertEquals(new Long(123), NodeUtils.translateFileHandleId("123"));
	}
	
	@Test
	public void testTranslateComment() {
		String comment = createStringOfSize(DBORevision.MAX_COMMENT_LENGTH);
		// call under test
		assertEquals(comment, NodeUtils.translateVersionComment(comment));
	}
	
	@Test
	public void testTranslateCommentNull() {
		// call under test
		assertEquals(null, NodeUtils.translateVersionComment(null));
	}
	
	@Test
	public void testTranslateCommentOverLimit() {
		String comment = createStringOfSize(DBORevision.MAX_COMMENT_LENGTH+1);
		try {
			// call under test
			NodeUtils.translateVersionComment(comment);
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(""+DBORevision.MAX_COMMENT_LENGTH));
		}
	}
	
	@Test
	public void testTranslateVersionLabel() {
		assertEquals(NodeConstants.DEFAULT_VERSION_LABEL, NodeUtils.translateVersionLabel(null));
		assertEquals("aLabel", NodeUtils.translateVersionLabel("aLabel"));
	}
	
	@Test
	public void testTranlateVersionNumber() {
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, NodeUtils.translateVersionNumber(null));
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, NodeUtils.translateVersionNumber(-1L));
		assertEquals(new Long(123), NodeUtils.translateVersionNumber(123L));
	}
	
	@Test
	public void testTranslateNodeToDBONode() {
		Node dto = createDefaultNode();
		// call under test
		DBONode dbo = NodeUtils.translateNodeToDBONode(dto);
		assertNotNull(dbo);
		assertEquals(null, dbo.getAlias());
		assertEquals(dto.getName(), dbo.getName());
		assertEquals(dto.getCreatedByPrincipalId(), dbo.getCreatedBy());
		assertEquals(new Long(dto.getCreatedOn().getTime()), dbo.getCreatedOn());
		assertEquals(dto.getVersionNumber(), dbo.getCurrentRevNumber());
		assertEquals(dto.getETag(), dbo.geteTag());
		assertEquals(new Long(123), dbo.getId());
		assertEquals(new Long(456), dbo.getParentId());
		assertEquals(EntityType.project.name(), dbo.getType());
	}
	
	@Test
	public void testTransalteNodeToDBORevision() {
		Node dto = createDefaultNode();
		// call under test
		DBORevision dbo = NodeUtils.transalteNodeToDBORevision(dto);
		assertNotNull(dbo);
		assertEquals(null, dbo.getActivityId());
		assertNotNull(dbo.getColumnModelIds());
		assertNotNull(dbo.getScopeIds());
		assertEquals(new Long(8888),dbo.getFileHandleId());
		assertEquals(dto.getModifiedByPrincipalId(), dbo.getModifiedBy());
		assertEquals(new Long(dto.getModifiedOn().getTime()), dbo.getModifiedOn());
		assertNotNull(dbo.getScopeIds());
		assertEquals(NodeConstants.DEFAULT_VERSION_LABEL, dbo.getLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, dbo.getRevisionNumber());
		assertEquals(dto.getVersionComment(), dbo.getComment());
		assertEquals(new Long(123), dbo.getOwner());
		assertNotNull(dbo.getReference());
	}

	@Test
	public void testIsBucketSynapseStorage() {
		String synapseBucket = StackConfigurationSingleton.singleton().getS3Bucket();
		assertTrue(NodeUtils.isBucketSynapseStorage(synapseBucket));

		String notSynapseBucket = "s3://my.personal.bucket";
		assertFalse(NodeUtils.isBucketSynapseStorage(notSynapseBucket));

		assertNull(NodeUtils.isBucketSynapseStorage(null));
	}
	
	Node createDefaultNode() {
		Node node = new Node();
		node.setNodeType(EntityType.project);
		node.setActivityId("-1");
		node.setAlias("");
		node.setColumnModelIds(Lists.newArrayList("111","222"));
		node.setScopeIds(Lists.newArrayList(Lists.newArrayList("333","444")));
		node.setCreatedByPrincipalId(11L);
		node.setCreatedOn(new Date(555L));
		node.setETag("etag");
		node.setFileHandleId("9999");
		node.setId("syn123");
		node.setModifiedByPrincipalId(22L);
		node.setModifiedOn(new Date(777L));
		node.setName("aName");
		node.setParentId("syn456");
		node.setFileHandleId("8888");
		Reference reference = new Reference();
		reference.setTargetId("syn888");
		reference.setTargetVersionNumber(4L);
		node.setReference(reference);
		node.setVersionComment("aComment");
		return node;
	}


	/**
	 * Helper to create a string of a given size
	 * @param size
	 * @return
	 */
	String createStringOfSize(int size) {
		char[] chars = new char[size];
		for(int i=0; i<size; i++) {
			chars[i] = 'a';
		}
		return new String(chars);
	}
}
