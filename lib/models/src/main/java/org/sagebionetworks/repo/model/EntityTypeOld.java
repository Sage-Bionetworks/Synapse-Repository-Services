package org.sagebionetworks.repo.model;


/**
 * These are the object types that can be stored as Nodes.
 * 
 * @author jmhill
 *
 */
public enum EntityTypeOld {

	dataset			(DatasetOld.class, 	(short)0, PrefixConst.DATASET,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.PROJECT}),
	layer			(LayerOld.class, 		(short)1, PrefixConst.LAYER,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DATASET}),
	location		(LocationOld.class, 	(short)2, PrefixConst.LOCATION,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DATASET, PrefixConst.LAYER}),
	project			(ProjectOld.class, 	(short)3, PrefixConst.PROJECT,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.FOLDER, PrefixConst.PROJECT, PrefixConst.DEFAULT}),
	preview			(PreviewOld.class, 	(short)4, PrefixConst.PREVIEW,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.LAYER}),
	eula			(EulaOld.class,		(short)5, PrefixConst.EULA,			NodeConstants.EULA_FOLDER_PATH,		new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	agreement		(AgreementOld.class,	(short)6, PrefixConst.AGREEMENT,	NodeConstants.AGREEMENT_FOLDER_PATH,new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	folder			(FolderOld.class,		(short)7, PrefixConst.FOLDER,		NodeConstants.ROOT_FOLDER_PATH,		new String[]{PrefixConst.DEFAULT, PrefixConst.FOLDER}),
	analysis		(AnalysisOld.class, 	(short)8, PrefixConst.ANALYSIS,		NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.PROJECT}),
	step			(StepOld.class, 		(short)9, PrefixConst.STEP,			NodeConstants.ROOT_FOLDER_PATH, 	new String[]{PrefixConst.FOLDER, PrefixConst.ANALYSIS, PrefixConst.DEFAULT});
	
	private Class<? extends NodeableOld> clazz;
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
	EntityTypeOld(Class<? extends NodeableOld> clazz, short id, String urlPrefix, String defaultParentPath, String[] validParents){
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
	public Class<? extends NodeableOld> getClassForType(){
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
	public boolean isValidParentType(EntityType type){
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
	public static EntityType getTypeForId(short id){
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.getId() == id) return type;
		}
		throw new IllegalArgumentException("Unknown id for ObjectType: "+id);
	}
	
	
	/**
	 * Lookup a type using the DTO class.
	 * @param clazz
	 * @return
	 */
	public static EntityType getNodeTypeForClass(Class<? extends Base> clazz){
		if(clazz == null) throw new IllegalArgumentException("Clazz cannot be null");
		EntityType[] array  = EntityType.values();
		for(EntityType type: array){
			if(type.getClassForType() == clazz) return type;
		}
		throw new IllegalArgumentException("Unknown Object type: "+clazz.getName());
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
		throw new IllegalArgumentException("Unknown Object type for URL: "+url);
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
		throw new IllegalArgumentException("Unknown Object type for URL: "+url);
	}
	
}
