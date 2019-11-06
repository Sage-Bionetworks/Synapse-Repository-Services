package org.sagebionetworks.util;


import org.sagebionetworks.schema.HasEffectiveSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class JSONEntitySample implements JSONEntity, HasEffectiveSchema {
	
	 public final static String EFFECTIVE_SCHEMA = "{\"id\":\"org.sagebionetworks.repo.util.JSONEntitySample\",\"title\":\"JSONEntitySample\",\"properties\":{\"stringField\":{\"description\":\"User's last name\",\"type\":\"string\"},\"hmac\":{\"description\":\"The hash message authentication code for the message.\",\"type\":\"string\"},\"firstName\":{\"description\":\"User's first name\",\"type\":\"string\"}},\"type\":\"object\"}";

	public JSONEntitySample() {concreteType=JSONEntitySample.class.getName();}

	public String getHmac() {
		return hmac;
	}
	public void setHmac(String hmac) {
		this.hmac = hmac;
	}
	public String getStringField() {
		return stringField;
	}
	public void setStringField(String stringField) {
		this.stringField = stringField;
	}
	private String hmac;
	private String stringField;
	private String concreteType;


	public String getConcreteType() {
		return concreteType;
	}

	public void setConcreteType(String concreteType) {
		this.concreteType = concreteType;
	}

	/**
	 * @see JSONEntity#initializeFromJSONObject(JSONObjectAdapter)
	 * @see JSONEntity#writeToJSONObject(JSONObjectAdapter)
	 * 
	 * @param adapter
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException
	{
		if (adapter == null) {
			throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
		}
		if (!adapter.isNull("concreteType")) {
			concreteType = adapter.getString("concreteType");
		} else {
			concreteType = null;
		}
		if (!adapter.isNull("stringField")) {
			stringField = adapter.getString("stringField");
		} else {
			stringField = null;
		}
		return adapter;
	}

	/**
	 * @see JSONEntity#initializeFromJSONObject(JSONObjectAdapter)
	 * @see JSONEntity#writeToJSONObject(JSONObjectAdapter)
	 * 
	 * @param adapter
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter)
			throws JSONObjectAdapterException
	{
		if (adapter == null) {
			throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
		}
		if (concreteType!= null) {
			adapter.put("concreteType", concreteType);
		}
		if (stringField!= null) {
			adapter.put("stringField", stringField);
		}
		return adapter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((concreteType == null) ? 0 : concreteType.hashCode());
		result = prime * result + ((hmac == null) ? 0 : hmac.hashCode());
		result = prime * result + ((stringField == null) ? 0 : stringField.hashCode());
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
		JSONEntitySample other = (JSONEntitySample) obj;
		if (concreteType == null) {
			if (other.concreteType != null)
				return false;
		} else if (!concreteType.equals(other.concreteType))
			return false;
		if (hmac == null) {
			if (other.hmac != null)
				return false;
		} else if (!hmac.equals(other.hmac))
			return false;
		if (stringField == null) {
			if (other.stringField != null)
				return false;
		} else if (!stringField.equals(other.stringField))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JSONEntitySample [hmac=" + hmac + ", stringField="
				+ stringField + "]";
	}

	@Override
	public String getEffectiveSchema() {
		return EFFECTIVE_SCHEMA;
	}
	
	
}

