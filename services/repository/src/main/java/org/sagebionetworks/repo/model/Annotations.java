package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class Annotations {
	private Map<String,Collection<String>> stringAnnotations;
	private Map<String,Collection<Integer>> integerAnnotations;
	private Map<String,Collection<Boolean>> booleanAnnotations;
	private Map<String,Collection<Float>> floatAnnotations;
	private Map<String,Collection<Date>> dateAnnotations;
	public Map<String, Collection<String>> getStringAnnotations() {
		return stringAnnotations;
	}
	public void setStringAnnotations(
			Map<String, Collection<String>> stringAnnotations) {
		this.stringAnnotations = stringAnnotations;
	}
	public Map<String, Collection<Integer>> getIntegerAnnotations() {
		return integerAnnotations;
	}
	public void setIntegerAnnotations(
			Map<String, Collection<Integer>> integerAnnotations) {
		this.integerAnnotations = integerAnnotations;
	}
	public Map<String, Collection<Boolean>> getBooleanAnnotations() {
		return booleanAnnotations;
	}
	public void setBooleanAnnotations(
			Map<String, Collection<Boolean>> booleanAnnotations) {
		this.booleanAnnotations = booleanAnnotations;
	}
	public Map<String, Collection<Float>> getFloatAnnotations() {
		return floatAnnotations;
	}
	public void setFloatAnnotations(Map<String, Collection<Float>> floatAnnotations) {
		this.floatAnnotations = floatAnnotations;
	}
	public Map<String, Collection<Date>> getDateAnnotations() {
		return dateAnnotations;
	}
	public void setDateAnnotations(Map<String, Collection<Date>> dateAnnotations) {
		this.dateAnnotations = dateAnnotations;
	}
	
}
