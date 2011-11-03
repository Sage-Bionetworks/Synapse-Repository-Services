package org.sagebionetworks.web.client;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.gwt.client.schema.adapter.JSONObjectGwt;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.transform.NodeModelCreatorImpl;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

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
		return "org.sagebionetworks.web.Portal";
	}
	
	private Dataset populatedDataset;
	
	@Override
	public void gwtSetUp() {
		// Create a dataset with all fields filled in
		populatedDataset = new Dataset();
		try {
			initilaizedJSONEntityFromSchema(populatedDataset);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testNodeModelCreatorImpl_createDataset() throws JSONObjectAdapterException, RestServiceException{
		assertNotNull(populatedDataset);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedDataset.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl();
		Dataset clone = modelCreator.createDataset(json);
		assertNotNull(clone);
		assertEquals(populatedDataset, clone);
	}
	
	/**
	 * Populate a given entity using all of the fields from the schema.
	 * @param toPopulate
	 * @throws JSONObjectAdapterException 
	 */
	private void initilaizedJSONEntityFromSchema(JSONEntity toPopulate) throws JSONObjectAdapterException{
		JSONObjectAdapter adapter =  JSONObjectGwt.createNewAdapter().createNew(toPopulate.getJSONSchema());
		ObjectSchema schema = new ObjectSchema(adapter);
		// This adapter will be used to populate the entity
		JSONObjectAdapter adapterToPopulate = JSONObjectGwt.createNewAdapter();
		Map<String, ObjectSchema> properteis = schema.getProperties();
		assertNotNull(properteis);
		int index = 0;
		for(String propertyName: properteis.keySet()){
			ObjectSchema propertySchema = properteis.get(propertyName);
			if(TYPE.STRING == propertySchema.getType()){
				// This could be a date or an enumeration.
				String value = "StringValue for "+propertyName;
				if(propertySchema.getFormat() == null && propertySchema.getEnum() == null){
					// This is just a normal string
					value = "StringValue for "+propertyName;
				}else if(FORMAT.DATE_TIME == propertySchema.getFormat()){
					value = adapter.convertDateToString(FORMAT.DATE_TIME, new Date());
				}else if(propertySchema.getEnum() != null){
					int enumIndex = propertySchema.getEnum().length % index;
					value = propertySchema.getEnum()[enumIndex];
				}else{
					if(propertySchema.isRequired()){
						throw new IllegalArgumentException("Unknown FORMAT: "+propertySchema.getFormat()+" for required property");
					}
				}
				// Set the string value
				adapterToPopulate.put(propertyName, value);
			}else if(TYPE.BOOLEAN == propertySchema.getType()){
				Boolean value = index % 2 == 0;
				adapterToPopulate.put(propertyName, value);
			}else if(TYPE.INTEGER == propertySchema.getType()){
				Long value = new Long(123+index);
				adapterToPopulate.put(propertyName, value);
			}else if(TYPE.NUMBER == propertySchema.getType()){
				Double value = new Double(456.0909+index);
				adapterToPopulate.put(propertyName, value);
			}else{
				if(propertySchema.isRequired()){
					throw new IllegalArgumentException("Unknown type:"+propertySchema.getType()+" for required property");
				}
			}
			index++;
		}
		// Now populate it with data
		toPopulate.initializeFromJSONObject(adapterToPopulate);
	}

	@Override
	public String toString() {
		return "GwtTestSuite for Module: "+getModuleName();
	}
	

}
