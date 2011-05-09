package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;

public class EntityToMapUtilTest {

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
		
		// Create the annotations from this object
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateAnnoationsFromObject(ds, annos);
		// Add some extra annotations
		annos.addAnnotation("someString", "one");
		annos.addAnnotation("someLong", new Long(12));
		annos.addAnnotation("someDate", new Date(0L));
		annos.addAnnotation("someDouble", new Double(1.2));
		
		// Combine them
		EntityWithAnnotations<Dataset> ewa = new EntityWithAnnotations<Dataset>();
		ewa.setAnnotations(annos);
		ewa.setEntity(ds);
		// Now convert this to a map
		Map<String, Object> map = EntityToMapUtil.createMapFromEntity(ewa);
		assertNotNull(map);
		// Now make sure we can get the values out
		assertEquals(Boolean.TRUE, map.get("hasExpressionData"));
		assertEquals(ds.getName(), map.get("name"));
		assertEquals(ds.getEtag(), map.get("etag"));
		assertEquals("one", getSingleValueFromCollection(map.get("someString")));
		assertEquals(new Long(12), getSingleValueFromCollection(map.get("someLong")));
		assertEquals(new Date(0L), getSingleValueFromCollection(map.get("someDate")));
		assertEquals(new Double(1.2), getSingleValueFromCollection(map.get("someDouble")));
	}
	
	public Object getSingleValueFromCollection(Object collection){
		assertTrue(collection instanceof Collection);
		Collection col = (Collection) collection;
		assertEquals(1, col.size());
		return col.iterator().next();
	}
}
