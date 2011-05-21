package org.sagebionetworks.repo.model.jdo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import org.sagebionetworks.repo.model.jdo.JDOBase;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "false", table=SqlConstants.TABLE_USER_GROUP)
public class JDOUserGroup implements JDOBase {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent (column=SqlConstants.COL_USER_GROUP_ID)
	@Unique
	private String name;
	
	@Column(name=SqlConstants.COL_NODE_ETAG)
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private Long eTag = new Long(0);
	
	@Persistent
	private Date creationDate;

	// true for groups established for individuals (in which case group 'name'==userId)
	@Persistent (column=SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL)
	private Boolean isIndividual = false;

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
	
	public String toString() {return getName();}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Boolean getIsIndividual() {
		return isIndividual;
	}

	public void setIsIndividual(Boolean isIndividual) {
		this.isIndividual = isIndividual;
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
		if (!(obj instanceof JDOUserGroup))
			return false;
		JDOUserGroup other = (JDOUserGroup) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	/**
	 * @return the eTag
	 */
	public Long getEtag() {
		return eTag;
	}

	/**
	 * @param eTag the eTag to set
	 */
	public void setEtag(Long eTag) {
		this.eTag = eTag;
	}


}
