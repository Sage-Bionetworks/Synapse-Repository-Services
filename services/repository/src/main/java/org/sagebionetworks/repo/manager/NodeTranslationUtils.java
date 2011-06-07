package org.sagebionetworks.repo.manager;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.TransientField;

/**
 * Converts to/from datasets and nodes.
 * @author jmhill
 *
 */

@SuppressWarnings("rawtypes")
public class NodeTranslationUtils {
	
	private static final Logger log = Logger.getLogger(NodeTranslationUtils.class.getName());
	
	/**
	 * Keep track of the know fields of a node.
	 */
	private static Map<String, Field> nodeFieldNames = new HashMap<String, Field>();
	private static Map<String, String> nameConvertion = new HashMap<String, String>();
	static{
		// Populate the nodeFieldNames
		Field[] fields = Node.class.getDeclaredFields();
		for(Field field: fields){
			// make sure all are
			nodeFieldNames.put(field.getName(), field);
		}
		// Add the name required name conversions
		nameConvertion.put("creator", "createdBy");
		nameConvertion.put("creationDate", "createdOn");
		nameConvertion.put("etag", "eTag");
	}
	
	
	/**
	 * Create a new node from the passed base object.
	 * @param dataset
	 * @return
	 */
	public static <T> Node createFromBase(T base){
		if(base == null) throw new IllegalArgumentException("Base Object cannot be null");
		Node node = new Node();
		updateNodeFromObject(base, node);
		return node;
	}

	/**
	 * Use the passed object to update a node.
	 * @param <T>
	 * @param base
	 * @param node
	 */
	public static <T> void updateNodeFromObject(T base, Node node) {
		Field[] fields = base.getClass().getDeclaredFields();
		for(Field field: fields){
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if(nodeName == null){
				nodeName = name;
			}
			// Only include fields that are in node.
			Field nodeField = nodeFieldNames.get(nodeName);
			if(nodeField != null){
				// Make sure we can call it.
				field.setAccessible(true);
				nodeField.setAccessible(true);
				Object value;
				try {
					value = field.get(base);
					nodeField.set(node, value);
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * Add any fields from the object that are not on a node.
	 * @param base
	 * @param annos
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T> void updateAnnoationsFromObject(T base, Annotations annos) {
		if(base == null) throw new IllegalArgumentException("Base cannot be null");
		if(annos == null) throw new IllegalArgumentException("Annotations cannot be null");
		// Find the fields that are not on nodes.
		Field[] fields = base.getClass().getDeclaredFields();
		for(Field field: fields){
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if(nodeName == null){
				nodeName = name;
			}
			// Is this a field already on Node?
			if(!nodeFieldNames.containsKey(nodeName)){
				// Make sure we can call it.
				field.setAccessible(true);
				Object value;
				try {
					value = field.get(base);
					// We do not store fields that are marked as @TransientField
					TransientField transientField = field.getAnnotation(TransientField.class);
					if(value != null && transientField == null){
						annos.replaceAnnotation(name, value);
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * Update an object using the a node
	 * @param <T>
	 * @param base
	 * @param node
	 */
	public static <T> void updateObjectFromNode(T base, Node node){
		if(base == null) throw new IllegalArgumentException("Base cannot be null");
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		Field[] fields = base.getClass().getDeclaredFields();
		for(Field field: fields){
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if(nodeName == null){
				nodeName = name;
			}
			// Only include fields that are in node.
			Field nodeField = nodeFieldNames.get(nodeName);
			if(nodeField != null){
				// Make sure we can call it.
				field.setAccessible(true);
				nodeField.setAccessible(true);
				Object value;
				try {
					value = nodeField.get(node);
					if(value != null){
						field.set(base, value);
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * Update an object using annotations.
	 * @param <T>
	 * @param base
	 * @param annos
	 */
	public static <T> void updateObjectFromAnnotations(T base, Annotations annos) {
		if(base == null) throw new IllegalArgumentException("Base cannot be null");
		if(annos == null) throw new IllegalArgumentException("Annotations cannot be null");
		// Find the fields that are not on nodes.
		Field[] fields = base.getClass().getDeclaredFields();
		for(Field field: fields){
			String name = field.getName();
			String nodeName = nameConvertion.get(name);
			if(nodeName == null){
				nodeName = name;
			}
			// Is this a field already on Node?
			if(!nodeFieldNames.containsKey(nodeName)){
				// Make sure we can call it.
				field.setAccessible(true);
				try {
					Object value = annos.getSingleValue(name);
					if(value != null){
						if(field.getType() == Boolean.class){
							// We need to convert the string to a boolean
							value = Boolean.parseBoolean((String)value);
						}
						if(field.getType().isAssignableFrom(Collection.class)){
							List list = new ArrayList();
							list.add(value);
							field.set(base, list);
						}else{
							field.set(base, value);
						}
					}
				} catch (IllegalAccessException e) {
					// This should never occur
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
}
