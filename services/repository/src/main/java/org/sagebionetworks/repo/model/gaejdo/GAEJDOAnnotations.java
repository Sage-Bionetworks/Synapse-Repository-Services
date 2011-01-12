package org.sagebionetworks.repo.model.gaejdo;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(detachable = "false")
public class GAEJDOAnnotations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
//	@Extension(vendorName = "datanucleus", key = "implementation-classes", 
//			value = "org.sagebionetworks.repo.model.GAEJDODataset")
//	@Persistent(mappedBy="annotations")
//	private Annotatable owner; // this is the backwards pointer for the 1-1 owned relationship
	

	// I'm not sure if this is quite right, as the data store may wish to
	// populate the fields itself.   However in the worst case the field 
	// are just overwritten and so a little time is wasted creating these empty maps.
	public GAEJDOAnnotations() {
		setStringAnnotations(new HashSet<GAEJDOStringAnnotation>());
		setIntegerAnnotations(new HashSet<GAEJDOIntegerAnnotation>());
		setTextAnnotations(new HashSet<GAEJDOTextAnnotation>());
		setBooleanAnnotations(new HashSet<GAEJDOBooleanAnnotation>());
		setFloatAnnotations(new HashSet<GAEJDOFloatAnnotation>());
		setDateAnnotations(new HashSet<GAEJDODateAnnotation>());
	}
		
	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOStringAnnotation> stringAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOIntegerAnnotation> integerAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOTextAnnotation> textAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOBooleanAnnotation> booleanAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOFloatAnnotation> floatAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDODateAnnotation> dateAnnotations;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

//	public Annotatable getOwner() {
//		return owner;
//	}
//
//	public void setOwner(Annotatable owner) {
//		this.owner = owner;
//	}

	public Set<GAEJDOStringAnnotation> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Set<GAEJDOStringAnnotation> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Set<GAEJDOIntegerAnnotation> getIntegerAnnotations() {
		return integerAnnotations;
	}

	public void setIntegerAnnotations(Set<GAEJDOIntegerAnnotation> integerAnnotations) {
		this.integerAnnotations = integerAnnotations;
	}

	public Set<GAEJDOTextAnnotation> getTextAnnotations() {
		return textAnnotations;
	}

	public void setTextAnnotations(Set<GAEJDOTextAnnotation> textAnnotations) {
		this.textAnnotations = textAnnotations;
	}

	public Set<GAEJDOBooleanAnnotation> getBooleanAnnotations() {
		return booleanAnnotations;
	}

	public void setBooleanAnnotations(Set<GAEJDOBooleanAnnotation> booleanAnnotations) {
		this.booleanAnnotations = booleanAnnotations;
	}

	public Set<GAEJDOFloatAnnotation> getFloatAnnotations() {
		return floatAnnotations;
	}

	public void setFloatAnnotations(Set<GAEJDOFloatAnnotation> floatAnnotations) {
		this.floatAnnotations = floatAnnotations;
	}

	public Set<GAEJDODateAnnotation> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Set<GAEJDODateAnnotation> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}


}
