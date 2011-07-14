package org.sagebionetworks.repo.model;

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

/**
 * This class has three changes from the copy auto-generated using com.googlecode.jsonschema2pojo
 * http://code.google.com/p/jsonschema2pojo/
 * <ol>
 * <li> it also implements BaseChild
 * <li> I had to comment out the "additional properties" for it to play nice with the nodeable stuff
 * <li> I had to change the type from Object to Long for the versionNumber properties
 * </ol>
 * 
 * See src/main/resources/schema/agreement.json for the corresponding JSON Schema
 * 
 * See target/java-gen/org/sagebionetworks/repo/model/notused/Agreement.java for the auto-generated POJO
 */

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
public class Agreement implements BaseChild, Serializable
{

    @JsonProperty("accessControlList")
    private String accessControlList;
    @JsonProperty("annotations")
    private String annotations;
    @JsonProperty("createdBy")
    private String createdBy;
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
    @JsonProperty("datasetId")
    private String datasetId;
    @JsonProperty("datasetVersionNumber")
    private Long datasetVersionNumber;
    @JsonProperty("eulaId")
    private String eulaId;
    @JsonProperty("eulaVersionNumber")
    private Long eulaVersionNumber;
//    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("accessControlList")
    public String getAccessControlList() {
        return accessControlList;
    }

    @JsonProperty("accessControlList")
    public void setAccessControlList(String accessControlList) {
        this.accessControlList = accessControlList;
    }

    @JsonProperty("annotations")
    public String getAnnotations() {
        return annotations;
    }

    @JsonProperty("annotations")
    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }
    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    @JsonProperty("datasetId")
    public String getDatasetId() {
        return datasetId;
    }

    @JsonProperty("datasetId")
    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    @JsonProperty("datasetVersionNumber")
    public Long getDatasetVersionNumber() {
        return datasetVersionNumber;
    }

    @JsonProperty("datasetVersionNumber")
    public void setDatasetVersionNumber(Long datasetVersionNumber) {
        this.datasetVersionNumber = datasetVersionNumber;
    }

    @JsonProperty("eulaId")
    public String getEulaId() {
        return eulaId;
    }

    @JsonProperty("eulaId")
    public void setEulaId(String eulaId) {
        this.eulaId = eulaId;
    }

    @JsonProperty("eulaVersionNumber")
    public Long getEulaVersionNumber() {
        return eulaVersionNumber;
    }

    @JsonProperty("eulaVersionNumber")
    public void setEulaVersionNumber(Long eulaVersionNumber) {
        this.eulaVersionNumber = eulaVersionNumber;
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
