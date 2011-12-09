package org.sagebionetworks.web.client;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.gwt.client.schema.adapter.JSONObjectGwt;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
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
	
	@Override
	public void gwtSetUp() {
		// Create a dataset with all fields filled in
	}
	
	@Test
	public void testNodeModelCreatorImpl_createDataset() throws JSONObjectAdapterException, RestServiceException{
		Dataset populatedDataset = new Dataset();
		initilaizedJSONEntityFromSchema(populatedDataset);
		assertNotNull(populatedDataset);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedDataset.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Dataset clone = modelCreator.createDataset(json);
		assertNotNull(clone);
		assertEquals(populatedDataset, clone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_createLayer() throws JSONObjectAdapterException, RestServiceException{
		Layer populatedLayer = new Layer();
		initilaizedJSONEntityFromSchema(populatedLayer);
		assertNotNull(populatedLayer);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedLayer.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Layer clone = modelCreator.createLayer(json);
		assertNotNull(clone);
		assertEquals(populatedLayer, clone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_createProject() throws JSONObjectAdapterException, RestServiceException{
		Project populatedProject = new Project();
		initilaizedJSONEntityFromSchema(populatedProject);
		assertNotNull(populatedProject);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedProject.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Project clone = modelCreator.createProject(json);
		assertNotNull(clone);
		assertEquals(populatedProject, clone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_createEULA() throws JSONObjectAdapterException, RestServiceException{
		Eula populatedEula = new Eula();
		initilaizedJSONEntityFromSchema(populatedEula);
		assertNotNull(populatedEula);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedEula.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Eula clone = modelCreator.createEULA(json);
		assertNotNull(clone);
		assertEquals(populatedEula, clone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_Agreement() throws JSONObjectAdapterException, RestServiceException{
		Agreement populatedAgreement = new Agreement();
		initilaizedJSONEntityFromSchema(populatedAgreement);
		assertNotNull(populatedAgreement);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedAgreement.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Agreement clone = modelCreator.createAgreement(json);
		assertNotNull(clone);
		assertEquals(populatedAgreement, clone);
		// Make sure we can go back to json
		String jsonClone = modelCreator.createAgreementJSON(clone);
		assertEquals(json, jsonClone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_createAnalysis() throws JSONObjectAdapterException, RestServiceException{
		Analysis populatedAnalysis = new Analysis();
		initilaizedJSONEntityFromSchema(populatedAnalysis);
		assertNotNull(populatedAnalysis);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedAnalysis.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Analysis clone = modelCreator.createAnalysis(json);
		assertNotNull(clone);
		assertEquals(populatedAnalysis, clone);
	}
	
	@Test
	public void testNodeModelCreatorImpl_createStep() throws JSONObjectAdapterException, RestServiceException{
		Step populatedStep = new Step();
		initilaizedJSONEntityFromSchema(populatedStep);
		assertNotNull(populatedStep);
		// Get the JSON for the populate dataset
		JSONObjectAdapter adapter = populatedStep.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		String json = adapter.toJSONString();
		assertNotNull(json);
		// Use the factor to create a clone
		NodeModelCreatorImpl modelCreator = new NodeModelCreatorImpl(null,null); // jsonadapter and entitytypeprovider not needed for this deprecated model creation
		Step clone = modelCreator.createStep(json);
		assertNotNull(clone);
		assertEquals(populatedStep, clone);
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
