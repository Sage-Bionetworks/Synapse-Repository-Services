package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;

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
		ds.setLayer("someLayerUrl");
		ds.setReleaseDate(new Date(System.currentTimeMillis()));
		ds.setStatus("someStatus");
		ds.setVersion("someVersion");
		ds.setUri("someUri");
		
		// Create a clone using node translation
		Dataset clone = cloneUsingNodeTranslation(ds);
		
		// Now our clone should match the original dataset.
		System.out.println("Original: "+ds.toString());
		System.out.println("Clone: "+clone.toString());
		assertEquals(ds, clone);
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
		Collection<String> locations = new ArrayList();
		locations.add("locationOne");
		locations.add("locatoinTwo");
		locations.add("locationThree");
		layer.setLocations(locations);
		layer.setName("someName");
		layer.setNumSamples(new Long(12));
		layer.setPlatform("somePlate");
		layer.setPreview("somePreview");
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
		assertEquals(layer, clone);
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
		T clone = (T) toClone.getClass().newInstance();
		// first apply the annotations
		NodeTranslationUtils.updateObjectFromAnnotations(clone, annos);
		// then apply the node
		// Apply the node
		NodeTranslationUtils.updateObjectFromNode(clone, dsNode);
		return clone;
	}
	
	
}
