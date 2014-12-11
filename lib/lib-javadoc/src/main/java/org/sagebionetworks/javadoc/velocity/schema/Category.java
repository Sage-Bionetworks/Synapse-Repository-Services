package org.sagebionetworks.javadoc.velocity.schema;

import java.util.LinkedList;
import java.util.List;

public class Category {
	
	String name;
	List<Example> examples;
	
	public Category(String name) {
		this.name = name;
		this.examples = new LinkedList<Example>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Example> getExamples() {
		return examples;
	}

	public void setExamples(List<Example> examples) {
		this.examples = examples;
	}

	public void add(Example example) {
		this.examples.add(example);
	}
	
}
