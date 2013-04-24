package org.sagebionetworks.repo.manager.backup.migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.ResourceAccess;
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
		instance.revisionSteps = new ArrayList();
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
	
	// migrate user names to principal IDs in node and ACL
	// Note, do NOT need to update the 'modifiedBy', which 
	// is only used during serialization, not deserialization
	@Override
	public void migrateNodePrincipals(NodeBackup nodeBackup) {
		if (nodeBackup==null) throw new IllegalArgumentException("NodeBackup cannot be null");
		Node node = nodeBackup.getNode();
		if (node==null) throw new IllegalArgumentException("Node cannot be null");
		AccessControlList acl = nodeBackup.getAcl();
		if (acl!=null) {
			// this set will hold the ResourceAccess objects that we don't skip
			Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
			for (ResourceAccess ra : acl.getResourceAccess()) {
				if (ra.getPrincipalId()==null) {
					Long principalId = nodeOwnerMigrator.getUserPrincipal(ra.getGroupName());
					if (principalId==null) {
						continue; // can't figure out the principal, so just skip this ResourceAccess
					} else {
						// fix up the ResourceAccess object 
						ra.setPrincipalId(principalId);
					}
				}
				// the ResourceAccess object is OK or was fixed up.  Now add it to the set.
				ras.add(ra);
			}
			// now put the finalized set of ResourceAccess objects in the acl
			acl.setResourceAccess(ras);
		}
	}


}
