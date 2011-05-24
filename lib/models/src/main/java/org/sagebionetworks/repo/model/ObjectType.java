package org.sagebionetworks.repo.model;


/**
 * These are the object types that can be stored as Nodes.
 * 
 * @author jmhill
 *
 */
public enum ObjectType {
	
	dataset(Dataset.class, (short)0, "/dataset"),
	layer(InputDataLayer.class, (short)1, "/layer"),
	layerlocation(LayerLocation.class, (short)2, "/location"),
	project(Project.class, (short)3, "/project"),
	layerpreview(StoredLayerPreview.class, (short)4, "/preview");
	
	private Class<? extends Base> clazz;
	private short id;
	private String urlPrefix;
	
	/**
	 * 
	 * @param clazz The DTO class that represents this type.
	 * @param id Give each type an ID that is used as the primary key for this type.
	 * @param urlPrefix The web-service URL that 
	 */
	ObjectType(Class<? extends Base> clazz, short id, String urlPrefix){
		this.clazz = clazz;
		this.id = id;
		this.urlPrefix = urlPrefix;
	}
	
	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Base> getClassForType(){
		return this.clazz;
	}
	
	public short getId(){
		return id;
	}
	/**
	 * The Url prefix used by this object
	 * @return
	 */
	public String getUrlPrefix(){
		return this.urlPrefix;
	}
	
	/**
	 * Lookup a type using its Primary key.
	 * @param id
	 * @return
	 */
	public static ObjectType getTypeForId(short id){
		ObjectType[] array  = ObjectType.values();
		for(ObjectType type: array){
			if(type.getId() == id) return type;
		}
		throw new IllegalArgumentException("Unkown id for ObjectType: "+id);
	}
	
	
	/**
	 * Lookup a type using the DTO class.
	 * @param clazz
	 * @return
	 */
	public static ObjectType getNodeTypeForClass(Class<? extends Base> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		ObjectType[] array  = ObjectType.values();
		for(ObjectType type: array){
			if(type.getClassForType() == clazz) return type;
		}
		throw new IllegalArgumentException("Unkown Object type: "+clazz.getName());
	}
	
	/**
	 * Lookup a type using that web-service base URL.
	 * @param url
	 * @return
	 */
	public static ObjectType getTypeForUrl(String url){
		if(url == null) throw new IllegalArgumentException("URL cannot be null");
		ObjectType[] array  = ObjectType.values();
		for(ObjectType type: array){
			int index = url.indexOf(type.getUrlPrefix());
			if(index >= 0) return type;
		}
		throw new IllegalArgumentException("Unkown Object type for URL: "+url);
	}
	
}
