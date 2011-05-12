package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.Date;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true")
public class JDONode {
		
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	@Column(name=SqlConstants.COL_NODE_PARENT_ID)
    @ForeignKey(name="NODE_PARENT_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode parent;

	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String name;
	
	@Persistent (mappedBy = "parent")
	@Element(dependent = "true")
	private Set<JDONode> children;
	
	@Persistent(dependent = "true")
	private JDOAnnotations annotations;
	
	@Persistent
	@Column(jdbcType="VARCHAR", length=3000)
	private String description;

	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long eTag = new Long(0);
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String createdBy;
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Date createdOn;
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String modifiedBy;
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Date modifiedOn;
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String nodeType;
	
	// Indicates the node that this node gets its permissions from.
	@Persistent 
	@Column(name=SqlConstants.COL_NODE_BENEFACTOR_ID)
    @ForeignKey(name="NODE_BENEFACTOR_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode permissionsBenefactor;
	// These are the nodes that that benefit from this nodes permissions
	@Persistent (mappedBy = "permissionsBenefactor")
	@Element(dependent = "true")
	private Set<JDONode> permissionsBeneficiaries;
	
	
	public Set<JDONode> getChildren() {
		return children;
	}

	public void setChildren(Set<JDONode> children) {
		this.children = children;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JDOAnnotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(JDOAnnotations annotations) {
		this.annotations = annotations;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public JDONode getParent() {
		return parent;
	}

	public void setParent(JDONode parentId) {
		this.parent = parentId;
	}

	public Long geteTag() {
		return eTag;
	}

	public void seteTag(Long eTag) {
		this.eTag = eTag;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public JDONode getPermissionsBenefactor() {
		return permissionsBenefactor;
	}

	public void setPermissionsBenefactor(JDONode permissionsBenefactor) {
		this.permissionsBenefactor = permissionsBenefactor;
	}

	public Set<JDONode> getPermissionsBeneficiaries() {
		return permissionsBeneficiaries;
	}

	public void setPermissionsBeneficiaries(Set<JDONode> permissionsBeneficiaries) {
		this.permissionsBeneficiaries = permissionsBeneficiaries;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		JDONode other = (JDONode) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
