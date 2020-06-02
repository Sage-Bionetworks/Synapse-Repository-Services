package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewEntityType;

/**
 * Utilities for entity type.
 * 
 * When a new EntityType is added to org.sagebionetworks.repo.model.EntityType,
 * a new EntityTypeMetadata must be built and added to the metadataArray.
 * 
 * @author jmhill
 * @author kimyen
 *
 */
public class EntityTypeUtils {

	private static final EntityTypeMetadata[] metadataArray;
	/*
	 * This map helps getting the class of an EntityType given the EntityType.
	 * 
	 * Since GWT does not compile with Class.forName() and there is no ways to
	 * auto generate EntityTypeMetadata with a class type variable, we store the
	 * string class name in EntityTypeMetadata, and use this map to look for
	 * the class of the entity type.
	 */
	private static final Map<String, Class<? extends Entity>> className;

	static {
		metadataArray = new EntityTypeMetadata[] {
				// project
				buildMetadata(EntityType.project, Arrays.asList("DEFAULT"), Project.class, "Project"),
				// file
				buildMetadata(EntityType.file, Arrays.asList(Project.class.getName(), Folder.class.getName()), FileEntity.class, "File"),
				// folder
				buildMetadata(EntityType.folder, Arrays.asList(Project.class.getName(), Folder.class.getName()), Folder.class, "Folder"),
				// table
				buildMetadata(EntityType.table, Arrays.asList(Project.class.getName(), Folder.class.getName()), TableEntity.class, "Table"),
				// link
				buildMetadata(EntityType.link, Arrays.asList(Project.class.getName(), Folder.class.getName()), Link.class, "Link"),
				// EntityView
				buildMetadata(EntityType.entityview, Arrays.asList(Project.class.getName(), Folder.class.getName()), EntityView.class, "Entity View"),
				// dockerrepo
				buildMetadata(EntityType.dockerrepo, Arrays.asList(Project.class.getName()), DockerRepository.class, "Docker Repository"),
				// submission views
				buildMetadata(EntityType.submissionview, Arrays.asList(Project.class.getName(), Folder.class.getName()), SubmissionView.class, "Submission View")
		};

		className = new HashMap<String, Class<? extends Entity>>();
		className.put(Project.class.getName(), Project.class);
		className.put(FileEntity.class.getName(), FileEntity.class);
		className.put(Folder.class.getName(), Folder.class);
		className.put(TableEntity.class.getName(), TableEntity.class);
		className.put(Link.class.getName(), Link.class);
		className.put(EntityView.class.getName(), EntityView.class);
		className.put(DockerRepository.class.getName(), DockerRepository.class);
		className.put(SubmissionView.class.getName(), SubmissionView.class);
	}

	/**
	 * 
	 * @param type
	 * @param validParentTypes
	 * @param clazz
	 * @param displayName
	 * @return the metadata of an entity type
	 */
	private static EntityTypeMetadata buildMetadata(EntityType type, List<String> validParentTypes, Class<? extends Entity> clazz, String displayName) {
		EntityTypeMetadata metadata = new EntityTypeMetadata();
		metadata.setAliases(Arrays.asList(type.name(),"entity"));
		metadata.setDefaultParentPath("/root");
		metadata.setClassName(clazz.getName());
		metadata.setValidParentTypes(validParentTypes);
		metadata.setName(type.name());
		metadata.setEntityType(type);
		metadata.setDisplayName(displayName);
		return metadata;
	}
	
	/**
	 * Get the entity Registry
	 * @return
	 */
	public static EntityRegistry getEntityRegistry() {
		EntityRegistry reg = new EntityRegistry();
		reg.setEntityTypes(new LinkedList<EntityTypeMetadata>());
		for(EntityTypeMetadata metadata : metadataArray){
			reg.getEntityTypes().add(metadata);
		}
		return reg;
	}
	
	/**
	 * 
	 * @param type
	 * @return the class that goes with this type
	 */
	public static Class<? extends Entity> getClassForType(EntityType type) {
		String name = getMetadata(type).getClassName();
		if (className.containsKey(name)) {
			return className.get(name);
		} else {
			throw new RuntimeException("Class not found for type " + type);
		}
	}
	
	/**
	 * 
	 * @param type
	 * @return the valid parent types for this
	 */
	public static String[] getValidParentTypes(EntityType type) {
		EntityTypeMetadata metadata = getMetadata(type);
		return metadata.getValidParentTypes().toArray(new String[metadata.getValidParentTypes().size()]);
		
	}
	
	/**
	 * @param type 
	 * @return all of the aliases that can be used to look their entity type
	 */
	public static Set<String> getAllAliases(EntityType type){
		return new LinkedHashSet<String>(getMetadata(type).getAliases());
	}
	
	/**
	 * 
	 * @param type
	 * @return the default parent path for this type
	 */
	public static String getDefaultParentPath(EntityType type){
		return getMetadata(type).getDefaultParentPath();
	}
	
	/**
	 * 
	 * @param type 
	 * @return the EntityTypeMetadata object
	 */
	public static EntityTypeMetadata getMetadata(EntityType type) {
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return metadata;
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
	
	/**
	 *  
	 * @param child - the child type
	 * @param parentType - the parent type or null if the child has no parent
	 * @return true if parent is a valid parent type of child, false otherwise
	 */
	public static boolean isValidParentType(EntityType child, EntityType parentType) {
		return isValidTypeInList(parentType, getMetadata(child).getValidParentTypes());
	}
	
	private static boolean isValidTypeInList(EntityType type, List<String> validParentTypes) {
		String entityTypeClassName;
		if (type == null) {
			entityTypeClassName = "DEFAULT";
		} else {
			entityTypeClassName = getEntityTypeClassName(type);
		}
		for (String validParent: validParentTypes) {
			if(validParent.equals(entityTypeClassName)) return true;
		}
		// No match found
		return false;				
	}
	
	/**
	 * 
	 * @param clazz
	 * @return get the EntityType for an entity class
	 */
	public static EntityType getEntityTypeForClass(Class<? extends Entity> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		return getEntityTypeForClassName(clazz.getName());
	}
	
	/**
	 * 
	 * @param fullClassName
	 * @return get the EntityType for an Entity class name
	 */
	public static EntityType getEntityTypeForClassName(String fullClassName) {
		if (fullClassName == null){
			throw new IllegalArgumentException("Full class name cannot be null");
		}
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getClassName().equals(fullClassName)) {
				return metadata.getEntityType();
			}
		}
		throw new IllegalArgumentException("Unknown EntityType for class name: " + fullClassName);
	}
	
	/**
	 * 
	 * @return the full class name for this EntityType 
	 */
	public static String getEntityTypeClassName(EntityType type) {
		return getMetadata(type).getClassName();
	}

	/**
	 * 
	 * @param type
	 * @return name that can be shown to users
	 */
	public static String getDisplayName(EntityType type){
		return getMetadata(type).getDisplayName();
	}

	/**
	 * @param type
	 * @return True if the type defines a view, false otherwise
	 */
	public static boolean isViewType(EntityType type) {
		if (type == null) {
			throw new IllegalArgumentException("The type cannot be null");
		}

		return Stream.of(ViewEntityType.values()).anyMatch((viewType) -> viewType.name().equals(type.name()));
	}
}
