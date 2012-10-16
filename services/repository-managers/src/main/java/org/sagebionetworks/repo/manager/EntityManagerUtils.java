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
	
	public static NodeRevisionBackup changeNodeRevisionBackupNodeType(NodeRevisionBackup nrb, String newType) throws JSONObjectAdapterException {
		NamedAnnotations namedAnnots = nrb.getNamedAnnotations();
		Annotations primaryAnnots = namedAnnots.getPrimaryAnnotations();
		Annotations additionalAnnots = namedAnnots.getAdditionalAnnotations();
		EntityType newEntityType = EntityType.valueOf(newType);
		Set<String> fieldsToMove = EntityManagerUtils.getSetOfPrimaryFieldsToMove(primaryAnnots.keySet(), newEntityType.getClassForType().getName());
		moveFieldsFromPrimaryToAdditionals(primaryAnnots, additionalAnnots, fieldsToMove);
		return nrb;
	}
	
//	Not needed keyset() returns all the keys...
//	public static List<String> getAllKeys(Annotations annots) {
//		List<String> l = new ArrayList<String>();
//		for (String k: annots.keySet()) {
////			Annotations a = (Annotations)annots.getSingleValue(k);
////			l.addAll(a.keySet());
//			System.out.println(k);
//		}
//		return l;
//	}

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
		
		// TODO: There's got to be a better way of handling each type of list
		// List<Map <String, List<? extends Object>>> srcAnnots;
		
//		Map<String, List<byte[]>> srcBlobAnnots = primaryAnnots.getBlobAnnotations();
//		Map<String, List<byte[]>> destBlobAnnots = additionalAnnots.getBlobAnnotations();
//		moveFields(srcBlobAnnots, destBlobAnnots, keysToMove);
//		Map<String, List<Date>> srcDateAnnots = primaryAnnots.getDateAnnotations();
//		Map<String, List<Date>> destDateAnnots = additionalAnnots.getDateAnnotations();
//		moveFields(srcDateAnnots, destDateAnnots, keysToMove);
//		Map<String, List<Double>> srcDoubleAnnots = primaryAnnots.getDoubleAnnotations();
//		Map<String, List<Double>> destDoubleAnnots = additionalAnnots.getDoubleAnnotations();
//		moveFields(srcDoubleAnnots, destDoubleAnnots, keysToMove);
//		Map<String, List<Long>> srcLongAnnots = primaryAnnots.getLongAnnotations();
//		Map<String, List<Long>> destLongAnnots = additionalAnnots.getLongAnnotations();
//		moveFields(srcLongAnnots, destLongAnnots, keysToMove);
//		Map<String, List<String>> srcStringAnnots = primaryAnnots.getStringAnnotations();
//		Map<String, List<String>> destStringAnnots = additionalAnnots.getStringAnnotations();
//		moveFields(srcStringAnnots, destStringAnnots, keysToMove);
//		
	}
	
	// TODO: Rewrite using delete/add method on Annotations
	public static <T extends Object> void moveFields(Map<String, List<T>> l1, Map<String, List<T>> l2, List<String> keysToMove) {
		Iterator<Map.Entry<String, List<T>>> iter = l1.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, List<T>> entry = iter.next();
			if (keysToMove.contains(entry.getKey())) {
				List<T> value = entry.getValue();
				if (! l2.containsKey(entry.getKey())) {
					l2.put(entry.getKey(), value);
				} else {
					List<T> targetValue = l2.get(entry.getKey());
					targetValue.addAll(value);
					l2.put(entry.getKey(), targetValue);
				}
				iter.remove();
			}
		}
	}

	// TODO: Better exception handling
	public static boolean isValidTypeChange(String srcTypeName, String destTypeName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
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
		// every type that has src as valid parent should also have dest as valid parent type
		if (v) {
			for (EntityType t: EntityType.values()) {
				List<String> l = t.getMetadata().getValidParentTypes();
				if ((l.contains(srcClassName)) && (! l.contains(destClassName))) {
					v = false;
					break;
				}
			}			
		}
		return v;
	}



}