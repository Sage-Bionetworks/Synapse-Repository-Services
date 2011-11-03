package org.sagebionetworks.web.client.widget.editpanels;

import org.sagebionetworks.web.client.ontology.NcboOntologyTerm;

import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.JsonPagingLoadResultReader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.Loader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelType;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoader;
import com.extjs.gxt.ui.client.data.ScriptTagProxy;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.form.ComboBox;

public class NcboSearchSuggestBox {

	public static ComboBox<NcboOntologyTerm> createNcboSuggestField() {
		String url = "/Portal/ncbo/search";
		ScriptTagProxy<PagingLoadResult<NcboOntologyTerm>> proxy = new ScriptTagProxy<PagingLoadResult<NcboOntologyTerm>>(
				url);

		ModelType type = new ModelType();
		type.setRoot("searchBean");
		type.setTotalName("numResultsTotal");
		type.addField(NcboOntologyTerm.PREFERRED_NAME, "preferredName");
		type.addField(NcboOntologyTerm.CONCENPT_ID_SHORT, "conceptIdShort");
		type.addField(NcboOntologyTerm.ONTOLOGY_DISPLAY_LABEL, "ontologyDisplayLabel");
		type.addField(NcboOntologyTerm.ONTOLOGY_VERSION_ID, "ontologyVersionId");

		JsonPagingLoadResultReader<PagingLoadResult<NcboOntologyTerm>> reader = new JsonPagingLoadResultReader<PagingLoadResult<NcboOntologyTerm>>(
				type);
		
		PagingLoader<PagingLoadResult<NcboOntologyTerm>> loader = new BasePagingLoader<PagingLoadResult<NcboOntologyTerm>>(
				proxy, reader);

		loader.addListener(Loader.BeforeLoad, new Listener<LoadEvent>() {
			public void handleEvent(LoadEvent be) {
				be.<ModelData> getConfig().set("start",
						be.<ModelData> getConfig().get("offset"));
			}
		});

		ListStore<NcboOntologyTerm> store = new ListStore<NcboOntologyTerm>(loader);

		ComboBox<NcboOntologyTerm> combo = new ComboBox<NcboOntologyTerm>();
		combo.setWidth(580);
		combo.setDisplayField(NcboOntologyTerm.PREFERRED_NAME);
		combo.setItemSelector("div.search-item");
		combo.setTemplate(getTemplate());
		combo.setStore(store);
		combo.setHideTrigger(true);
		combo.setPageSize(10);

		return combo;
	}

	private static native String getTemplate() /*-{
		return [ '<tpl for="."><div class="search-item">',
				'<table cellspacing="0" cellpadding="0" width="100%"><tr valign="top">',
				'<td><span class="suggestSearchTerm">{preferredName}</span></td>',
				'<td align="right"><span class="suggestSearchOntology">{ontologyDisplayLabel}</span></td>',
				'</tr></table>',
				'</div></tpl>' ].join("");
	}-*/;

}
