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

//@PersistenceCapable(detachable = "false")
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
		setStringAnnotations(new HashSet<GAEJDOAnnotation<String>>());
		setNumberAnnotations(new HashSet<GAEJDOAnnotation<Number>>());
		setTextAnnotations(new HashSet<GAEJDOAnnotation<Text>>());
		setDateAnnotations(new HashSet<GAEJDOAnnotation<Date>>());
	}
	
	public static GAEJDOAnnotations clone(GAEJDOAnnotations a) {
		GAEJDOAnnotations ans = new GAEJDOAnnotations();
		Set<GAEJDOAnnotation<String>> sa = ans.getStringAnnotations();
		for (GAEJDOAnnotation<String> annot : a.getStringAnnotations()) {
			sa.add(new GAEJDOStringAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOAnnotation<Number>> ia = ans.getNumberAnnotations();
		for (GAEJDOAnnotation<Number> annot : a.getNumberAnnotations()) {
			ia.add(new GAEJDONumberAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOAnnotation<Text>> ta = ans.getTextAnnotations();
		for (GAEJDOAnnotation<Text> annot : a.getTextAnnotations()) {
			ta.add(new GAEJDOTextAnnotation(annot.getAttribute(), annot.getValue()));
		}
		Set<GAEJDOAnnotation<Date>> da = ans.getDateAnnotations();
		for (GAEJDOAnnotation<Date> annot : a.getDateAnnotations()) {
			da.add(new GAEJDODateAnnotation(annot.getAttribute(), annot.getValue()));
		}
		return ans;
	}
		
	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOAnnotation<String>> stringAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOAnnotation<Number>> numberAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOAnnotation<Text>> textAnnotations;

	@Persistent(mappedBy = "owner") 	
	@Element(dependent = "true")
	private Set<GAEJDOAnnotation<Date>> dateAnnotations;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public Set<GAEJDOAnnotation<String>> getStringAnnotations() {
		return stringAnnotations;
	}

	public void setStringAnnotations(
			Set<GAEJDOAnnotation<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}

	public Set<GAEJDOAnnotation<Number>> getNumberAnnotations() {
		return numberAnnotations;
	}

	public void setNumberAnnotations(Set<GAEJDOAnnotation<Number>> numberAnnotations) {
		this.numberAnnotations = numberAnnotations;
	}

	public Set<GAEJDOAnnotation<Text>> getTextAnnotations() {
		return textAnnotations;
	}

	public void setTextAnnotations(Set<GAEJDOAnnotation<Text>> textAnnotations) {
		this.textAnnotations = textAnnotations;
	}

	public Set<GAEJDOAnnotation<Date>> getDateAnnotations() {
		return dateAnnotations;
	}

	public void setDateAnnotations(Set<GAEJDOAnnotation<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}


}
