package org.sagebionetworks.repo.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * This is metadata about an entity type.  It is patterned after an enumeration.  The values are initialized at
 * startup 
 *  
 * @author jmhill
 *
 */
public class EntityType {	
	
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType dataset = new EntityType("org.sagebionetworks.repo.model.Study");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType layer = new EntityType("org.sagebionetworks.repo.model.Data");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType project = new EntityType("org.sagebionetworks.repo.model.Project");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType folder = new EntityType("org.sagebionetworks.repo.model.Folder");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType step = new EntityType("org.sagebionetworks.repo.model.Step");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType preview = new EntityType("org.sagebionetworks.repo.model.Preview");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType code = new EntityType("org.sagebionetworks.repo.model.Code");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType analysis = new EntityType("org.sagebionetworks.repo.model.Analysis");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType link = new EntityType("org.sagebionetworks.repo.model.Link");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType phenotypedata = new EntityType("org.sagebionetworks.repo.model.PhenotypeData");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType genotypedata = new EntityType("org.sagebionetworks.repo.model.GenotypeData");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType expressiondata = new EntityType("org.sagebionetworks.repo.model.ExpressionData");
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType unknown = new EntityType("org.sagebionetworks.repo.model.Unknown");

	/**
	 * The JSON file that contains the register data.
	 */
	public static final String REGISTER_JSON_FILE_NAME = "Register.json";
	
	/*
	 * Static 
	 */
	private static EntityType[] values;
	private static EntityRegistry registry;
	static{
		// Load the Register from the classpath
		try{
			ClassLoader classLoader = EntityType.class.getClassLoader();
			InputStream in = classLoader.getResourceAsStream(REGISTER_JSON_FILE_NAME);
			if(in == null) throw new IllegalStateException("Cannot find the "+REGISTER_JSON_FILE_NAME+" file on the classpath");
			String jsonString = readToString(in);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonString);
			// Get the model object
			registry = new EntityRegistry(adapter);
			List<EntityTypeMetadata> typeList = registry.getEntityTypes();
			values = new EntityType[typeList.size()];
						
			// Build up the values.
			for(short i=0; i<typeList.size(); i++){
				EntityTypeMetadata meta = typeList.get(i);				
				EntityType type;				
				if(dataset.getEntityType().equals(meta.getEntityType())){
					type = dataset;
				}else if(layer.getEntityType().equals(meta.getEntityType())){
					type = layer;
				}else if(project.getEntityType().equals(meta.getEntityType())){
					type = project;
				}else if(folder.getEntityType().equals(meta.getEntityType())){
					type = folder;
				}else if(step.getEntityType().equals(meta.getEntityType())){
					type = step;
				}else if(preview.getEntityType().equals(meta.getEntityType())){
					type = preview;
				}else if(code.getEntityType().equals(meta.getEntityType())){
					type = code;
				}else if(analysis.getEntityType().equals(meta.getEntityType())){
					type = analysis;
				}else if(link.getEntityType().equals(meta.getEntityType())){
					type = link;
				}else if(phenotypedata.getEntityType().equals(meta.getEntityType())){
					type = phenotypedata;
				}else if(genotypedata.getEntityType().equals(meta.getEntityType())){
					type = genotypedata;
				}else if(expressiondata.getEntityType().equals(meta.getEntityType())){
					type = expressiondata;
				}else{
					type = new EntityType(meta.getEntityType());
				}				
				
				values[i] = type;
				type.id = i;
				type.clazz = (Class<? extends Entity>) Class.forName(meta.getEntityType());
				type.validParents = meta.getValidParentTypes().toArray(new String[meta.getValidParentTypes().size()]);
				type.defaultParenPath = meta.getDefaultParentPath();
				type.name = meta.getName();
				type.metadata = meta;				
			}
			
			// calculate children
			Map<String, Set<String>> typeToChildTypes = new HashMap<String, Set<String>>();
			for(EntityType type : values) {
				for(String parentPrefix : type.validParents) {					
					if(!typeToChildTypes.containsKey(parentPrefix)) {
						typeToChildTypes.put(parentPrefix, new HashSet<String>());
					}
					// add this type to its parent
					typeToChildTypes.get(parentPrefix).add(type.getEntityType());
				}
			}
			for(EntityType type : values) {
				if(typeToChildTypes.containsKey(type.getEntityType())) {
					Set<String> children = typeToChildTypes.get(type.getEntityType());
					type.validChildren = children.toArray(new String[children.size()]);
				}
			}
		}catch(Exception e){
			// Convert to a runtime
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the entity Registry
	 * @return
	 */
	public static EntityRegistry getEntityRegistry(){
		return registry;
	}
	
	/**
	 * Read an input stream into a string.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
	/*
	 * Non Static
	 */	
	private Class<? extends Entity> clazz;
	private String entityType;
	private short id;
	private String[] validParents;
	private String defaultParenPath;
	private String name;
	private EntityTypeMetadata metadata;
	private String[] validChildren;
	
	/**
	 * Do not make this public
	 */
	EntityType(String entityType){
		if(entityType == null) throw new IllegalArgumentException("EntityType cannot be null");
		this.entityType = entityType;
	}
	
	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Entity> getClassForType(){
		return this.clazz;
	}
	
	public short getId(){
		return id;
	}
	
	/**
	 * Get the name of this entity
	 * @return
	 */
	public String name(){
		return name;
	}

	/***
	 * These are the valid parent types for this ObjectType.
	 * @return
	 */
	public String[] getValidParentTypes(){
		return validParents;
	}
	
	/***
	 * These are the valid child types for this ObjectType.
	 * @return
	 */
	public String[] getValidChildTypes(){
		return validChildren;
	}
	
	/**
	 * Get all of the aliases that can be used to look then entity type.
	 * @return
	 */
	public Set<String> getAllAliases(){
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		if(this.metadata != null){
			if(this.metadata.getAliases() != null){
				for(String alias: this.metadata.getAliases()){
					// Add all values as lower case.
					set.add(alias.toLowerCase());
				}
			}
		}
		// Add all of the types this Class implements
		addAllInterfacesRecursive(set, this.clazz);
		return set;
	}
	
	private static void addAllInterfacesRecursive(HashSet<String> set, Class<?> clazz){
		set.add(clazz.getSimpleName().toLowerCase());
		// Add all of the types this Class implements
		Class<?>[] interfaces = clazz.getInterfaces();
		if(interfaces != null){
			for(Class<?> interClass: interfaces){
				set.add(interClass.getSimpleName().toLowerCase());
				// Add all this interfances interfaces
				addAllInterfacesRecursive(set, interClass);
			}
		}
	}
	
	public String getDefaultParentPath(){
		return defaultParenPath;
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
		return isValidTypeInList(type, validParents);
	}

	/**
	 * Is this a valid child
	 * @param type
	 * @return
	 */
	public boolean isValidChildType(EntityType type){
		return isValidTypeInList(type, validChildren);
	}

	private boolean isValidTypeInList(EntityType type, String[] typeUrlList) {
		String prefix;
		if(type == null){
			prefix = "DEFAULT";
		}else{
			prefix = type.getEntityType();
		}
		for(String validParent:  typeUrlList){
			if(validParent.equals(prefix)) return true;
		}
		// No match found
		return false;				
	}
	
	public static EntityType[] values(){
		return values;
	}
	
	public static EntityType valueOf(String name){
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.name().equals(name)) return type;
		}
		throw new IllegalArgumentException("Unknown name for EntityType: "+name);
	}
	
	/**
	 * Lookup a type using its Primary key.
	 * @param id
	 * @return
	 */
	public static EntityType getTypeForId(short id){
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.getId() == id) return type;
		}
		throw new IllegalArgumentException("Unknown id for EntityType: "+id);
	}
	
	
	/**
	 * Lookup a type using the DTO class.
	 * @param clazz
	 * @return
	 */
	public static EntityType getNodeTypeForClass(Class<? extends JSONEntity> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.getClassForType() == clazz) return type;
		}
		throw new IllegalArgumentException("Unknown Entity type: "+clazz.getName());
	}
	
	/**
	 * Get the first type that occurs in a given url.
	 * @param url
	 * @return
	 */
	public static EntityType getEntityType(String entityType){
		if(entityType == null) throw new IllegalArgumentException("URL cannot be null");
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.clazz.getName().equals(entityType)) return type;
		}
		throw new IllegalArgumentException("Unknown Entity type for entityType: "+entityType);
	}


	@Override
	public String toString() {
		return name;
	}

	public String getEntityType() {
		return this.entityType;
	}
	
}
