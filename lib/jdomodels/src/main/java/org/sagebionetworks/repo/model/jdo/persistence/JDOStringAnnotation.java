package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.jdo.JDOAnnotation;



/**
 * This is the persistable class for a Annotations whose values are Strings
 * 
 * Note: equals and hashcode are based on the attribute and value, allowing
 * distinct annotations with the same attribute.
 * 
 * @author bhoff
 * 
 */
@PersistenceCapable(detachable = "false")
public class JDOStringAnnotation implements JDOAnnotation<String> {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	// this is the backwards pointer for the 1-1 owned relationship
	@Persistent
	private JDOAnnotations owner;

	@Persistent
	private String attribute;

	@Persistent
	@Column(jdbcType="VARCHAR", length=3000)
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

	public JDOAnnotations getOwner() {
		return owner;
	}

	public void setOwner(JDOAnnotations owner) {
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
