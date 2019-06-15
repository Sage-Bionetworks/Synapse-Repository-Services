package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;

public class NodeTranslationUtilsTest {

	@Test
	public void testBinaryRoundTripString(){
		ObjectSchema schema = new ObjectSchema(TYPE.STRING);
		String longString = "This string is so long we must store it as a blob";
		byte[] bytes = NodeTranslationUtils.objectToBytes(longString, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		String back = (String) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(longString, back);
	}
	
	@Test
	public void testBinaryRoundTripJSONEntity(){
		Project example = new Project();
		example.setName("Example name");
		ObjectSchema schema = SchemaCache.getSchema(example);
		byte[] bytes = NodeTranslationUtils.objectToBytes(example, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		Project back = (Project) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(example, back);
	}
	
	@Test
	public void testBinaryRoundTripListJSONEntity(){
		ObjectSchema schema = new ObjectSchema();
		schema.setType(TYPE.ARRAY);
		schema.setItems(SchemaCache.getSchema(new Project()));
		
		Project example = new Project();
		example.setName("Example name");
		List<Project> list = new ArrayList<Project>();
		list.add(example);
		// Add one more
		example = new Project();
		example.setName("Example 2");
		list.add(example);
		
		byte[] bytes = NodeTranslationUtils.objectToBytes(list, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		List<Project> back = (List<Project>) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(list, back);
	}
	
	@Test
	public void testBinaryRoundTripSetJSONEntity(){
		ObjectSchema schema = new ObjectSchema();
		schema.setType(TYPE.ARRAY);
		schema.setItems(SchemaCache.getSchema(new Project()));
		schema.setUniqueItems(true);
		
		Project example = new Project();
		example.setName("Example name");
		Set<Project> set = new HashSet<Project>();
		set.add(example);
		// Add one more
		example = new Project();
		example.setName("Example 2");
		set.add(example);
		
		byte[] bytes = NodeTranslationUtils.objectToBytes(set, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		Set<Project> back = (Set<Project>) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(set, back);
	}


	/**
	 * Assert two Entity objects are equal while ignoring transient fields.
	 * @param <T>
	 * @param one
	 * @param two
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static <T extends Entity> void assertEqualsNonTransient(T one, T two, ObjectSchema schema) throws IllegalArgumentException, IllegalAccessException{
		assertNotNull(one);
		assertNotNull(two);
		assertEquals(one.getClass(), two.getClass());
		// Check the fields
		Field[] oneFields = one.getClass().getDeclaredFields();
		for(int i=0; i<oneFields.length;i++){
			Field field = oneFields[i];
			field.setAccessible(true);
			ObjectSchema propSchema = schema.getProperties().get(field.getName());
			if(propSchema == null){
				continue;
			}
			// Only compare non-transient fields
			if(!propSchema.isTransient()){
				assertEquals(field.get(one), field.get(two), "Name: "+field.getName());
			}
		}
	}
	

	@Test
	public void testLayerPreviewRoundTrip() throws Exception{
		Preview preview = new Preview();
		preview.setPreviewString("Pretend this a very long string and needs to be stored as a blob");
		// Create a clone using node translation
		Preview clone = cloneUsingNodeTranslation(preview);
		// Now our clone should match the original layer.
		System.out.println("Original: "+preview.toString());
		System.out.println("Clone: "+clone.toString());
		assertEquals(preview, clone);
	}
	
	@Test
	public void testSetNullOnNode(){
		Project project = new Project();
		project.setParentId("90");
		Node copy = new Node();
		NodeTranslationUtils.updateNodeFromObject(project, copy);
		assertTrue(copy.getParentId() != null);
		// Now clear the node parent id
		project.setParentId(null);
		NodeTranslationUtils.updateNodeFromObject(project, copy);
		// The copy should have a null parent id.
		assertTrue(copy.getParentId() == null);
	}
	
	@Test
	public void testVersionableRoundTrip() throws InstantiationException, IllegalAccessException{
		FileEntity code = new FileEntity();
		code.setVersionComment("version comment");
		code.setVersionNumber(new Long(134));
		code.setVersionLabel("1.133.0");
		code.setName("mame");
		FileEntity clone = cloneUsingNodeTranslation(code);
		assertEquals(code, clone);
	}

	/**
	 * Will clone an object by first creating a node and annotations for the passed object.
	 * A new object will then be created and populated using the node and annotations.
	 * @param <T>
	 * @param toClone
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static <T extends Entity> T cloneUsingNodeTranslation(T toClone) throws InstantiationException, IllegalAccessException{
		// Create a node using this the object
		Node dsNode = NodeTranslationUtils.createFromEntity(toClone);
		// Update an annotations object using the object
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(toClone, annos);
		// Now crate the clone
		@SuppressWarnings("unchecked")
		T clone = (T) toClone.getClass().newInstance();
		// first apply the annotations
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(clone, annos);
		// then apply the node
		// Apply the node
		NodeTranslationUtils.updateObjectFromNode(clone, dsNode);
		return clone;
	}
	
	@Test
	public void testLinkTrip() throws InstantiationException, IllegalAccessException{
		Link link = new Link();
		Reference ref = new Reference();
		ref.setTargetId("123");
		link.setLinksTo(ref);
		link.setLinksToClassName(Folder.class.getName());
		Node node = NodeTranslationUtils.createFromEntity(link);
		// Set the type for this object
		node.setNodeType(EntityType.link);
		NamedAnnotations annos = new NamedAnnotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(link, annos.getPrimaryAnnotations());
		assertNotNull(node.getReference());
		assertEquals(ref, node.getReference());
		// Make sure we can make the round trip
		Link newLink = new Link();
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newLink, annos.getPrimaryAnnotations());
		NodeTranslationUtils.updateObjectFromNode(newLink, node);
		assertNotNull(newLink.getLinksTo());
		assertEquals("123", newLink.getLinksTo().getTargetId());
	}

	/**
	 * We must be able to clear values by passing null
	 */
	@Test
	public void testPLFM_1214(){
		// Start with a study with a
		ExampleEntity example = new ExampleEntity();
		example.setSingleInteger(123l);
		Node node = NodeTranslationUtils.createFromEntity(example);
		NamedAnnotations annos = new NamedAnnotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(example, annos.getPrimaryAnnotations());
		Annotations primaryAnnos = annos.getPrimaryAnnotations();
		// First make sure it is set correctly.
		assertNotNull(primaryAnnos.getSingleValue("singleInteger"));
		Long value = (Long) primaryAnnos.getSingleValue("singleInteger");
		assertEquals(new Long(123),value);
		
		// Now the second update we want to clear it out.
		example.setSingleInteger(null);
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(example, annos.getPrimaryAnnotations());
		// The value should now be cleared out.
		assertEquals(null, primaryAnnos.getSingleValue("singleInteger"));

	}
	
	@Test
	public void testFileEntityRoundTrip() throws InstantiationException, IllegalAccessException{
		String fileHandle = "12345";
		FileEntity file = new FileEntity();
		file.setDataFileHandleId(fileHandle);
		FileEntity clone = cloneUsingNodeTranslation(file);
		assertEquals(file, clone);
	}
	
	@Test
	public void testTableEntityRoundTrip() throws InstantiationException, IllegalAccessException{
		TableEntity table = new TableEntity();
		table.setName("oneTwoThree");
		List<String> columns = new LinkedList<String>();
		columns.add("123");
		columns.add("456");
		table.setColumnIds(columns);
		
		TableEntity clone = cloneUsingNodeTranslation(table);
		assertNotNull(clone);
		assertEquals(table, clone);
	}
	
	@Test
	public void testTableEntityRoundTripOneColumn() throws InstantiationException, IllegalAccessException{
		TableEntity table = new TableEntity();
		table.setName("oneTwoThree");
		List<String> columns = new LinkedList<String>();
		columns.add("123");
		table.setColumnIds(columns);
		
		TableEntity clone = cloneUsingNodeTranslation(table);
		assertNotNull(clone);
		assertEquals(table, clone);
	}
	
	@Test
	public void testFileViewSetAmdGet() throws InstantiationException, IllegalAccessException{
		EntityView view = new EntityView();
		view.setName("viewName");
		view.setType(ViewType.file);
		view.setColumnIds(Lists.newArrayList("1","2"));
		view.setScopeIds(Lists.newArrayList("syn4","5"));
		
		EntityView clone = cloneUsingNodeTranslation(view);
		assertNotNull(clone);
		assertEquals(view, clone);
	}

	@Test
	public void testUpdateNodeSecondaryFieldsFromObject_ConcreteTypeNotInAnnotations(){
		Annotations annotations = new Annotations();
		FileEntity fileEntity = new FileEntity();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(fileEntity, annotations);
		assertNull(annotations.getAllValues(ObjectSchema.CONCRETE_TYPE));
	}


	@Test
	public void testGetTranslatableEntityFields(){
		TableEntity entity = new TableEntity();
		Field[] translatableFields = NodeTranslationUtils.getTranslatableEntityFields(entity);

		assertTrue(Arrays.stream(translatableFields).noneMatch( field -> ObjectSchema.CONCRETE_TYPE.equals(field.getName())) );
	}
}
