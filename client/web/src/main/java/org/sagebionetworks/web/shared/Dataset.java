package org.sagebionetworks.web.shared;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Dataset implements IsSerializable {
	private String id;
	private String uri;
	private String etag;
	private String name;
	private String description;
	private String creator;
	private Date creationDate;
	private String status;
	private Date releaseDate;
	private String version;
	private String annotations;
	private String layer;
	private Boolean hasExpressionData = false;
	private Boolean hasGeneticData = false;
	private Boolean hasClinicalData = false;

//	private List<LayerLink> layerPreviews;

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

	/**
	 * Default constructor is required
	 */
	public Dataset() {

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

//	public List<LayerLink> getLayerPreviews() {
//		return layerPreviews;
//	}
//
//	public void setLayerPreviews(List<LayerLink> layers) {
//		this.layerPreviews = layers;
//	}	
	
	public String getLayer() {
		return layer;
	}

	public void setLayer(String layer) {
		this.layer = layer;
	}
	
	public Boolean getHasExpressionData() {
		return hasExpressionData;
	}

	public void setHasExpressionData(Boolean hasExpressionData) {
		this.hasExpressionData = hasExpressionData;
	}

	public Boolean getHasGeneticData() {
		return hasGeneticData;
	}

	public void setHasGeneticData(Boolean hasGeneticData) {
		this.hasGeneticData = hasGeneticData;
	}

	public Boolean getHasClinicalData() {
		return hasClinicalData;
	}

	public void setHasClinicalData(Boolean hasClinicalData) {
		this.hasClinicalData = hasClinicalData;
	}
		

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		Dataset other = (Dataset) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", name=" + name + ", description="
				+ description + ", creator=" + creator + ", creationDate="
				+ creationDate + ", status=" + status + ", releaseDate="
				+ releaseDate + ", version=" + version + ", hasExpressionData=" + hasExpressionData 
				+ ", hasGeneticsData=" + hasGeneticData + ", hasClinicalData="
				+ hasClinicalData + "]";
	}

}
