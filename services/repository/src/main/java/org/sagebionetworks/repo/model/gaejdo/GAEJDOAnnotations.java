package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable(detachable = "false")
public class GAEJDOAnnotations {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id;
	
//	@Extension(vendorName = "datanucleus", key = "implementation-classes", 
//			value = "org.sagebionetworks.repo.model.GAEJDODataset")
//	@Persistent(mappedBy="annotations")
//	private GAEJDOAnnotatable owner; // this is the backwards pointer for the 1-1 owned relationship
	

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
	
	public static GAEJDOAnnotations clone(GAEJDOAnnotations a) {
		GAEJDOAnnotations ans = new GAEJDOAnnotations();
		Set<GAEJDOStringAnnotation> sa = ans.getStringAnnotations();
		for (GAEJDOStringAnnotation annot : a.getStringAnnotations()) {
			sa.add(new GAEJDOStringAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOIntegerAnnotation> ia = ans.getIntegerAnnotations();
		for (GAEJDOAnnotation<Integer> annot : a.getIntegerAnnotations()) {
			ia.add(new GAEJDOIntegerAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOBooleanAnnotation> ba = ans.getBooleanAnnotations();
		for (GAEJDOAnnotation<Boolean> annot : a.getBooleanAnnotations()) {
			ba.add(new GAEJDOBooleanAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOTextAnnotation> ta = ans.getTextAnnotations();
		for (GAEJDOAnnotation<Text> annot : a.getTextAnnotations()) {
			ta.add(new GAEJDOTextAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOFloatAnnotation> fa = ans.getFloatAnnotations();
		for (GAEJDOAnnotation<Float> annot : a.getFloatAnnotations()) {
			fa.add(new GAEJDOFloatAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDODateAnnotation> da = ans.getDateAnnotations();
		for (GAEJDOAnnotation<Date> annot : a.getDateAnnotations()) {
			da.add(new GAEJDODateAnnotation(annot.getAttribute(), annot.getValue()));
		}
		return ans;
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

//	public GAEJDOAnnotatable getOwner() {
//		return owner;
//	}
//
//	public void setOwner(GAEJDOAnnotatable owner) {
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
