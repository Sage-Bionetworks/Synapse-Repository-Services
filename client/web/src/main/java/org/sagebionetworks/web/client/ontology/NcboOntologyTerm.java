package org.sagebionetworks.web.client.ontology;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

public class NcboOntologyTerm extends BaseModelData {
		
	public static final String CONCENPT_ID_SHORT = "conceptIdShort";
	public static final String ONTOLOGY_VERSION_ID = "ontologyVersionId";
	public static final String PREFERRED_NAME = "preferredName";
	public static final String ONTOLOGY_DISPLAY_LABEL = "ontologyDisplayLabel";
	private static final long serialVersionUID = 1L;
	
	// TODO : temporary
	public static final String NCBO_VALUE_PREFIX = "NCBO-ENTRY:";	
	
	public NcboOntologyTerm() {
		super();
	}
	
	public NcboOntologyTerm(BaseModelData copy) {
		this(copy.get(CONCENPT_ID_SHORT).toString(), 
				copy.get(ONTOLOGY_VERSION_ID).toString(), 
				copy.get(PREFERRED_NAME).toString(), 
				copy.get(ONTOLOGY_DISPLAY_LABEL).toString());
	}
	
	public NcboOntologyTerm(String concenptIdShort,
			String ontologyVersionId, String preferredName, String ontologyDisplayLabel) {
		super();
		setConcenptIdShort(concenptIdShort);
		setOntologyVersionId(ontologyVersionId);
		setPreferredName(preferredName);
		setOntologyDisplayLabel(ontologyDisplayLabel);
	} 
	
	public NcboOntologyTerm(String serialized) {
		String objJson = serialized.replaceFirst(NCBO_VALUE_PREFIX, "");
		JSONObject obj = JSONParser.parseStrict(objJson).isObject();
		if(!obj.containsKey(CONCENPT_ID_SHORT) 
				|| !obj.containsKey(ONTOLOGY_VERSION_ID)
				|| !obj.containsKey(PREFERRED_NAME)
				|| !obj.containsKey(ONTOLOGY_DISPLAY_LABEL)) {
			throw new JSONException("JSON is poorly formatted");
		} 
		setConcenptIdShort(obj.get(CONCENPT_ID_SHORT).isString().stringValue());
		setOntologyVersionId(obj.get(ONTOLOGY_VERSION_ID).isString().stringValue());
		setPreferredName(obj.get(PREFERRED_NAME).isString().stringValue());
		setOntologyDisplayLabel(obj.get(ONTOLOGY_DISPLAY_LABEL).isString().stringValue());
	}
	
	/**
	 * Convert this to a serialized form
	 */
	public String serialize() {
		JSONObject obj = new JSONObject();
		obj.put(CONCENPT_ID_SHORT, new JSONString(getConcenptIdShort()));
		obj.put(ONTOLOGY_VERSION_ID, new JSONString(getOntologyVersionId()));
		obj.put(PREFERRED_NAME, new JSONString(getPreferredName()));
		obj.put(ONTOLOGY_DISPLAY_LABEL, new JSONString(getOntologyDisplayLabel()));
		
		return NCBO_VALUE_PREFIX + obj.toString();
	}
	
		
	public String getPreferredName() {
		return get(PREFERRED_NAME);
	}

	public void setPreferredName(String preferredName) {
		set(PREFERRED_NAME, preferredName);
	}

	public String getConcenptIdShort() {
		return get(CONCENPT_ID_SHORT);
	}

	public void setConcenptIdShort(String concenptIdShort) {
		set(CONCENPT_ID_SHORT, concenptIdShort);
	}

	public String getOntologyVersionId() {
		return get(ONTOLOGY_VERSION_ID);
	}

	public void setOntologyVersionId(String ontologyId) {
		set(ONTOLOGY_VERSION_ID, ontologyId);
	}

	public String getOntologyDisplayLabel() {
		return get(ONTOLOGY_DISPLAY_LABEL);
	}

	public void setOntologyDisplayLabel(String ontologyDisplayLabel) {
		set(ONTOLOGY_DISPLAY_LABEL, ontologyDisplayLabel);
	}
	
}
