package org.sagebionetworks.web.client.widget.footer;

import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class FooterViewImpl extends Composite implements FooterView {

	public interface Binder extends UiBinder<Widget, FooterViewImpl> {
	}

	@UiField
	ScriptElement searchScript;
	
	private Presenter presenter;
	
	@Inject
	public FooterViewImpl(Binder binder) {
		this.initWidget(binder.createAndBindUi(this));
		
		searchScript.setText("var TRANSMART_SEARCH = \"http://transmart.sagebase.org/transmart/search/search?sourcepage=search&id=\";		$(function() {			$( \"#query\" ).autocomplete({				source: function( request, response ) {					$.ajax({						url: \"http://transmart.sagebase.org/transmart/search/loadSearch\",						dataType: \"jsonp\",						data: {							query: \"all:\" + request.term												},						success: function( data ) {							response( $.map( data.rows, function( item ) {								return {									label: item.display + \": \" + item.keyword,									value: item.name,									id: item.id								}							}));						}					});				},				minLength: 1,				select: function( event, ui ) {					if(ui.item.id) {											document.location =  TRANSMART_SEARCH + ui.item.id;					}				}			});		});");
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

}
