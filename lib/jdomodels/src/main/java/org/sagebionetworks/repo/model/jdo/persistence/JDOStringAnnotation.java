package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.jdo.JDOAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;



/**
 * This is the persistable class for a Annotations whose values are Strings
 * 
 * Note: equals and hashcode are based on the attribute and value, allowing
 * distinct annotations with the same attribute.
 * 
 * @author bhoff
 * 
 * NOTE: The Indices added are part of the fix for PLFM-770.
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_STRING_ANNOTATIONS)
@Indices({
	@Index(name="VAL1_ATT2_INDEX", members={"value","attribute"}),
	@Index(name="VAL1_OWN2_INDEX", members={"value","owner"}),
	@Index(name="ATT1_VAL1_INDEX", members={"attribute","value"}),
	@Index(name="ATT1_OWN2_INDEX", members={"attribute","owner"}),
	@Index(name="OWN1_VAL2_INDEX", members={"owner","value"}),
	@Index(name="OWN1_ATT2_INDEX", members={"owner","attribute"})
	})
public class JDOStringAnnotation implements JDOAnnotation<String> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	// this is the backwards pointer for the 1-1 owned relationship
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	@Column(name=SqlConstants.ANNOTATION_OWNER_ID_COLUMN)
    @ForeignKey(name="STRING_ANNON_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode owner;

	@Persistent
	@Column(name=SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN)
	private String attribute;

	@Persistent
	@Column(name=SqlConstants.ANNOTATION_VALUE_COLUMN, jdbcType="VARCHAR", length=SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH)
	private String value;

	public JDOStringAnnotation() {
	}

	public JDOStringAnnotation(String attr, String value) {
		setAttribute(attr);
		setValue(value);
	}

	public String toString() {
		return getAttribute() + ": " + getValue();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JDONode getOwner() {
		return owner;
	}

	public void setOwner(JDONode owner) {
		this.owner = owner;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		JDOStringAnnotation other = (JDOStringAnnotation) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
