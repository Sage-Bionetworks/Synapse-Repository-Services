package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityWithAnnotations;
import org.sagebionetworks.repo.model.Folder;

@SuppressWarnings("rawtypes")
public class EntityToMapUtilTest {

	@Test
	public void testDatasetRoundTrip() throws InstantiationException, IllegalAccessException{
		// First we create a dataset with all fields filled in.
		Folder ds = new Folder();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreatedBy("magic");
		ds.setCreatedOn(new Date(System.currentTimeMillis()));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setUri("someUri");
		
		// Create the annotations from this object
		Annotations annos = new Annotations();
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(ds, annos);
		// Add some extra annotations
		annos.addAnnotation("someString", "one");
		annos.addAnnotation("someLong", new Long(12));
		annos.addAnnotation("someDate", new Date(0L));
		annos.addAnnotation("someDouble", new Double(1.2));
		
		// Combine them
		EntityWithAnnotations<Folder> ewa = new EntityWithAnnotations<Folder>();
		ewa.setAnnotations(annos);
		ewa.setEntity(ds);
		// Now convert this to a map
		Map<String, Object> map = EntityToMapUtil.createMapFromEntity(ewa);
		System.out.println(map);
		assertNotNull(map);
		// Now make sure we can get the values out
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
