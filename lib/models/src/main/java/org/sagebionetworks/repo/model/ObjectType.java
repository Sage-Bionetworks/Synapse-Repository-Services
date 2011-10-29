package org.sagebionetworks.repo.model;


/**
 * These are the object types that can be stored as Nodes.
 * 
 * @author jmhill
 *
 */
public enum ObjectType {

	dataset			(Dataset.class, 	(short)0, PrefixConst.DATASET,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.PROJECT}),
	layer			(Layer.class, 		(short)1, PrefixConst.LAYER,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DATASET}),
	location		(Location.class, 	(short)2, PrefixConst.LOCATION,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DATASET, PrefixConst.LAYER, PrefixConst.CODE}),
	project			(Project.class, 	(short)3, PrefixConst.PROJECT,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.FOLDER, PrefixConst.PROJECT, PrefixConst.DEFAULT}),
	preview			(Preview.class, 	(short)4, PrefixConst.PREVIEW,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.LAYER}),
	eula			(Eula.class,		(short)5, PrefixConst.EULA,			NodeConstants.EULA_FOLDER_PATH,		new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	agreement		(Agreement.class,	(short)6, PrefixConst.AGREEMENT,	NodeConstants.AGREEMENT_FOLDER_PATH,new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	folder			(Folder.class,		(short)7, PrefixConst.FOLDER,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	analysis		(Analysis.class, 	(short)8, PrefixConst.ANALYSIS,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.PROJECT}),
	step			(Step.class, 		(short)9, PrefixConst.STEP,			NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.FOLDER, PrefixConst.ANALYSIS, PrefixConst.DEFAULT}),
	code			(Code.class,			(short)10, PrefixConst.CODE,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.PROJECT});
	
	private Class<? extends Nodeable> clazz;
	private short id;
	private String urlPrefix;
	private String[] validParents;
	private String defaultParenPath;
	
	/**
	 * 
	 * @param clazz The DTO class that represents this type.
	 * @param id Give each type an ID that is used as the primary key for this type.
	 * @param urlPrefix The web-service URL that 
	 */
	ObjectType(Class<? extends Nodeable> clazz, short id, String urlPrefix, String defaultParentPath, String[] validParents){
		this.clazz = clazz;
		this.id = id;
		this.urlPrefix = urlPrefix;
		this.validParents = validParents;
		this.defaultParenPath = defaultParentPath;
	}
	
	/**
	 * What is the class that goes with this type?
	 * @return
	 */
	public Class<? extends Nodeable> getClassForType(){
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
	 * 
	 * @param type, if null then the object must support a null parent.
	 * @return
	 */
	public boolean isValidParentType(ObjectType type){
		String prefix;
		if(type == null){
			prefix = PrefixConst.DEFAULT;
		}else{
			prefix = type.getUrlPrefix();
		}
		for(String validParent:  validParents){
			if(validParent.equals(prefix)) return true;
		}
		// No match found
		return false;
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
		throw new IllegalArgumentException("Unknown id for ObjectType: "+id);
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
		throw new IllegalArgumentException("Unknown Object type: "+clazz.getName());
	}
	
	/**
	 * Get the first type that occurs in a given url.
	 * @param url
	 * @return
	 */
	public static ObjectType getFirstTypeInUrl(String url){
		if(url == null) throw new IllegalArgumentException("URL cannot be null");
		ObjectType[] array  = ObjectType.values();
		int minIndex = Integer.MAX_VALUE;
		ObjectType minType = null;
		for(ObjectType type: array){
			int index = url.indexOf(type.getUrlPrefix());
			if(index < minIndex && index >= 0){
				minIndex = index;
				minType = type;
			}
		}
		if(minType != null) return minType;
		throw new IllegalArgumentException("Unknown Object type for URL: "+url);
	}
	
	/**
	 * Get the last type that occurs in a given url.
	 * @param url
	 * @return
	 */
	public static ObjectType getLastTypeInUrl(String url){
		if(url == null) throw new IllegalArgumentException("URL cannot be null");
		ObjectType[] array  = ObjectType.values();
		int maxIndex = -1;
		ObjectType maxType = null;
		for(ObjectType type: array){
			int index = url.lastIndexOf(type.getUrlPrefix());
			if(index > maxIndex){
				maxIndex = index;
				maxType = type;
			}
		}
		if(maxType != null) return maxType;
		throw new IllegalArgumentException("Unknown Object type for URL: "+url);
	}
	
}
