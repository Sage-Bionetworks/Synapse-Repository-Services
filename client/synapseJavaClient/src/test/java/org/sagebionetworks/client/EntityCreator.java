package org.sagebionetworks.client;

import java.util.Date;

import org.sagebionetworks.repo.model.Folder;

/**
 * Helper to create sample entities for tests.
 * @author jmhill
 *
 */
public class EntityCreator {
		
	/**
	 * Create a new folder with all of the fields set.
	 * @return
	 */
	public static Folder createNewFolder(){
		Folder toCreate = new Folder();
		String id = "124";
		toCreate.setId(id);
		toCreate.setName("The Big Cheese");
		toCreate.setCreatedBy("creator@gmail.com");
		toCreate.setModifiedBy("modifier@gmail.com");
		long now = System.currentTimeMillis();
		toCreate.setModifiedOn(new Date(now+1000));
		toCreate.setCreatedOn(new Date(now));
		toCreate.setDescription("A detailed description");
		toCreate.setEtag("335");
		toCreate.setParentId("1");
		return toCreate;
	}

}
