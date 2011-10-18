package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Layer.LayerTypeNames;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.TransientField;

public class NodeTranslationUtilsTest {
	

	@Test
	public void testDatasetRoundTrip() throws InstantiationException, IllegalAccessException{
		// First we create a dataset with all fields filled in.
		Dataset ds = new Dataset();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreator("magic");
		ds.setCreationDate(new Date(System.currentTimeMillis()));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setHasClinicalData(false);
		ds.setHasExpressionData(true);
		ds.setHasGeneticData(true);
		ds.setLayers("someLayerUrl");
		ds.setReleaseDate(new Date(System.currentTimeMillis()));
		ds.setStatus("someStatus");
		ds.setVersion("someVersion");
		ds.setUri("someUri");
		
		// Create a clone using node translation
		Dataset clone = cloneUsingNodeTranslation(ds);
		
		// Now our clone should match the original dataset.
		System.out.println("Original: "+ds.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(ds, clone);
	}
	
	/**
	 * Assert two Nodeable objects are equal while ignoring transient fields.
	 * @param <T>
	 * @param one
	 * @param two
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static <T extends Nodeable> void assertEqualsNonTransient(T one, T two) throws IllegalArgumentException, IllegalAccessException{
		assertNotNull(one);
		assertNotNull(two);
		assertEquals(one.getClass(), two.getClass());
		// Check the fields
		Field[] oneFields = one.getClass().getDeclaredFields();
		for(int i=0; i<oneFields.length;i++){
			Field field = oneFields[i];
			field.setAccessible(true);
			TransientField trans = field.getAnnotation(TransientField.class);
			// Only compare non-transient fields
			if(trans == null){
				assertEquals(field.get(one), field.get(two));
			}
		}
	}
	
	@Test
	public void testLayerRoundTrip() throws InstantiationException, IllegalAccessException, InvalidModelException{
		// First we create a layer with all fields filled in.
		Layer layer = new Layer();
		layer.setAnnotations("someAnnoUrl");
		layer.setCreationDate(new Date(System.currentTimeMillis()));
		layer.setDescription("someDescr");
		layer.setEtag("12");
		layer.setId("44");
		layer.setLocations("/locations");
		layer.setName("someName");
		layer.setNumSamples(new Long(12));
		layer.setPlatform("somePlate");
		layer.setPreviews("somePreview");
		layer.setProcessingFacility("processing");
		layer.setPublicationDate(new Date(System.currentTimeMillis()));
		layer.setQcBy("joe");
		layer.setQcDate(new Date(123123));
		layer.setReleaseNotes("resleaseNote");
		layer.setStatus("someStatus");
		layer.setTissueType("type");
		layer.setType(LayerTypeNames.C.name());
		layer.setUri("someUri");
		layer.setVersion("someVersion");
		
		// Create a clone using node translation
		Layer clone = cloneUsingNodeTranslation(layer);
		
		// Now our clone should match the original layer.
		System.out.println("Original: "+layer.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(layer, clone);
	}
	
	@Test
	public void testStepRoundTrip() throws InstantiationException, IllegalAccessException, InvalidModelException {
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
		
		// First we create a step with all fields filled in.
		Step step = new Step();
		step.setEtag("12");
		step.setId("44");
		step.setName("someName");
		step.setParentId("42");
		step.setUri("/step/42");
		step.setCreationDate(new Date(System.currentTimeMillis()));
		step.setCreatedBy("foo@bar.com");
		step.setStartDate(new Date());
		step.setEndDate(new Date());
		step.setDescription("someDescr");
		step.setCommandLine("/usr/bin/r");
		step.setCode(code);
		step.setInput(input);
		step.setOutput(output);
		step.setAnnotations("/step/42/annotations");
		step.setAccessControlList("/step/42/accessControlList");
		
		// Create a clone using node translation
		Step clone = cloneUsingNodeTranslation(step);
		
		// Now our clone should match the original step.
		System.out.println("Original: "+step.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(step, clone);
	}

	@Test
	public void testLayerPreviewRoundTrip() throws Exception{
		Preview preview = new Preview();
		preview.setPreviewBlob("Pretend this a very long string and needs to be stored as a blob".getBytes("UTF-8"));
		// Create a clone using node translation
		Preview clone = cloneUsingNodeTranslation(preview);
		// Now our clone should match the original layer.
		System.out.println("Original: "+preview.toString());
		System.out.println("Clone: "+clone.toString());
		assertEquals(preview, clone);
	}
	
	@Test
	public void testDoubleAdd() throws InvalidModelException{
		Layer layer = new Layer();
		layer.setType("C");
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnotationsFromObject(layer, annos, null);
		layer.setType("E");
		// update again
		NodeTranslationUtils.updateAnnotationsFromObject(layer, annos, null);
		// The E should replace the C
		Object result = annos.getSingleValue("type");
		assertEquals("E", result);
	}
	
	@Ignore // We no longer have an object with a collection.
	@Test
	public void testSingleValueCollection(){
		Layer layer = new Layer();
		layer.setLocations("/locations");
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnotationsFromObject(layer, annos, null);
		// Now go back
		Layer copy = new Layer();
		NodeTranslationUtils.updateObjectFromAnnotations(copy, annos, null);
		String copyLocations = copy.getLocations();
		assertEquals(layer.getLocations(), copyLocations);
	}
	
	@Test
	public void testSetNullOnNode(){
		Node node = createNew("notNull");
		node.setParentId("90");
		Node copy = new Node();
		NodeTranslationUtils.updateNodeFromObject(node, copy);
		assertTrue(copy.getParentId() != null);
		// Now clear the node parent id
		node.setParentId(null);
		NodeTranslationUtils.updateNodeFromObject(node, copy);
		// The copy should have a null parent id.
		assertTrue(copy.getParentId() == null);
	}
	
	@Test
	public void testTransientData() throws InstantiationException, IllegalAccessException{
		TransientTest test = new TransientTest();
		test.setMarkedTransient("I should not be stored");
		test.setNonTransient("I should be stored");
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnotationsFromObject(test, annos, null);
		// The transient field should not be in the annotations
		assertEquals("I should be stored", annos.getSingleValue("nonTransient"));
		assertTrue("A @TransientField should not be stored in the annotations",annos.getSingleValue("markedTransient") == null);
	}
	
	@Test
	public void testIsPrimaryFieldNames(){
		// check all of the fields for each object type.
		for(ObjectType type: ObjectType.values()){
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
	 * Helper class to test transient fields
	 * @author jmhill
	 *
	 */
	private static class TransientTest {
		private String nonTransient;
		@TransientField
		private String markedTransient;
		public String getNonTransient() {
			return nonTransient;
		}
		public void setNonTransient(String nonTransient) {
			this.nonTransient = nonTransient;
		}
		public String getMarkedTransient() {
			return markedTransient;
		}
		public void setMarkedTransient(String markedTransient) {
			this.markedTransient = markedTransient;
		}
	}
	
	
	
	private static Node createNew(String name){
		Node node = new Node();
		node.setName(name);
		node.setCreatedBy("anonymous");
		node.setModifiedBy("anonymous");
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType("unknown");
		return node;
	}
	
	/**
	 * Will clone an object by first creating a node and annotations for the passed object.
	 * A new object will then be created and populated using the node and annotations.
	 * @param <T>
	 * @param toClone
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static <T> T cloneUsingNodeTranslation(T toClone) throws InstantiationException, IllegalAccessException{
		// Create a node using this the object
		Node dsNode = NodeTranslationUtils.createFromBase(toClone);
		// Update an annotations object using the object
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnotationsFromObject(toClone, annos, dsNode.getReferences());
		// Now crate the clone
		@SuppressWarnings("unchecked")
		T clone = (T) toClone.getClass().newInstance();
		// first apply the annotations
		NodeTranslationUtils.updateObjectFromAnnotations(clone, annos, dsNode.getReferences());
		// then apply the node
		// Apply the node
		NodeTranslationUtils.updateObjectFromNode(clone, dsNode);
		return clone;
	}
	
	
}
