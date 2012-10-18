package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class EntityManagerUtils {
	
	public static Set<String> getSetOfPrimaryFieldsToMove(Set<String> srcKeys, String newEntityTypeClass) throws JSONObjectAdapterException {
		Set<String> s = new HashSet<String>();
		AutoGenFactory autoGenFactory = new AutoGenFactory();
		JSONEntity entity = autoGenFactory.newInstance(newEntityTypeClass);
		ObjectSchema os = EntityFactory.createEntityFromJSONString(entity.getJSONSchema(), ObjectSchema.class);
		Map<String, ObjectSchema> props = os.getProperties();
		Set<String> destKeys = props.keySet();
		for (String k: srcKeys) {
			if (! destKeys.contains(k)) {
				s.add(k);
			}
		}
		return s;
	}
	
	public static void moveFieldsFromPrimaryToAdditionals(Annotations primaryAnnots, Annotations additionalAnnots, Set<String> keysToMove) {
		// Move annotations from primary to additional
		for (String key: keysToMove) {
			if (primaryAnnots.getBlobAnnotations().containsKey(key)) {
				Object o = primaryAnnots.getBlobAnnotations().remove(key);
				additionalAnnots.addAnnotation(key, o);
			}
			if (primaryAnnots.getDateAnnotations().containsKey(key)) {
				Object o = primaryAnnots.getDateAnnotations().remove(key);
				additionalAnnots.addAnnotation(key, o);
			}
			if (primaryAnnots.getDoubleAnnotations().containsKey(key)) {
				Object o = primaryAnnots.getDoubleAnnotations().remove(key);
				additionalAnnots.addAnnotation(key, o);
			}
			if (primaryAnnots.getLongAnnotations().containsKey(key)) {
				Object o = primaryAnnots.getLongAnnotations().remove(key);
				additionalAnnots.addAnnotation(key, o);
			}
			if (primaryAnnots.getStringAnnotations().containsKey(key)) {
				Object o = primaryAnnots.getStringAnnotations().remove(key);
				additionalAnnots.addAnnotation(key, o);
			}
		}
	}

	public static Node cetChangeNodeRevision(Node node, String newType) {
		node.setNodeType(newType);
		return node;
	}
	
	public static NamedAnnotations cetChangeNamedAnnotations(NamedAnnotations namedAnnots, String newEntityTypeName) throws JSONObjectAdapterException {
		Annotations primaryAnnots = namedAnnots.getPrimaryAnnotations();
		Annotations additionalAnnots = namedAnnots.getAdditionalAnnotations();
		EntityType newEntityType = EntityType.valueOf(newEntityTypeName);
		Set<String> fieldsToMove = getSetOfPrimaryFieldsToMove(primaryAnnots.keySet(), newEntityType.getClassForType().getName());
		moveFieldsFromPrimaryToAdditionals(primaryAnnots, additionalAnnots, fieldsToMove);
		return namedAnnots;
	}
	
	// TODO: Better exception handling
	public static boolean isValidTypeChange(boolean entityHasChildren, String srcTypeName, String destTypeName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		boolean v = true;
		EntityType srcType = EntityType.valueOf(srcTypeName);
		EntityType destType = EntityType.valueOf(destTypeName);
		EntityTypeMetadata srcTypeMetadata = srcType.getMetadata();
		EntityTypeMetadata destTypeMetadata = destType.getMetadata();
		String srcClassName = srcTypeMetadata.getEntityType();
		String destClassName = destTypeMetadata.getEntityType();
		
		// Check compatible interfaces
		Object srcObj = Class.forName(srcClassName).newInstance();
		Object destObj = Class.forName(destClassName).newInstance();
		if (srcObj instanceof Locationable) {
			if (! (Locationable.class.isInstance(destObj))) {
				return false;
			}
		}
		if (Versionable.class.isInstance(srcObj)) {
			if (! (Versionable.class.isInstance(destObj))) {
				return false;
			}
		}
		
		// src valid parent types should be included in dest valid parent types
		for (String vpSrc: srcTypeMetadata.getValidParentTypes()) {
			if (! destTypeMetadata.getValidParentTypes().contains(vpSrc)) {
				v = false;
				break;
			}
		}
		// if entity has children then the destination must be project or folder/study
		// Note: This could require some cleanup if there are children at leaf entities
		// such as data etc.
		if (v) {
			if (entityHasChildren) {
				if ((! "project".equals(destTypeName)) && (! "folder".equals(destTypeName)) && (! "study".equals(srcTypeName))) {
					v = false;
				}
			}
		}
		return v;
	}



}