package org.sagebionetworks.web.shared;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.web.shared.EnvironmentDescriptor;
import org.sagebionetworks.web.shared.Reference;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Step implements IsSerializable {
	private String etag;
	private String id;
	private String name;
	private String parentId;
	private String uri;
	private Date creationDate;
	private String createdBy;
	private Date startDate;
	private Date endDate;
	private String description;
	private String commandLine;
	private Set<Reference> code;
	private Set<Reference> input;
	private Set<Reference> output;
	private Set<EnvironmentDescriptor> environmentDescriptors;
	private String annotations; // URI for annotations
	private String accessControlList; // URI for acl

	public Step() {
	}

	/**
	 * Default constructor is required
	 * 
	 * @param obj
	 *            JSONObject of the Project object
	 */
	public Step(JSONObject object) {
		String key = null;

		key = "name";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setName(object.get(key).isString().stringValue());

		key = "annotations";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setAnnotations(object.get(key).isString().stringValue());

		key = "id";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setId(object.get(key).isString().stringValue());

		key = "commandLine";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setCommandLine(object.get(key).isString().stringValue());

		key = "description";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setDescription(object.get(key).isString().stringValue());

		key = "creationDate";
		if (object.containsKey(key))
			if (object.get(key).isNumber() != null)
				setCreationDate(new Date(new Double(object.get(key).isNumber()
						.doubleValue()).longValue()));

		key = "parentId";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setParentId(object.get(key).isString().stringValue());

		key = "etag";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setEtag(object.get(key).isString().stringValue());

		key = "uri";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setUri(object.get(key).isString().stringValue());

		key = "accessControlList";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setAccessControlList(object.get(key).isString().stringValue());

		key = "createdBy";
		if (object.containsKey(key))
			if (object.get(key).isString() != null)
				setCreatedBy(object.get(key).isString().stringValue());

		key = "startDate";
		if (object.containsKey(key))
			if (object.get(key).isNumber() != null)
				setStartDate(new Date(new Double(object.get(key).isNumber()
						.doubleValue()).longValue()));

		key = "endDate";
		if (object.containsKey(key))
			if (object.get(key).isNumber() != null)
				setEndDate(new Date(new Double(object.get(key).isNumber()
						.doubleValue()).longValue()));

		key = "code";
		if (object.containsKey(key))
			if (object.get(key).isArray() != null)
				setCode(deserializeReferences(object.get(key).isArray()));

		key = "input";
		if (object.containsKey(key))
			if (object.get(key).isArray() != null)
				setInput(deserializeReferences(object.get(key).isArray()));

		key = "output";
		if (object.containsKey(key))
			if (object.get(key).isArray() != null)
				setOutput(deserializeReferences(object.get(key).isArray()));

		key = "environmentDescriptors";
		if (object.containsKey(key))
			if (object.get(key).isArray() != null)
				setEnvironmentDescriptors(deserializeEnvironmentDescriptors(object
						.get(key).isArray()));
	}

	private Set<Reference> deserializeReferences(JSONArray array) {
		Set<Reference> refs = new HashSet<Reference>();
		for (int i = 0; i < array.size(); i++) {
			JSONObject refJson = array.get(i).isObject();
			String targetId = (refJson.containsKey("targetId") && (null != refJson.get(
					"targetId").isString())) ? refJson.get(
					"targetId").isString().stringValue() : "";
			long targetVersionNumber = (refJson
					.containsKey("targetVersionNumber") && (null != refJson
							.get("targetVersionNumber").isNumber())) ? new Double(refJson
					.get("targetVersionNumber").isNumber().doubleValue())
					.longValue() : 0L;

			Reference ref = new Reference();
			ref.setTargetId(targetId);
			ref.setTargetVersionNumber(targetVersionNumber);
		}
		return refs;
	}

	private Set<EnvironmentDescriptor> deserializeEnvironmentDescriptors(
			JSONArray array) {
		Set<EnvironmentDescriptor> descriptors = new HashSet<EnvironmentDescriptor>();
		for (int i = 0; i < array.size(); i++) {
			JSONObject descriptorJson = array.get(i).isObject();
			String name = (descriptorJson.containsKey("name") && (null != descriptorJson
					.get("name").isString())) ? descriptorJson
					.get("name").isString().stringValue() : "";
			String type = (descriptorJson.containsKey("type") && (null != descriptorJson
					.get("type").isString())) ? descriptorJson
					.get("type").isString().stringValue() : "";
			String quantifier = (descriptorJson.containsKey("quantifier") && (null != descriptorJson
					.get("quantifier").isString())) ? descriptorJson
					.get("quantifier").isString().stringValue()
					: "";

			EnvironmentDescriptor descriptor = new EnvironmentDescriptor();
			descriptor.setName(name);
			descriptor.setType(type);
			descriptor.setQuantifier(quantifier);
		}
		return descriptors;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCommandLine() {
		return commandLine;
	}

	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUri() {
		return uri;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate
	 *            the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate
	 *            the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the code
	 */
	public Set<Reference> getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(Set<Reference> code) {
		this.code = code;
	}

	/**
	 * @return the input
	 */
	public Set<Reference> getInput() {
		return input;
	}

	/**
	 * @param input
	 *            the input to set
	 */
	public void setInput(Set<Reference> input) {
		this.input = input;
	}

	/**
	 * @return the output
	 */
	public Set<Reference> getOutput() {
		return output;
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(Set<Reference> output) {
		this.output = output;
	}

	/**
	 * @return the environmentDescriptors
	 */
	public Set<EnvironmentDescriptor> getEnvironmentDescriptors() {
		return environmentDescriptors;
	}

	/**
	 * @param environmentDescriptors
	 *            the environmentDescriptors to set
	 */
	public void setEnvironmentDescriptors(
			Set<EnvironmentDescriptor> environmentDescriptors) {
		this.environmentDescriptors = environmentDescriptors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessControlList == null) ? 0 : accessControlList
						.hashCode());
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result
				+ ((commandLine == null) ? 0 : commandLine.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime
				* result
				+ ((environmentDescriptors == null) ? 0
						: environmentDescriptors.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((output == null) ? 0 : output.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result
				+ ((startDate == null) ? 0 : startDate.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Step other = (Step) obj;
		if (accessControlList == null) {
			if (other.accessControlList != null)
				return false;
		} else if (!accessControlList.equals(other.accessControlList))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (commandLine == null) {
			if (other.commandLine != null)
				return false;
		} else if (!commandLine.equals(other.commandLine))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (endDate == null) {
			if (other.endDate != null)
				return false;
		} else if (!endDate.equals(other.endDate))
			return false;
		if (environmentDescriptors == null) {
			if (other.environmentDescriptors != null)
				return false;
		} else if (!environmentDescriptors.equals(other.environmentDescriptors))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (output == null) {
			if (other.output != null)
				return false;
		} else if (!output.equals(other.output))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (startDate == null) {
			if (other.startDate != null)
				return false;
		} else if (!startDate.equals(other.startDate))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Step [accessControlList=" + accessControlList
				+ ", annotations=" + annotations + ", code=" + code
				+ ", commandLine=" + commandLine + ", createdBy=" + createdBy
				+ ", creationDate=" + creationDate + ", description="
				+ description + ", endDate=" + endDate
				+ ", environmentDescriptors=" + environmentDescriptors
				+ ", etag=" + etag + ", id=" + id + ", input=" + input
				+ ", name=" + name + ", output=" + output + ", parentId="
				+ parentId + ", startDate=" + startDate + ", uri=" + uri + "]";
	}

}
