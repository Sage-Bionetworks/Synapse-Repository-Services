package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.repo.model.table.TableEntity;


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
	
	private static final EntityTypeMetadata project = buildMetadata(EntityType.project, Arrays.asList("DEFAULT"), Project.class, "Project");
	private static final EntityTypeMetadata file = buildMetadata(EntityType.file, Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), FileEntity.class, "File");
	private static final EntityTypeMetadata folder = buildMetadata(EntityType.folder, Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), Folder.class, "Folder");
	private static final EntityTypeMetadata table = buildMetadata(EntityType.table, Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), TableEntity.class, "Table");
	private static final EntityTypeMetadata link = buildMetadata(EntityType.link, Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), Link.class, "Link");
	private static final EntityTypeMetadata[] metadataArray = new EntityTypeMetadata[] {project, file, folder, table, link};
	
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
	@SuppressWarnings("unchecked")
	public static Class<? extends Entity> getClassForType(EntityType type) {
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				try {
					return (Class<? extends Entity>) Class.forName(metadata.getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Class not found for type " + type);
				}
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
	
	/**
	 * 
	 * @param type
	 * @return the valid parent types for this
	 */
	public static String[] getValidParentTypes(EntityType type) {
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return metadata.getValidParentTypes().toArray(new String[metadata.getValidParentTypes().size()]);
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
	
	/**
	 * @param type 
	 * @return all of the aliases that can be used to look their entity type
	 */
	public static Set<String> getAllAliases(EntityType type){
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return new LinkedHashSet<String>(metadata.getAliases());
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
	
	/**
	 * 
	 * @param type
	 * @return the default parent path for this type
	 */
	public static String getDefaultParentPath(EntityType type){
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return metadata.getDefaultParentPath();
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
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
	 * @param parent - the parent type
	 * @return true if parent is a valid parent type of child, false otherwise
	 */
	public static boolean isValidParentType(EntityType child, EntityType parent){
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == child) {
				return isValidTypeInList(parent, metadata.getValidParentTypes());
			}
		}
		throw new IllegalArgumentException("Type not supported: " + child);
	}
	
	private static boolean isValidTypeInList(EntityType type, List<String> typeUrlList) {
		String prefix;
		if(type == null){
			prefix = "DEFAULT";
		}else{
			prefix = getEntityTypeClassName(type);
		}
		for(String validParent:  typeUrlList){
			if(validParent.equals(prefix)) return true;
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
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return metadata.getClassName();
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
	
	/**
	 * 
	 * @param type
	 * @return name that can be shown to users
	 */
	public String getDisplayName(EntityType type){
		for (EntityTypeMetadata metadata : metadataArray) {
			if (metadata.getEntityType() == type) {
				return metadata.getDisplayName();
			}
		}
		throw new IllegalArgumentException("Type not supported: " + type);
	}
}
