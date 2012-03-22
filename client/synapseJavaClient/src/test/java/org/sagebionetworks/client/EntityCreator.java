package org.sagebionetworks.client;

import java.util.Date;

import org.sagebionetworks.repo.model.Study;

/**
 * Helper to create sample entities for tests.
 * @author jmhill
 *
 */
public class EntityCreator {
	
	/**
	 * Create a new dataset with all of the fields set.
	 * @return
	 */
	public static Study createNewDataset(){
		Study toCreate = new Study();
		String id = "123";
		toCreate.setId("123");
		toCreate.setName("The Big Cheese");
		toCreate.setAnnotations("/dataset/"+id+"/annotations");
		toCreate.setAnnotations("/dataset/"+id+"/acl");
		toCreate.setCreatedBy("creator@gmail.com");
		toCreate.setModifiedBy("modifier@gmail.com");
		long now = System.currentTimeMillis();
		toCreate.setModifiedOn(new Date(now+1000));
		toCreate.setCreatedOn(new Date(now));
		toCreate.setDescription("A detailed description");
		toCreate.setEtag("334");
		toCreate.setParentId("1");
		toCreate.setUri("/dataset/"+id);
		return toCreate;
	}

}
