package org.sagebionetworks.repo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleInstanceFactory;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Low-level bundle to transport an Entity and related data objects between the 
 * Synapse platform and external clients.
 */
public class EntityBundle implements JSONEntity, Serializable {
	
	/**
	 * Masks for requesting what should be included in the bundle.
	 */
	public static int ENTITY 		      		= 0x1;
	public static int ANNOTATIONS	      		= 0x2;
	public static int PERMISSIONS	     		= 0x4;
	public static int ENTITY_PATH	      		= 0x8;
	public static int HAS_CHILDREN				= 0x20;
	public static int ACL						= 0x40;
	public static int FILE_HANDLES				= 0x800;
	public static int TABLE_DATA				= 0x1000;
	public static int ROOT_WIKI_ID				= 0x2000;
	public static int BENEFACTOR_ACL			= 0x4000;
	public static int DOI						= 0x8000;
	public static int FILE_NAME					= 0x10000;
	public static int THREAD_COUNT				= 0x20000;
	public static int RESTRICTION_INFORMATION	= 0x40000;
	
	private static FileHandleInstanceFactory fileHandleInstanceFactory = new FileHandleInstanceFactory();
	private static EntityInstanceFactory entityInstanceFactory = new EntityInstanceFactory();
	
	public static final String JSON_ENTITY = "entity";
	public static final String JSON_ENTITY_TYPE = "entityType";
	public static final String JSON_ANNOTATIONS = "annotations";
	public static final String JSON_PERMISSIONS = "permissions";
	public static final String JSON_PATH = "path";
	public static final String JSON_REFERENCED_BY = "referencedBy";
	public static final String JSON_HAS_CHILDREN 	= "hasChildren";

	public static final String JSON_ACL = "accessControlList";
	public static final String JSON_BENEFACTOR_ACL = "benefactorAcl";
	public static final String JSON_ACCESS_REQUIREMENTS = "accessRequirements";
	public static final String JSON_UNMET_ACCESS_REQUIREMENTS = "unmetAccessRequirements";
	public static final String JSON_FILE_HANDLES = "fileHandles";
	public static final String JSON_TABLE_DATA = "tableBundle";
	public static final String JSON_ROOT_WIKI_ID = "rootWikiId";
	public static final String JSON_DOI = "doi";
	public static final String JSON_DOI_ASSOCIATION = "doiAssociation";
	public static final String JSON_FILE_NAME = "fileName";
	public static final String JSON_THREAD_COUNT = "threadCount";
	public static final String JSON_RESTRICTION_INFORMATION = "restrictionInformation";
	
	private Entity entity;
	private String entityType;
	private Annotations annotations;
	private UserEntityPermissions permissions;
	private EntityPath path;
	private List<EntityHeader> referencedBy;
	private Boolean hasChildren;
	private AccessControlList acl;
	private List<AccessRequirement> accessRequirements;
	private List<AccessRequirement> unmetAccessRequirements;
	private List<FileHandle> fileHandles;
	private TableBundle tableBundle;
	private String rootWikiId;
	private AccessControlList benefactorAcl;
	private Doi doi;
	private DoiAssociation doiAssociation;
	private String fileName;
	private Long threadCount;
	private RestrictionInformationResponse restrictionInformation;

	/**
	 * Create a new EntityBundle
	 */
	public EntityBundle() {}
	
	/**
	 * Create a new EntityBundle and initialize from a JSONObjectAdapter.
	 * 
	 * @param initializeFrom
	 * @throws JSONObjectAdapterException
	 */
	public EntityBundle(JSONObjectAdapter initializeFrom) throws JSONObjectAdapterException {
		this();
		initializeFromJSONObject(initializeFrom);
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(
			JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom == null) {
            throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
        }	
		if (toInitFrom.has(JSON_ENTITY)) {
			entityType = toInitFrom.getString(JSON_ENTITY_TYPE);
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ENTITY);
			entity = entityInstanceFactory.newInstance(entityType);
			entity.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ANNOTATIONS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ANNOTATIONS);
			if (annotations == null)
				annotations = new Annotations();
			annotations.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_PERMISSIONS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_PERMISSIONS);
			if (permissions == null)
				permissions = new UserEntityPermissions();
			permissions.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_PATH)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_PATH);
			if (path == null)
				path = new EntityPath();
			path.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_REFERENCED_BY)) {
			JSONArrayAdapter a = (JSONArrayAdapter) toInitFrom.getJSONArray(JSON_REFERENCED_BY);
			referencedBy = new ArrayList<EntityHeader>();
			for (int i=0; i<a.length(); i++) {
				JSONObjectAdapter joa = (JSONObjectAdapter)a.getJSONObject(i);
				EntityHeader header  = new EntityHeader();
				header.initializeFromJSONObject(joa);
				referencedBy.add(header);
			}
		}
		if (toInitFrom.has(JSON_HAS_CHILDREN)) {
			hasChildren = toInitFrom.getBoolean(JSON_HAS_CHILDREN);
		}
		if (toInitFrom.has(JSON_ACL)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ACL);
			if (acl == null)
				acl = new AccessControlList();
			acl.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_BENEFACTOR_ACL)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_BENEFACTOR_ACL);
			if (benefactorAcl == null){
				benefactorAcl = new AccessControlList();
			}
			benefactorAcl.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ACCESS_REQUIREMENTS)) {
			JSONArrayAdapter a = (JSONArrayAdapter) toInitFrom.getJSONArray(JSON_ACCESS_REQUIREMENTS);
			accessRequirements = new ArrayList<AccessRequirement>();
			for (int i=0; i<a.length(); i++) {
				JSONObjectAdapter joa = (JSONObjectAdapter)a.getJSONObject(i);
				String type = joa.getString("concreteType");
				AccessRequirement ar = AccessRequirementInstanceFactory.singleton().newInstance(type);
				ar.initializeFromJSONObject(joa);
				accessRequirements.add(ar);
			}
		}
		if (toInitFrom.has(JSON_UNMET_ACCESS_REQUIREMENTS)) {
			JSONArrayAdapter a = (JSONArrayAdapter) toInitFrom.getJSONArray(JSON_UNMET_ACCESS_REQUIREMENTS);
			unmetAccessRequirements = new ArrayList<AccessRequirement>();
			for (int i=0; i<a.length(); i++) {
				JSONObjectAdapter joa = (JSONObjectAdapter)a.getJSONObject(i);
				String type = joa.getString("concreteType");
				AccessRequirement ar = AccessRequirementInstanceFactory.singleton().newInstance(type);
				ar.initializeFromJSONObject(joa);
				unmetAccessRequirements.add(ar);
			}
		}
		if (toInitFrom.has(JSON_FILE_HANDLES)) {
			JSONArrayAdapter a = (JSONArrayAdapter) toInitFrom.getJSONArray(JSON_FILE_HANDLES);
			fileHandles = new ArrayList<FileHandle>();
			for (int i=0; i<a.length(); i++) {
				JSONObjectAdapter joa = (JSONObjectAdapter)a.getJSONObject(i);
				String type = joa.getString("concreteType");
				FileHandle handle = fileHandleInstanceFactory.newInstance(type);
				handle.initializeFromJSONObject(joa);
				fileHandles.add(handle);
			}
		}
		if(toInitFrom.has(JSON_TABLE_DATA)){
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_TABLE_DATA);
			if (tableBundle == null)
				tableBundle = new TableBundle();
			tableBundle.initializeFromJSONObject(joa);
		}
		if(toInitFrom.has(JSON_ROOT_WIKI_ID)){
			rootWikiId = toInitFrom.getString(JSON_ROOT_WIKI_ID);
		}
		if(toInitFrom.has(JSON_DOI)){
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_DOI);
			if (doi == null) 
				doi = new Doi();
			doi.initializeFromJSONObject(joa);
		}
		if(toInitFrom.has(JSON_DOI_ASSOCIATION)){
			JSONObjectAdapter joa = toInitFrom.getJSONObject(JSON_DOI_ASSOCIATION);
			if (doiAssociation == null)
				doiAssociation = new DoiAssociation();
			doiAssociation.initializeFromJSONObject(joa);
		}
		if(toInitFrom.has(JSON_FILE_NAME)){
			fileName = toInitFrom.getString(JSON_FILE_NAME);
		}
		if(toInitFrom.has(JSON_THREAD_COUNT)) {
			threadCount = toInitFrom.getLong(JSON_THREAD_COUNT);
		}
		if(toInitFrom.has(JSON_RESTRICTION_INFORMATION)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_RESTRICTION_INFORMATION);
			if (restrictionInformation == null) 
				restrictionInformation = new RestrictionInformationResponse();
			restrictionInformation.initializeFromJSONObject(joa);
		}
		return toInitFrom;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo)
			throws JSONObjectAdapterException {
		if (writeTo == null) {
		        throw new IllegalArgumentException("JSONObjectAdapter cannot be null");
		}
		if (entity != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			entity.writeToJSONObject(joa);
			writeTo.put(JSON_ENTITY, joa);
			writeTo.put(JSON_ENTITY_TYPE, entityType);
		}
		if (annotations != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			annotations.writeToJSONObject(joa);
			writeTo.put(JSON_ANNOTATIONS, joa);
		}
		if (permissions != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			permissions.writeToJSONObject(joa);
			writeTo.put(JSON_PERMISSIONS, joa);
		}
		if (path != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			path.writeToJSONObject(joa);
			writeTo.put(JSON_PATH, joa);
		}
		if (referencedBy != null) {
			JSONArrayAdapter arArray = writeTo.createNewArray();
			for (int i=0; i<referencedBy.size(); i++) {
				JSONObjectAdapter joa = arArray.createNew();
				referencedBy.get(i).writeToJSONObject(joa);
				arArray.put(i, joa);	
			}
			writeTo.put(JSON_REFERENCED_BY, arArray);
		}
		if (hasChildren != null) {
			writeTo.put(JSON_HAS_CHILDREN, hasChildren);
		}
		if (acl != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			acl.writeToJSONObject(joa);
			writeTo.put(JSON_ACL, joa);
		}
		if(benefactorAcl != null){
			JSONObjectAdapter joa = writeTo.createNew();
			benefactorAcl.writeToJSONObject(joa);
			writeTo.put(JSON_BENEFACTOR_ACL, joa);
		}
		if (accessRequirements != null) {
			JSONArrayAdapter arArray = writeTo.createNewArray();
			for (int i=0; i<accessRequirements.size(); i++) {
				JSONObjectAdapter joa = arArray.createNew();
				accessRequirements.get(i).writeToJSONObject(joa);
				arArray.put(i, joa);	
			}
			writeTo.put(JSON_ACCESS_REQUIREMENTS, arArray);
		}
		if (unmetAccessRequirements != null) {
			JSONArrayAdapter arArray = writeTo.createNewArray();
			for (int i=0; i<unmetAccessRequirements.size(); i++) {
				JSONObjectAdapter joa = arArray.createNew();
				unmetAccessRequirements.get(i).writeToJSONObject(joa);
				arArray.put(i, joa);	
			}
			writeTo.put(JSON_UNMET_ACCESS_REQUIREMENTS, arArray);
		}
		if (fileHandles != null) {
			JSONArrayAdapter arArray = writeTo.createNewArray();
			for (int i=0; i<fileHandles.size(); i++) {
				JSONObjectAdapter joa = arArray.createNew();
				fileHandles.get(i).writeToJSONObject(joa);
				arArray.put(i, joa);	
			}
			writeTo.put(JSON_FILE_HANDLES, arArray);
		}
		if(tableBundle != null){
			JSONObjectAdapter joa = writeTo.createNew();
			tableBundle.writeToJSONObject(joa);
			writeTo.put(JSON_TABLE_DATA, joa);
		}
		if(rootWikiId != null){
			writeTo.put(JSON_ROOT_WIKI_ID, rootWikiId);
		}
		if (doi != null){
			JSONObjectAdapter joa = writeTo.createNew();
			doi.writeToJSONObject(joa);
			writeTo.put(JSON_DOI, joa);
		}
		if (doiAssociation != null){
			JSONObjectAdapter joa = writeTo.createNew();
			doiAssociation.writeToJSONObject(joa);
			writeTo.put(JSON_DOI_ASSOCIATION, joa);
		}
		if (fileName != null){
			writeTo.put(JSON_FILE_NAME, fileName);
		}
		if (threadCount != null) {
			writeTo.put(JSON_THREAD_COUNT, threadCount);
		}
		if (restrictionInformation != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			restrictionInformation.writeToJSONObject(joa);
			writeTo.put(JSON_RESTRICTION_INFORMATION, joa);
		}
		return writeTo;
	}

	/**
	 * Get the Entity in this bundle.
	 */
	public Entity getEntity() {
		return entity;
	}

	/**
	 * Set the Entity in this bundle.
	 */
	public void setEntity(Entity entity) {
		this.entity = entity;
		if(entity != null) {
			String s = entity.getClass().toString();
			// trim "Class " from the above String
			this.entityType = s.substring(s.lastIndexOf(" ") + 1);
		} else {
			this.entityType = null;
		}
	}

	public String getEntityType(){
		return this.entityType;
	}

	/**
	 * Get the Annotations for the Entity in this bundle.
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Set the Annotations for this bundle. Should correspond to the Entity in
	 * the bundle.
	 */
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	/**
	 * Get the UserEntityPermissions in this bundle.
	 */
	public UserEntityPermissions getPermissions() {
		return permissions;
	}

	/**
	 * Set the UserEntityPermissions for this bundle. Should be the requesting
	 * user's permissions on the Entity in the bundle.
	 */
	public void setPermissions(UserEntityPermissions permissions) {
		this.permissions = permissions;
	}

	/**
	 * Get the hierarchical path to the Entity in this bundle.
	 */
	public EntityPath getPath() {
		return path;
	}

	/**
	 * Set the Path for this bundle. Should point to the Entity in the bundle.
	 */
	public void setPath(EntityPath path) {
		this.path = path;
	}

	/**
	 * Get the collection of names of Entities which reference the Entity in 
	 * this bundle.
	 */
	public List<EntityHeader> getReferencedBy() {
		return referencedBy;
	}

	/**
	 * Set the collection of names of referencing Entities in this bundle. 
	 * Should contain all Entities which reference the Entity in this bundle.
	 */
	public void setReferencedBy(List<EntityHeader> referencedBy) {
		this.referencedBy = referencedBy;
	}

	/**
	 * Does this entity have children?
	 */
	public Boolean getHasChildren() {
		return hasChildren;
	}

	/**
	 * Does this entity have children?
	 */
	public void setHasChildren(Boolean hasChildren) {
		this.hasChildren = hasChildren;
	}
	
	/**
	 * 
	 * Get the doi associated with this entity.
	 */
	@Deprecated
	public Doi getDoi() {
		return doi;
	}


	/**
	 *
	 * Get the DOI Association associated with this entity.
	 */
	public org.sagebionetworks.repo.model.doi.v2.DoiAssociation getDoiAssociation() {
		return doiAssociation;
	}

	/**
	 *
	 * Set the doi associated with this entity.
	 */
	@Deprecated
	public void setDoi(Doi doi) {
		this.doi = doi;
	}

	/**
	 *
	 * Set the DOI association associated with this entity.
	 */
	public void setDoiAssociation(org.sagebionetworks.repo.model.doi.v2.DoiAssociation doiAssociation) {
		this.doiAssociation = doiAssociation;
	}

	/**
	 * Get the AccessControlList for the Entity in this bundle.
	 */
	public AccessControlList getAccessControlList() {
		return acl;
	}

	/**
	 * Set the AccessControlList for this bundle. Should correspond to the
	 * Entity in this bundle.
	 */
	public void setAccessControlList(AccessControlList acl) {
		this.acl = acl;
	}
	
	public List<AccessRequirement> getAccessRequirements() {
		return accessRequirements;
	}

	public void setAccessRequirements(
			List<AccessRequirement> accessRequirements) {
		this.accessRequirements = accessRequirements;
	}

	public List<AccessRequirement> getUnmetAccessRequirements() {
		return unmetAccessRequirements;
	}

	public void setUnmetAccessRequirements(
			List<AccessRequirement> unmetAccessRequirements) {
		this.unmetAccessRequirements = unmetAccessRequirements;
	}

	public List<FileHandle> getFileHandles() {
		return fileHandles;
	}

	public void setFileHandles(List<FileHandle> fileHandles) {
		this.fileHandles = fileHandles;
	}

	public TableBundle getTableBundle() {
		return tableBundle;
	}

	public void setTableBundle(TableBundle tableBundle) {
		this.tableBundle = tableBundle;
	}

	public AccessControlList getAcl() {
		return acl;
	}

	public void setAcl(AccessControlList acl) {
		this.acl = acl;
	}

	public String getRootWikiId() {
		return rootWikiId;
	}

	public void setRootWikiId(String rootWikiId) {
		this.rootWikiId = rootWikiId;
	}

	public AccessControlList getBenefactorAcl() {
		return benefactorAcl;
	}

	public void setBenefactorAcl(AccessControlList benefactorAcl) {
		this.benefactorAcl = benefactorAcl;
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(Long threadCount) {
		this.threadCount = threadCount;
	}

	public RestrictionInformationResponse getRestrictionInformation() {
		return restrictionInformation;
	}

	public void setRestrictionInformation(RestrictionInformationResponse restrictionInformation) {
		this.restrictionInformation = restrictionInformation;
	}

	@Override
	public String toString() {
		return "EntityBundle [entity=" + entity + ", entityType=" + entityType + ", annotations=" + annotations
				+ ", permissions=" + permissions + ", path=" + path + ", referencedBy=" + referencedBy
				+ ", hasChildren=" + hasChildren + ", acl=" + acl + ", accessRequirements=" + accessRequirements
				+ ", unmetAccessRequirements=" + unmetAccessRequirements + ", fileHandles=" + fileHandles
				+ ", tableBundle=" + tableBundle + ", rootWikiId=" + rootWikiId + ", benefactorAcl=" + benefactorAcl
				+ ", doi=" + doi + ", doiAssociation=" + doiAssociation + ", fileName=" + fileName +
				", threadCount=" + threadCount + ", restrictionInformation=" + restrictionInformation + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirements == null) ? 0 : accessRequirements.hashCode());
		result = prime * result + ((acl == null) ? 0 : acl.hashCode());
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((benefactorAcl == null) ? 0 : benefactorAcl.hashCode());
		result = prime * result + ((doi == null) ? 0 : doi.hashCode());
		result = prime * result + ((doiAssociation == null) ? 0 : doiAssociation.hashCode());
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
		result = prime * result + ((fileHandles == null) ? 0 : fileHandles.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((hasChildren == null) ? 0 : hasChildren.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((permissions == null) ? 0 : permissions.hashCode());
		result = prime * result + ((referencedBy == null) ? 0 : referencedBy.hashCode());
		result = prime * result + ((restrictionInformation == null) ? 0 : restrictionInformation.hashCode());
		result = prime * result + ((rootWikiId == null) ? 0 : rootWikiId.hashCode());
		result = prime * result + ((tableBundle == null) ? 0 : tableBundle.hashCode());
		result = prime * result + ((threadCount == null) ? 0 : threadCount.hashCode());
		result = prime * result + ((unmetAccessRequirements == null) ? 0 : unmetAccessRequirements.hashCode());
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
		EntityBundle other = (EntityBundle) obj;
		if (accessRequirements == null) {
			if (other.accessRequirements != null)
				return false;
		} else if (!accessRequirements.equals(other.accessRequirements))
			return false;
		if (acl == null) {
			if (other.acl != null)
				return false;
		} else if (!acl.equals(other.acl))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (benefactorAcl == null) {
			if (other.benefactorAcl != null)
				return false;
		} else if (!benefactorAcl.equals(other.benefactorAcl))
			return false;
		if (doi == null) {
			if (other.doi != null)
				return false;
		} else if (!doi.equals(other.doi))
			return false;
		if (doiAssociation == null) {
			if (other.doiAssociation != null)
				return false;
		} else if (!doiAssociation.equals(other.doiAssociation))
			return false;
		if (entity == null) {
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		if (fileHandles == null) {
			if (other.fileHandles != null)
				return false;
		} else if (!fileHandles.equals(other.fileHandles))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (hasChildren == null) {
			if (other.hasChildren != null)
				return false;
		} else if (!hasChildren.equals(other.hasChildren))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		if (referencedBy == null) {
			if (other.referencedBy != null)
				return false;
		} else if (!referencedBy.equals(other.referencedBy))
			return false;
		if (restrictionInformation == null) {
			if (other.restrictionInformation != null)
				return false;
		} else if (!restrictionInformation.equals(other.restrictionInformation))
			return false;
		if (rootWikiId == null) {
			if (other.rootWikiId != null)
				return false;
		} else if (!rootWikiId.equals(other.rootWikiId))
			return false;
		if (tableBundle == null) {
			if (other.tableBundle != null)
				return false;
		} else if (!tableBundle.equals(other.tableBundle))
			return false;
		if (threadCount == null) {
			if (other.threadCount != null)
				return false;
		} else if (!threadCount.equals(other.threadCount))
			return false;
		if (unmetAccessRequirements == null) {
			if (other.unmetAccessRequirements != null)
				return false;
		} else if (!unmetAccessRequirements.equals(other.unmetAccessRequirements))
			return false;
		return true;
	}

}
