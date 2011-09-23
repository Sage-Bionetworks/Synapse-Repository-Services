package org.sagebionetworks.repo.manager.backup.migration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sagebionetworks.repo.manager.NodeTranslationUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.ObjectType;

/**
 * The only job for this step is to take NodeRevision from v0 to v1.
 * @author John
 *
 */
public class RevisionStepV0toV1 implements RevisionMigrationStep {

	@Override
	public NodeRevision migrateOneStep(NodeRevision toMigrate, ObjectType type) {
		// Only migrate v0 (null) to v1.
		if(!isXmlV0(toMigrate.getXmlVersion())) return toMigrate;
		
		// The major change between v0 and v1 as annotations now have a name-space.
		// Therefore, annotations are stored in a map of annotations rather than as a single set.
		// Note: This addresses PLFM-203.
		NamedAnnotations namespaceAnnotations = new NamedAnnotations();
		Annotations primaryAnnotations = namespaceAnnotations.getPrimaryAnnotations();
		Annotations additionalAnnotations = namespaceAnnotations.getAdditionalAnnotations();
		// Split the single set of annotations into a set for each name-spaces
		Annotations oldStyleAnnos = toMigrate.getAnnotations();
		if(oldStyleAnnos != null){
			Iterator<String> it = oldStyleAnnos.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				Collection values = oldStyleAnnos.getAllValues(key);
				if(NodeTranslationUtils.isPrimaryFieldName(type, key)){
					// Add it to the primary.
					primaryAnnotations.addAnnotation(key, values);
				}else{
					// Add it to additional.
					additionalAnnotations.addAnnotation(key, values);
				}
			}
		}
		// Set the new annotations.
		toMigrate.setNamedAnnotations(namespaceAnnotations);
		// Clear-out the old annotations
		toMigrate.setAnnotations(null);
		// Now that we are done, change the version to v1
		toMigrate.setXmlVersion(NodeRevision.XML_V_1);
		return toMigrate;
	}
	
	/**
	 * We are on V0 if the current step is 
	 * @param xmlVersion
	 * @return
	 */
	protected boolean isXmlV0(String xmlVersion){
		// The first version did not have a version string and will be null.
		if(xmlVersion == null) return true;
		// If the V0 string is applied.
		return NodeRevision.XML_V_0.equals(xmlVersion);
	}

}
