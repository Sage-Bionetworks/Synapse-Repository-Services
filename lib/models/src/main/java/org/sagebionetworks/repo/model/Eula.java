
package org.sagebionetworks.repo.model;

/**
 * This class has several changes from the copy auto-generated using com.googlecode.jsonschema2pojo
 * http://code.google.com/p/jsonschema2pojo/
 * <ol>
 * <li> it also implements BaseChild
 * <li> I had to comment out the "additional properties" for it to play nice with the nodeable stuff
 * <li> transient fields
 * <li> blob primary field
 * </ol>
 * 
 * See src/main/resources/schema/eula.json for the corresponding JSON Schema
 * 
 * See target/java-gen/org/sagebionetworks/repo/model/notused/Eula.java for the auto-generated POJO
 */

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
public class Eula implements BaseChild, Serializable
{

    private byte[] agreementBlob;
    @JsonProperty("creationDate")
    private Date creationDate;
    @JsonProperty("etag")
    private String etag;
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("parentId")
    private String parentId;
    @JsonProperty("uri")
    private String uri;
//    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("agreement")
	@TransientField
    private String agreement;
    @JsonProperty("accessControlList")
	@TransientField
    private String accessControlList;
    @JsonProperty("annotations")
	@TransientField
    private String annotations;

    @JsonProperty("accessControlList")
    public String getAccessControlList() {
        return accessControlList;
    }

    @JsonProperty("accessControlList")
    public void setAccessControlList(String accessControlList) {
        this.accessControlList = accessControlList;
    }

    @JsonProperty("agreement")
    public String getAgreement() {
        return agreement;
    }

    @JsonProperty("agreement")
    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public byte[] getAgreementBlob() {
		return agreementBlob;
	}

    public void setAgreementBlob(byte[] agreementBlob) {
		this.agreementBlob = agreementBlob;
	}

    @JsonProperty("annotations")
    public String getAnnotations() {
        return annotations;
    }

    @JsonProperty("annotations")
    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    @JsonProperty("creationDate")
    public Date getCreationDate() {
        return creationDate;
    }

    @JsonProperty("creationDate")
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("etag")
    public String getEtag() {
        return etag;
    }

    @JsonProperty("etag")
    public void setEtag(String etag) {
        this.etag = etag;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("parentId")
    public String getParentId() {
        return parentId;
    }

    @JsonProperty("parentId")
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

//    @JsonAnyGetter
//    public Map<String, Object> getAdditionalProperties() {
//        return this.additionalProperties;
//    }
//
//    @JsonAnySetter
//    public void setAdditionalProperties(String name, Object value) {
//        this.additionalProperties.put(name, value);
//    }

}
