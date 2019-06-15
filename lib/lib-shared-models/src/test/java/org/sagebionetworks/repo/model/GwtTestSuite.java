package org.sagebionetworks.repo.model;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.gwt.client.schema.adapter.GwtAdapterFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.gwt.junit.client.GWTTestCase;


/**
 * Since the GWT test are so slow to start and we could not get the GWTTestSuite to work,
 * we put all GWT tests in one class.
 * @author jmhill
 *
 */
public class GwtTestSuite extends GWTTestCase {

	/**
	 * Must refer to a valid module that sources this class.
	 */
	public String getModuleName() { 
		return "org.sagebionetworks.repo.SharedSynapseDTOs";
	}
	
	String registerJson = null;
	
	@Override
	public void gwtSetUp() {

	}
	
	@Override
	public String toString() {
		return "GwtTestSuite for Module: "+getModuleName();
	}
	
	@Test
	public void testAnnotationsRoundTrip() throws JSONObjectAdapterException {
		Annotations annos = new Annotations();
		annos.addAnnotation("string", "one");
		annos.addAnnotation("string", "two");
		annos.addAnnotation("long", new Long(123));
		annos.addAnnotation("double", new Double(123.456));
		annos.addAnnotation("date", new Date());
		
		// Write it to GWT
		GwtAdapterFactory factory = new GwtAdapterFactory();
		JSONObjectAdapter adapter = factory.createNew();
		annos.writeToJSONObject(adapter);
		String json = adapter.toJSONString();
		adapter = factory.createNew(json);
		
		// Clone it
		Annotations clone = new Annotations();
		clone.initializeFromJSONObject(adapter);
		assertEquals(annos, clone);		
	}
	
	/**
	 * Test that extra fields are preserved.
	 * 
	 * @throws JSONObjectAdapterException
	 */
	@Test
	public void testExtraFields() throws JSONObjectAdapterException {
		GwtAdapterFactory factory = new GwtAdapterFactory();
		JSONObjectAdapter adapter = factory.createNew();
		adapter.put("junk", "junkValue");
		adapter.put("name", "columnName");
		ColumnModel cm = new ColumnModel(adapter);
		
		JSONObjectAdapter cloneAdapter = factory.createNew();
		cm.writeToJSONObject(cloneAdapter);
		assertEquals("columnName", cloneAdapter.get("name"));
		assertEquals("junkValue", cloneAdapter.get("junk"));
	}
	
	@Test
	public void testEntityBundleRoundTrip() throws JSONObjectAdapterException {
		EntityBundle entityBundle = EntityBundleTest.createDummyEntityBundle();
		
		// Write it to GWT
		GwtAdapterFactory factory = new GwtAdapterFactory();
		JSONObjectAdapter adapter = factory.createNew();
		entityBundle.writeToJSONObject(adapter);
		String json = adapter.toJSONString();
		adapter = factory.createNew(json);
		
		// Clone it
		EntityBundle clone = new EntityBundle();
		clone.initializeFromJSONObject(adapter);
		assertEquals(entityBundle, clone);
	}

}
