package org.sagebionetworks.repo.manager.backup.migration;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a simple step-wise migration driver implementation. 
 * 
 * @author jmhill
 *
 */
public class MigrationDriverImpl implements MigrationDriver{
	
	@Autowired
	private NodeOwnerMigrator nodeOwnerMigrator;
		
	/**
	 * Each revision step is responsible for migrating the object a single revision.
	 * For example, a single step is used to go from version "0.0" to "0.1"
	 */
	private List<RevisionMigrationStep> revisionSteps = null;
	
	/**
	 * 
	 * For use by unit tests
	 */
	public static MigrationDriver instanceForTesting() {
		MigrationDriverImpl instance = new MigrationDriverImpl();
		instance.revisionSteps = new ArrayList<RevisionMigrationStep>();
		return instance;
	}

	/**
	 * 
	 * This is injected by Spring
	 * 
	 * @param steps
	 */
	public void setRevisionSteps(List<RevisionMigrationStep> steps) {
		revisionSteps = steps;
	}

	/**
	 * Walk over each step until we get to the current version.
	 */
	@Override
	public EntityType migrateToCurrentVersion(NodeRevisionBackup toMigrate,	EntityType type) {
		if(toMigrate == null) throw new IllegalArgumentException("NodeRevsion toMigrate cannot be null");
		if(type == null) throw new IllegalArgumentException("ObjectType cannot be null");
		String startingVersion = toMigrate.getXmlVersion();
		EntityType newType = EntityType.unknown;
		// Take each step to get to the current version
		for(RevisionMigrationStep step: revisionSteps){
			newType = step.migrateOneStep(toMigrate, type);
		}
		// Validate we are on the current version
		if(!NodeRevisionBackup.CURRENT_XML_VERSION.equals(toMigrate.getXmlVersion())) throw new IllegalStateException("Failed to migrate a NodeRevisionBackup from version: "+startingVersion+" to the current version: "+NodeRevisionBackup.CURRENT_XML_VERSION);
		return newType;
	}
}
