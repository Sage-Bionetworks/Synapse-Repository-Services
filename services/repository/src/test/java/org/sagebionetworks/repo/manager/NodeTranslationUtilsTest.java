package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.StoredLayerPreview;
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
		InputDataLayer layer = new InputDataLayer();
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
		InputDataLayer clone = cloneUsingNodeTranslation(layer);
		
		// Now our clone should match the original layer.
		System.out.println("Original: "+layer.toString());
		System.out.println("Clone: "+clone.toString());
		assertEqualsNonTransient(layer, clone);
	}

	@Test
	public void testLayerPreviewRoundTrip() throws Exception{
		StoredLayerPreview preview = new StoredLayerPreview();
		preview.setPreviewBlob("Pretend this a very long string and needs to be stored as a blob".getBytes("UTF-8"));
		// Create a clone using node translation
		StoredLayerPreview clone = cloneUsingNodeTranslation(preview);
		// Now our clone should match the original layer.
		System.out.println("Original: "+preview.toString());
		System.out.println("Clone: "+clone.toString());
		assertEquals(preview, clone);
	}
	
	@Test
	public void testDoubleAdd() throws InvalidModelException{
		InputDataLayer layer = new InputDataLayer();
		layer.setType("C");
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnoationsFromObject(layer, annos);
		layer.setType("E");
		// update again
		NodeTranslationUtils.updateAnnoationsFromObject(layer, annos);
		// The E should replace the C
		Object result = annos.getSingleValue("type");
		assertEquals("E", result);
	}
	
	@Ignore // We no longer have an object with a collection.
	@Test
	public void testSingleValueCollection(){
		InputDataLayer layer = new InputDataLayer();
		layer.setLocations("/locations");
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnoationsFromObject(layer, annos);
		// Now go back
		InputDataLayer copy = new InputDataLayer();
		NodeTranslationUtils.updateObjectFromAnnotations(copy, annos);
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
		NodeTranslationUtils.updateAnnoationsFromObject(test, annos);
		// The transient field should not be in the annotations
		assertEquals("I should be stored", annos.getSingleValue("nonTransient"));
		assertTrue("A @TransientField should not be stored in the annotations",annos.getSingleValue("markedTransient") == null);
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
		NodeTranslationUtils.updateAnnoationsFromObject(toClone, annos);
		// Now crate the clone
		@SuppressWarnings("unchecked")
		T clone = (T) toClone.getClass().newInstance();
		// first apply the annotations
		NodeTranslationUtils.updateObjectFromAnnotations(clone, annos);
		// then apply the node
		// Apply the node
		NodeTranslationUtils.updateObjectFromNode(clone, dsNode);
		return clone;
	}
	
	
}
