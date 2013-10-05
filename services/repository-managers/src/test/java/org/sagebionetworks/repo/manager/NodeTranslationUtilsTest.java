package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.manager.backup.SerializationUseCases;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.sample.Example;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;

public class NodeTranslationUtilsTest {
	
	@Test
	public void testGetNameFromEnum(){
		LocationTypeNames names =LocationTypeNames.awsebs;
		// Get the string value
		String name = NodeTranslationUtils.getNameFromEnum(names);
		assertNotNull(name);
		assertEquals(names.name(), name);;
	}
	
	@Test
	public void testGetEnumValueFromString(){
		LocationTypeNames names =LocationTypeNames.sage;
		// Get the string value
		LocationTypeNames enumValue = (LocationTypeNames) NodeTranslationUtils.getValueOfFromEnum(names.name(), LocationTypeNames.class);
		assertNotNull(enumValue);
		assertEquals(names, enumValue);;
	}
	
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
		Example example = new Example();
		example.setName("Example name");
		example.setType("The best type ever");
		example.setQuantifier("Totally not quantifyable!");
		ObjectSchema schema = SchemaCache.getSchema(example);
		byte[] bytes = NodeTranslationUtils.objectToBytes(example, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		Example back = (Example) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(example, back);
	}
	
	@Test
	public void testBinaryRoundTripListJSONEntity(){
		ObjectSchema schema = new ObjectSchema();
		schema.setType(TYPE.ARRAY);
		schema.setItems(SchemaCache.getSchema(new Example()));
		schema.setUniqueItems(false);
		
		Example example = new Example();
		example.setName("Example name");
		example.setType("The best type ever");
		example.setQuantifier("Totally not quantifyable!");
		List<Example> list = new ArrayList<Example>();
		list.add(example);
		// Add one more
		example = new Example();
		example.setName("Example 2");
		example.setType("The best type ever 2");
		example.setQuantifier("Totally not quantifyable 2!");
		list.add(example);
		
		byte[] bytes = NodeTranslationUtils.objectToBytes(list, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		List<Example> back = (List<Example>) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(list, back);
	}
	
	@Test
	public void testBinaryRoundTripSetJSONEntity(){
		ObjectSchema schema = new ObjectSchema();
		schema.setType(TYPE.ARRAY);
		schema.setItems(SchemaCache.getSchema(new Example()));
		schema.setUniqueItems(true);
		
		Example example = new Example();
		example.setName("Example name");
		example.setType("The best type ever");
		example.setQuantifier("Totally not quantifyable!");
		Set<Example> set = new HashSet<Example>();
		set.add(example);
		// Add one more
		example = new Example();
		example.setName("Example 2");
		example.setType("The best type ever 2");
		example.setQuantifier("Totally not quantifyable 2!");
		set.add(example);
		
		byte[] bytes = NodeTranslationUtils.objectToBytes(set, schema);
		assertNotNull(bytes);
		// Now convert it back to a string
		Set<Example> back = (Set<Example>) NodeTranslationUtils.bytesToObject(bytes, schema);
		assertNotNull(back);
		assertEquals(set, back);
	}

	@Test
	public void testDatasetRoundTrip1() throws InstantiationException, IllegalAccessException{
		// First we create a dataset with all fields filled in.
		Study ds = new Study();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreatedBy("magic");
		ds.setCreatedOn(new Date(System.currentTimeMillis()));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setUri("someUri");
		ds.setContentType("text/xml");
		ds.setMd5("8b12c8b413504cf19889efca5605a6c9");
		// Add some location data
		List<LocationData> locations = new ArrayList<LocationData>();
		ds.setLocations(locations);
		LocationData ldata = new LocationData();
		ldata.setPath("http://my.home.com:8990/wow");
		ldata.setType(LocationTypeNames.sage);
		locations.add(ldata);
		
//		List<String> diseases = new ArrayList<String>();
//		diseases.add("disease1");
//		diseases.add("disease2");
//		ds.setDiseases(diseases);
		
//		List<String> sampleTypes = new ArrayList<String>();
//		sampleTypes.add("normalTissue");
//		sampleTypes.add("tumorTissue");
//		ds.setSampleType(sampleTypes);
		
		// Create a clone using node translation
		Study clone = cloneUsingNodeTranslation(ds);
		
		// Now our clone should match the original dataset.
		System.out.println("Original: "+ds.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(ds, clone, SchemaCache.getSchema(ds));
	}
	
//	@Test
//	public void testDatasetRoundTrip2() throws InstantiationException, IllegalAccessException{
//		// First we create a dataset with all fields filled in.
//		Dataset ds = new Dataset();
//		ds.setName("someName");
//		ds.setDescription("someDesc");
//		ds.setCreatedBy("magic");
//		ds.setCreatedOn(new Date(System.currentTimeMillis()));
//		ds.setAnnotations("someAnnoUrl");
//		ds.setEtag("110");
//		ds.setId("12");
//		ds.setHasClinicalData(false);
//		ds.setHasExpressionData(true);
//		ds.setHasGeneticData(true);
//		ds.setLayers("someLayerUrl");
//		ds.setReleaseDate(new Date(System.currentTimeMillis()));
//		ds.setStatus("someStatus");
////		ds.setVersion("someVersion");
//		ds.setUri("someUri");
//		ds.setContentType("text/xml");
//		ds.setMd5("8b12c8b413504cf19889efca5605a6c9");
//		// Add some location data
//		List<LocationData> locations = new ArrayList<LocationData>();
//		ds.setLocations(locations);
//		LocationData ldata = new LocationData();
//		ldata.setPath("http://my.home.com:8990/wow");
//		ldata.setType(LocationTypeNames.sage);
//		locations.add(ldata);
//		
//		List<String> diseases = new ArrayList<String>();
//		diseases.add("disease1");
//		ds.setDiseases(diseases);
//		
////		List<String> sampleTypes = new ArrayList<String>();
////		sampleTypes.add("normalTissue");
////		sampleTypes.add("tumorTissue");
////		ds.setSampleType(sampleTypes);
//		
//		// Create a clone using node translation
//		Dataset clone = cloneUsingNodeTranslation(ds);
//		
//		// Now our clone should match the original dataset.
//		System.out.println("Original: "+ds.toString());
//		System.out.println("Clone: "+clone.toString());
//		assertEqualsNonTransient(ds, clone, SchemaCache.getSchema(ds));
//	}
//	
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
				assertEquals("Name: "+field.getName(),field.get(one), field.get(two));
			}
		}
	}
	
	@Test
	public void testLayerRoundTrip() throws InstantiationException, IllegalAccessException, InvalidModelException{
		// First we create a layer with all fields filled in.
		Data layer = new Data();
		layer.setAnnotations("someAnnoUrl");
		layer.setCreatedOn(new Date(System.currentTimeMillis()));
		layer.setDescription("someDescr");
		layer.setEtag("12");
		layer.setId("44");
		layer.setName("someName");
		layer.setNumSamples(new Long(12));
		layer.setPlatform("somePlate");
		layer.setTissueType("type");
		layer.setType(LayerTypeNames.C);
		layer.setUri("someUri");
		
		// Create a clone using node translation
		Data clone = cloneUsingNodeTranslation(layer);
		
		// Now our clone should match the original layer.
		System.out.println("Original: "+layer.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(layer, clone, SchemaCache.getSchema(layer));
	}
	
	@Test
	public void testStepRoundTrip() throws InstantiationException, IllegalAccessException, InvalidModelException {
		Step step = SerializationUseCases.createStepWithReferences();

		// Create a clone using node translation
		Step clone = cloneUsingNodeTranslation(step);
		clone.setCode(null);
		
		// Now our clone should match the original step.
		System.out.println("Original: "+step.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(step, clone, SchemaCache.getSchema(step));
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
	public void testDoubleAdd() throws InvalidModelException{
		Data layer = new Data();
		layer.setType(LayerTypeNames.C);
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(layer, annos, null);
		layer.setType(LayerTypeNames.E);
		// update again
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(layer, annos, null);
		// The E should replace the C
		Object result = annos.getSingleValue("type");
		assertEquals("E", result);
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
		Code code = new Code();
		code.setVersionComment("version comment");
		code.setVersionNumber(new Long(134));
		code.setVersionLabel("1.133.0");
		code.setName("mame");
		Code clone = cloneUsingNodeTranslation(code);
		assertEquals(code, clone);
	}
	
	@Test
	public void testIsPrimaryFieldNames(){
		// check all of the fields for each object type.
		for(EntityType type: EntityType.values()){
			Field[] fields = type.getClassForType().getDeclaredFields();
			for(Field field: fields){
				String name = field.getName();
				assertTrue(NodeTranslationUtils.isPrimaryFieldName(type, name));
				String notName = name+"1";
				assertFalse(NodeTranslationUtils.isPrimaryFieldName(type, notName));
			}
		}
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
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(toClone, annos, dsNode.getReferences());
		// Now crate the clone
		@SuppressWarnings("unchecked")
		T clone = (T) toClone.getClass().newInstance();
		// first apply the annotations
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(clone, annos, dsNode.getReferences());
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
		link.setLinksToClassName(Study.class.getName());
		Node node = NodeTranslationUtils.createFromEntity(link);
		// Set the type for this object
		node.setNodeType(EntityType.getNodeTypeForClass(link.getClass()).name());
		NamedAnnotations annos = new NamedAnnotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(link, annos.getPrimaryAnnotations(), node.getReferences());
		assertNotNull(node.getReferences());
		assertEquals(1, node.getReferences().size());
		Set<Reference> set = (Set<Reference>) node.getReferences().get("linksTo");
		assertNotNull(set);
		assertEquals(1, set.size());
		Reference r = set.iterator().next();
		assertEquals("123", r.getTargetId());
		// Make sure we can make the round trip
		Link newLink = new Link();
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newLink, annos.getPrimaryAnnotations(), node.getReferences());
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
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(example, annos.getPrimaryAnnotations(), node.getReferences());
		Annotations primaryAnnos = annos.getPrimaryAnnotations();
		// First make sure it is set correctly.
		assertNotNull(primaryAnnos.getSingleValue("singleInteger"));
		Long value = (Long) primaryAnnos.getSingleValue("singleInteger");
		assertEquals(new Long(123),value);
		
		// Now the second update we want to clear it out.
		example.setSingleInteger(null);
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(example, annos.getPrimaryAnnotations(), node.getReferences());
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
	
}
