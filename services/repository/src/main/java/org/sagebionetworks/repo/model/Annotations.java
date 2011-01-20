package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class Annotations {
	private Map<String,Collection<String>> stringAnnotations;
	private Map<String,Collection<Number>> numberAnnotations;
	private Map<String,Collection<Date>> dateAnnotations;
	public Map<String, Collection<String>> getStringAnnotations() {
		return stringAnnotations;
	}
	public void setStringAnnotations(
			Map<String, Collection<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}
	public Map<String, Collection<Number>> getIntegerAnnotations() {
		return numberAnnotations;
	}
	public void setNumberAnnotations(
			Map<String, Collection<Number>> numberAnnotations) {
		this.numberAnnotations = numberAnnotations;
	}
	public Map<String, Collection<Date>> getDateAnnotations() {
		return dateAnnotations;
	}
	public void setDateAnnotations(Map<String, Collection<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}
	
	
}
