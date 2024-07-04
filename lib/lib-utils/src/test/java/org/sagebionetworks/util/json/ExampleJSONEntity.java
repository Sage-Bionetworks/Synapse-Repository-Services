package org.sagebionetworks.util.json;

import java.util.Arrays;
import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ExampleJSONEntity implements JSONEntity {

	private String name;
	private Long age;
	private Integer[] integerArray;

	public String getName() {
		return name;
	}

	public ExampleJSONEntity setName(String name) {
		this.name = name;
		return this;
	}

	public Long getAge() {
		return age;
	}

	public ExampleJSONEntity setAge(Long age) {
		this.age = age;
		return this;
	}

	public Integer[] getIntegerArray() {
		return integerArray;
	}

	public ExampleJSONEntity setIntegerArray(Integer[] integerArray) {
		this.integerArray = integerArray;
		return this;
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom.has("name")) {
			this.name = toInitFrom.getString("name");
		}
		if (toInitFrom.has("age")) {
			this.age = toInitFrom.getLong("age");
		}
		if (toInitFrom.has("integerArray")) {
			JSONArrayAdapter array = toInitFrom.getJSONArray("integerArray");
			integerArray = new Integer[array.length()];
			for (int i = 0; i < array.length(); i++) {
				integerArray[i] = array.getInt(i);
			}
		}
		return toInitFrom;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (name != null) {
			writeTo.put("name", name);
		}
		if (age != null) {
			writeTo.put("age", age);
		}
		if (integerArray != null) {
			JSONArrayAdapter array = writeTo.createNewArray();
			for (int i = 0; i < integerArray.length; i++) {
				array.put(i, integerArray[i]);
			}
			writeTo.put("integerArray", array);
		}
		return writeTo;
	}

	@Override
	public String toString() {
		return "ExampleJSONEntity [name=" + name + ", age=" + age + ", integerArray=" + Arrays.toString(integerArray)
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(integerArray);
		result = prime * result + Objects.hash(age, name);
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
		ExampleJSONEntity other = (ExampleJSONEntity) obj;
		return Objects.equals(age, other.age) && Arrays.equals(integerArray, other.integerArray)
				&& Objects.equals(name, other.name);
	}
}
