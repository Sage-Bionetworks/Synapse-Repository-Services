package org.sagebionetworks.repo.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

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
	public static final EntityType dataset = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType layer = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType project = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType agreement = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType folder = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType location = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType eula = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType step = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType preview = new EntityType();
	@Deprecated // Only added for backwards compatibility.
	public static final EntityType code = new EntityType();
	/**
	 * The JSON file that contains the register data.
	 */
	public static final String REGISTER_JSON_FILE_NAME = "Register.json";
	
	/**
	 * 
	 */
	private static EntityType[] values;
	static{
		// Load the Register from the classpath
		try{
			ClassLoader classLoader = EntityType.class.getClassLoader();
			InputStream in = classLoader.getResourceAsStream(REGISTER_JSON_FILE_NAME);
			if(in == null) throw new IllegalStateException("Cannot find the "+REGISTER_JSON_FILE_NAME+" file on the classpath");
			String jsonString = readToString(in);
			JSONObjectAdapter adapter = JSONObjectAdapterImpl.createAdapterFromJSONString(jsonString);
			// Get the model object
			EntityRegistry registry = new EntityRegistry(adapter);
			List<EntityTypeMetadata> typeList = registry.getEntityTypes();
			values = new EntityType[typeList.size()];
			// Build up the values.
			for(short i=0; i<typeList.size(); i++){
				EntityTypeMetadata meta = typeList.get(i);				
				EntityType type = null;
				if(PrefixConst.DATASET.equals(meta.getUrlPrefix())){
					type = dataset;
				}else if(PrefixConst.LAYER.equals(meta.getUrlPrefix())){
					type = layer;
				}else if(PrefixConst.PROJECT.equals(meta.getUrlPrefix())){
					type = project;
				}else if(PrefixConst.AGREEMENT.equals(meta.getUrlPrefix())){
					type = agreement;
				}else if(PrefixConst.FOLDER.equals(meta.getUrlPrefix())){
					type = folder;
				}else if(PrefixConst.LOCATION.equals(meta.getUrlPrefix())){
					type = location;
				}else if(PrefixConst.EULA.equals(meta.getUrlPrefix())){
					type = eula;
				}else if(PrefixConst.STEP.equals(meta.getUrlPrefix())){
					type = step;
				}else if(PrefixConst.PREVIEW.equals(meta.getUrlPrefix())){
					type = preview;
				}else if(PrefixConst.CODE.equals(meta.getUrlPrefix())){
					type = code;
				}else{
					type = new EntityType();
				}
				values[i] = type;
				type.id = i;
				type.clazz = (Class<? extends Entity>) Class.forName(meta.getClassName());
				type.urlPrefix = meta.getUrlPrefix();
				type.validParents = meta.getValidParentTypes().toArray(new String[meta.getValidParentTypes().size()]);
				type.defaultParenPath = meta.getDefaultParentPath();
				type.name = meta.getName();
				type.metadata = meta;

			}
		}catch(Exception e){
			// Convert to a runtime
			throw new RuntimeException(e);
		}
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
	
	private Class<? extends Entity> clazz;
	private short id;
	private String urlPrefix;
	private String[] validParents;
	private String defaultParenPath;
	private String name;
	private EntityTypeMetadata metadata; 
	
	/**
	 * Do not make this public
	 */
	EntityType(){
		
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
	/**
	 * The Url prefix used by this object
	 * @return
	 */
	public String getUrlPrefix(){
		return this.urlPrefix;
	}

	/***
	 * These are the valid parent types for this ObjectType.
	 * @return
	 */
	public String[] getValidParentTypes(){
		return validParents;
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
		String prefix;
		if(type == null){
			prefix = "DEFAULT";
		}else{
			prefix = type.getUrlPrefix();
		}
		for(String validParent:  validParents){
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
	public static EntityType getFirstTypeInUrl(String url){
		if(url == null) throw new IllegalArgumentException("URL cannot be null");
		EntityType[] array  = EntityType.values();
		int minIndex = Integer.MAX_VALUE;
		EntityType minType = null;
		for(EntityType type: array){
			int index = url.indexOf(type.getUrlPrefix());
			if(index < minIndex && index >= 0){
				minIndex = index;
				minType = type;
			}
		}
		if(minType != null) return minType;
		throw new IllegalArgumentException("Unknown Entity type for URL: "+url);
	}
	
	/**
	 * Get the last type that occurs in a given url.
	 * @param url
	 * @return
	 */
	public static EntityType getLastTypeInUrl(String url){
		if(url == null) throw new IllegalArgumentException("URL cannot be null");
		EntityType[] array  = EntityType.values();
		int maxIndex = -1;
		EntityType maxType = null;
		for(EntityType type: array){
			int index = url.lastIndexOf(type.getUrlPrefix());
			if(index > maxIndex){
				maxIndex = index;
				maxType = type;
			}
		}
		if(maxType != null) return maxType;
		throw new IllegalArgumentException("Unknown Entity type for URL: "+url);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime
				* result
				+ ((defaultParenPath == null) ? 0 : defaultParenPath.hashCode());
		result = prime * result + id;
		result = prime * result
				+ ((urlPrefix == null) ? 0 : urlPrefix.hashCode());
		result = prime * result + Arrays.hashCode(validParents);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityType other = (EntityType) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (defaultParenPath == null) {
			if (other.defaultParenPath != null)
				return false;
		} else if (!defaultParenPath.equals(other.defaultParenPath))
			return false;
		if (id != other.id)
			return false;
		if (urlPrefix == null) {
			if (other.urlPrefix != null)
				return false;
		} else if (!urlPrefix.equals(other.urlPrefix))
			return false;
		if (!Arrays.equals(validParents, other.validParents))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
	
}
