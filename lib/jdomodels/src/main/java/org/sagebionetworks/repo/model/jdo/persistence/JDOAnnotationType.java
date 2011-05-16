package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Enforces annotation types.  Once an annotation name is used for a type it cannot be re-used for another.
 * @author jmhill
 *
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_ANNOTATION_TYPE)
public class JDOAnnotationType {
	
	
	@PrimaryKey
	private String attributeName;
	
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	private String typeClass;


	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getTypeClass() {
		return typeClass;
	}

	public void setTypeClass(String typeClass) {
		this.typeClass = typeClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributeName == null) ? 0 : attributeName.hashCode());
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
		JDOAnnotationType other = (JDOAnnotationType) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		return true;
	}

}
