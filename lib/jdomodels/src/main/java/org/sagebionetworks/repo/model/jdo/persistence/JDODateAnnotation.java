package org.sagebionetworks.repo.model.jdo.persistence;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.jdo.JDOAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;



/**
 * This is the persistable class for a Annotations whose values are Dates
 * 
 * Note: equals and hashcode are based on the attribute and value, allowing
 * distinct annotations with the same attribute.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_DATE_ANNOTATIONS)
public class JDODateAnnotation implements JDOAnnotation<Date> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	@Column(name=SqlConstants.ANNOTATION_OWNER_ID_COLUMN)
    @ForeignKey(name="DATE_ANNON_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDONode owner; // this is the backwards pointer for the
										// 1-1 owned relationship

	@Persistent
	@Column(name=SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN)
	private String attribute;

	@Persistent
	@Column(name=SqlConstants.ANNOTATION_VALUE_COLUMN, jdbcType="TIMESTAMP")
	private Date value;

	public JDODateAnnotation() {
	}

	public JDODateAnnotation(String attr, Date value) {
		setAttribute(attr);
		setValue(value);
	}

	public String toString() {
		return getAttribute() + ": " + new SimpleDateFormat("ddMMMyyyy HH:mm:ss.").format(getValue());
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

	public Date getValue() {
		return value;
	}

	public void setValue(Date value) {
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
		JDODateAnnotation other = (JDODateAnnotation) obj;
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
