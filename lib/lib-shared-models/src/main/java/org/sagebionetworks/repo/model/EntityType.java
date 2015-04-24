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
 * Defines each entity type.
 *  
 * @author jmhill
 *
 */
public enum EntityType {
	
	project(Arrays.asList("DEFAULT"), Project.class, "Project"),
	folder(Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), Folder.class, "Folder"),
	link(Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), Link.class, "Link"),
	file(Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), FileEntity.class, "File"),
	table(Arrays.asList("DEFAULT",Project.class.getName(), Folder.class.getName()), TableEntity.class, "Table");
	
	EntityTypeMetadata metadata;
	Class<? extends Entity> clazz;
	String displayName;
	
	EntityType(List<String> validParentTypes, Class<? extends Entity> clazz, String displayName){
		this.metadata = new EntityTypeMetadata();
		this.metadata.setAliases(Arrays.asList(name(),"entity"));
		this.metadata.setDefaultParentPath("/root");
		this.metadata.setEntityType(clazz.getName());
		this.metadata.setValidParentTypes(validParentTypes);
		this.metadata.setName(this.name());
		this.clazz = clazz;
		this.displayName = displayName;
	}
	
	/**
	 * Get the entity Registry
	 * @return
	 */
	public static EntityRegistry getEntityRegistry(){
		EntityRegistry reg = new EntityRegistry();
		reg.setEntityTypes(new LinkedList<EntityTypeMetadata>());
		for(EntityType type: values()){
			reg.getEntityTypes().add(type.metadata);
		}
		return reg;
	}

	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Entity> getClassForType(){
		return this.clazz;
	}

	/***
	 * These are the valid parent types for this ObjectType.
	 * @return
	 */
	public String[] getValidParentTypes(){
		return this.metadata.getValidParentTypes().toArray(new String[this.metadata.getValidParentTypes().size()]);
	}
	
	
	/**
	 * Get all of the aliases that can be used to look then entity type.
	 * @return
	 */
	public Set<String> getAllAliases(){
		return new LinkedHashSet<String>(metadata.getAliases());
	}

	public String getDefaultParentPath(){
		return metadata.getDefaultParentPath();
	}
		
	/**
	 * The EntityTypeMetadata object
	 * @return
	 */
	public EntityTypeMetadata getMetadata() {
		return metadata;
	}

	/**
	 *  
	 * @param type, if null then the object must support a null parent.
	 * @return
	 */
	public boolean isValidParentType(EntityType type){
		if(type == null){
			return true;
		}
		return isValidTypeInList(type, this.getValidParentTypes());
	}
	
	private boolean isValidTypeInList(EntityType type, String[] typeUrlList) {
		String prefix;
		if(type == null){
			prefix = "DEFAULT";
		}else{
			prefix = type.getEntityTypeClassName();
		}
		for(String validParent:  typeUrlList){
			if(validParent.equals(prefix)) return true;
		}
		// No match found
		return false;				
	}

	/**
	 * Given an entity class, get the EntityType.
	 * @param clazz
	 * @return
	 */
	public static EntityType getEntityTypeForClass(Class<? extends Entity> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		return getEntityTypeForClassName(clazz.getName());
	}

	/**
	 * Get the full class name for this EntityType.
	 * @return
	 */
	public String getEntityTypeClassName() {
		return metadata.getEntityType();
	}

	/**
	 * Given an Entity class name, get the EntityType.
	 * 
	 * @param url
	 * @return
	 */
	public static EntityType getEntityTypeForClassName(String fullClassName) {
		if (fullClassName == null){
			throw new IllegalArgumentException("Full class name cannot be null");
		}
		EntityType[] array = EntityType.values();
		for (EntityType type : array) {
			if (type.clazz.getName().equals(fullClassName))
				return type;
		}
		throw new IllegalArgumentException(
				"Unknown Entity type for class name: " + fullClassName);
	}
	
	/**
	 * Name that can be shown to users.
	 * @return
	 */
	public String getDisplayName(){
		return displayName;
	}

}
