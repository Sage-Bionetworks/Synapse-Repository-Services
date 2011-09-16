package org.sagebionetworks.web.client.widget.editpanels.phenotype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.ontology.StaticOntologies;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PhenotypeEditor implements PhenotypeEditorView.Presenter, SynapseWidgetPresenter {

	private PhenotypeEditorView view;
	private NodeServiceAsync nodeService;
	private NodeModelCreator nodeModelCreator;
    private PlaceChanger placeChanger;
    private StaticOntologies staticOntologies;
    private AuthenticationController authenticationController;
    private ColumnDefinitionEditor columnDefinitionEditor;
    private ColumnMappingEditor columnMappingEditor;
    private PhenotypeMatrix phenotypeMatrix;
    private String layerId;
	 
    private List<String> columns;
    private String currentIdentityColumn;
    private Map<String,String> columnToOntology;
    
	@Inject
	public PhenotypeEditor(PhenotypeEditorView view, NodeServiceAsync nodeService, NodeModelCreator nodeModelCreator, AuthenticationController authenticationController, StaticOntologies staticOntologies, ColumnDefinitionEditor columnDefinitionEditor, ColumnMappingEditor columnMappingEditor, PhenotypeMatrix phenotypeMatrix) {
        this.view = view;
		this.nodeService = nodeService;
		this.nodeModelCreator = nodeModelCreator;
		this.staticOntologies = staticOntologies;
		this.authenticationController = authenticationController;
		this.columnDefinitionEditor = columnDefinitionEditor;
		this.columnMappingEditor = columnMappingEditor;
		this.phenotypeMatrix = phenotypeMatrix;
        view.setPresenter(this);		
	}
	
    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
    }

    public void setLayerId(String layerId) {
   		view.showLoading();
    	this.layerId = layerId;
    	String phenotypeFileString = getPhenotypesFileString();
    	columns = parseColumnsPhenotypeFile(phenotypeFileString, ",", false);
    	List<Map<String, String>> phenoData = parsePhenotypeFile(phenotypeFileString, columns, ",", false);
    	columnDefinitionEditor.setResources(columns, columns.get(0), columnToOntology, staticOntologies.getAnnotationToOntology().values());
    	columnMappingEditor.setResources();    	
    	phenotypeMatrix.setResources();
    	
    	view.generatePhenotypeEditor(columns, columns.get(0), phenoData, staticOntologies.getAnnotationToOntology().values(), columnDefinitionEditor, columnMappingEditor, phenotypeMatrix);    	
    }
    
    @Override
	public Widget asWidget() {
   		view.setPresenter(this);
        return view.asWidget();
    }
	
	
	@Override
	public void removeColumn(String columnName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColumnOntology(String columnName, String value) {
		columnToOntology.put(columnName, value);
	}

	@Override
	public void setIdentityColumn(String value) {
		currentIdentityColumn = value;
	}

	
	/*
	 * Private Methods
	 */   	
   	private String getPhenotypesFileString() {
   		return "ID sample,aCGH,Expression array,Lesion (Adenoma or Carcinoma),Location,Type,Size (mm),Stage ,Grade,Age at diagnosis,Gender,lymphnode status,MSI status\nP1A,yes,No,A,Rectum,Tubular,11, x,severe,65,M,x,MSS\nP1C,yes,No,C,Rectum,Adenocarcinoma,11,A,moderate,65,M,?,MSS\nP2A,yes,No,A,Rectum,Tubulovillous,30, x,severe,71,F,x,?\nP2B,yes,No,C,Rectum,Adenocarcinoma,30,A,moderate,71,F,?,MSS\nP3A1,yes,No,A1 ,Rectosigmoid,Tubular,32, x,moderate,66,M,x,MSS\nP3C1,yes,No,C1,Rectosigmoid,Adenocarcinoma,10,B1,moderate,66,M,0,MSS\nP3A2,yes,No,A2 ,Rectosigmoid,Tubulovillous,29, x,severe,66,M,x,MSS\nP3C2,yes,No,C2,Rectosigmoid,Adenocarcinoma,65,B1,moderate,66,M,0,MSS\nP4A1,yes,No,A1,Sigmoid,Tubulovillous,20, x,moderate,66,M,x,MSS\nP4C1,yes,No,C1,Sigmoid,Adenocarcinoma,20,A,well,66,M,0,MSS\nP5A,yes,No,A,poliepectomie 20 CM,Tubular,25, x,severe,71,F,x,MSS\nP5C,yes,No,C,poliepectomie 20 CM,Adenocarcinoma,25,A,moderate,71,F,?,MSS\nP6A,yes,No,A2,left,Tubular,20, x,moderate,69,F,x,MSS\nP6C,yes,No,C,Rectosigmoid,Adenocarcinoma,20,A,moderate,69,F,0,?\nP7A,yes,No,A,Colon ascendens,Tubulovillous,60, x,moderate,53,F,x,MSI\nP7C,yes,No,C,Colon ascendens,Adenocarcinoma,60,C2,well,53,F,1,MSI\nP8A,yes,No,A ,Sigmoid,Tubulovillous,20, x,severe,86,F,x,MSS\nP8C,yes,No,C ,Sigmoid,Adenocarcinoma,20,B1,moderate,86,F,?,MSS\nP9A,yes,No,A,Coecum,Tubulovillous,80, x,severe,62,M,x,?\nP9C,yes,No,C,Coecum,Adenocarcinoma,80,B2,moderate,62,M,0,MSS\nP10A,yes,No,A2,Rectosigmoid,Tubulovillous,20, x,moderate,68,M,x,?\nP10C,yes,No,C1,Rectosigmoid,Adenocarcinoma,20,A,well,68,M,0,MSS\nP11A1,yes,No,A1, 35 CM,Tubular,18, x,severe,46,F,x,MSS\nP11A2,yes,No,A2, 20 CM,Tubulovillous,15, x,moderate,46,F,x,MSS\nP11C1,yes,No,C1, 35CM,Adenocarcinoma,18,A,well,46,F,?,MSS\nP11C2,yes,No,C2, 20 CM,Adenocarcinoma,15,A,moderate,46,F,?,MSS\nP12A,yes,No,A,Coecum/Colon ascendens,Tubulovillous,100, x,moderate,68,F,x,?\nP12C,yes,No,C,Coecum/Colon ascendens,Adenocarcinoma,100,B1,well,68,F,0,?\nP13A,yes,No,A,30cm van het coecum,Tubulovillous,40, x,moderate,45,M,x,MSS\nP13C,yes,No,C,30cm van het coecum,Adenocarcinoma,40,B1,well,45,M,0,MSI\nP14A,yes,No,A,Coecum,Tubulovillous,35, x,moderate,56,F,x,MSS\nP14C,yes,No,C,Coecum,mucinous,35,B2,moderate,56,F,0,MSS\nP15A,yes,No,A,Ileocoecaal,Tubulovillous,40, x,severe,77,M,x,MSS\nP15C,yes,No,C,Ileocoecaal,Adenocarcinoma,40,A,moderate,77,M,0,?\nP16A,yes,No,A,Coecum,Tubulovillous,8, x,severe,65,F,x,MSS\nP16C,yes,No,C,Coecum,Adenocarcinoma,8,A,well,65,F,?,?\nP17A,yes,No,A,poliep 20 CM,Tubular,10, x,severe,51,F,x,MSS\nP17C,yes,No,C,poliep 20 CM,Adenocarcinoma,10,A,well,51,F,?,?\nP18A,yes,No,A,rectosigmoid,Tubulovillous,30, x,moderate,71,F,x,?\nP18C,yes,No,C,rectosigmoid,Adenocarcinoma,30,A,well,71,F,0,?\nP19A1,yes,No,A1,Sigmoid proximal,Tubular,10, x,moderate,66,M,x,MSS\nP19C1,yes,No,C1,Sigmoid proximal,Adenocarcinoma,10,A,poor,66,M,?,?\nP19A2,yes,No,A2,distaal 15 cm,Tubular,22, x,severe,66,M,x,MSS\nP19C2,yes,No,C2,Sigmoid distaal,Adenocarcinoma,22,B,well,66,M,?,?\nP20A,yes,No,A2,poliep 7 CM,Tubular,10, x,severe,74,M,x,?\nP20C,yes,No,C,poliep 7 CM,Adenocarcinoma,10,A,moderate,74,M,?,?\nP21A,yes,No,A,Sigmoid,Tubulovillous,45, x,severe,81,F,x,?\nP21C,yes,No,C,Sigmoid,Adenocarcinoma,45,C2,moderate,81,F,1,?\nP22A,yes,No,A,Rectum,Tubulovillous,10, x,severe,68,M,x,MSS\nP22C,yes,No,C,Rectum,Adenocarcinoma,10,A,well,68,M,?,MSS\nP23A,yes,No,A,Sigmoid,Tubulovillous,65, x,severe,76,F,x,?\nP23C,yes,No,C,Sigmoid,Adenocarcinoma,65,D,poor,76,F,3,?\nP24A,yes,No,A,Poliep 20 CM,Tubulovillous,20, x,moderate,71,M,x,MSS\nP24C,yes,No,C,Poliep 20 CM,Adenocarcinoma,20,A,moderate,71,M,?,MSS\nP25C,yes,No,C,Rectum,Adenocarcinoma,60,?,moderate,59,F,1,MSS\nP25A,yes,No,A,Rectum,Tubular,60, x,severe,59,F,x,MSS\nP26C2,yes,No,C2,Sigmoid,Adenocarcinoma,7,A,moderate,75,M,?,?\nP26A2,yes,No,A2,Sigmoid,Tubular,7, x,moderate,75,M,x,?\nP26A1,yes,No,A1,Sigmoid,Tubulovillous,45, x,severe,75,M,x,MSS\nP26C1,yes,No,C1,Sigmoid,Adenocarcinoma,45,B,moderate,75,M,0,?\nP27C,yes,No,C,Rectum,Adenocarcinoma,10,A,moderate,70,M,?,MSS\nP27A,yes,No,A,Rectum,Tubular,10, x,moderate,70,M,x,MSS\nP28A,yes,No,A,Sigmoid,Tubulovillous,12, x,severe,69,M,x,?\nP28C,yes,No,C,Sigmoid,Adenocarcinoma,15,?,moderate,69,M,?,MSS\nP29C,yes,No,C,Right,Adenocarcinoma,60,?,moderate,58,F,6,MSS\nP29A,yes,No,A,Right,Tubular,60, x,moderate,58,F,x,MSS\nP30A,yes,No,A,flexura lienalis,Tubulovillous,12, x,moderate,66,M,x,?\nP30C,yes,No,C,flexura lienalis,Adenocarcinoma,12,?,well,66,M,?,MSS\nP31C,yes,No,C,Rectosigmoid,Adenocarcinoma,40,A,moderate,83,M,0,MSS\nP31A,yes,No,A,Rectosigmoid,Tubulovillous,40, x,moderate,83,M,x,MSS\nP32C,yes,No,C,?,Adenocarcinoma,23,B,moderate,79,F,0,MSS\nP32A,yes,No,A,?,Tubulovillous,23, x,moderate,79,F,x,MSS\nP33C,yes,No,C,?,Adenocarcinoma,23,?,moderate,?,F,?,?\nP33A,yes,No,A,?,Tubulovillous,23, x,moderate,?,F,x,MSS\nP34A,yes,No,A,Low anterior,Tubulovillous,35, x,severe,73,F,x,MSS\nP34C,yes,No,C,Low anterior,Adenocarcinoma,35,?,poor,73,F,6,?\nP35C,yes,No,C,Sigmoid,Adenocarcinoma,20,A,well,63,M,?,MSS\nP35A,yes,No,A2,Sigmoid,Tubulovillous,20, x,severe,63,M,x,?\nP36C,yes,No,C2,Colon transversum,Adenocarcinoma,55,A,moderate,65,F,0,MSS\nP36A,yes,No,A,Colon transversum,Tubulovillous,55, x,moderate,65,F,x,MSS\nP37C,yes,No,C,Rectosigmoid,Adenocarcinoma,35,A,moderate,72,M,?,MSS\nP37A,yes,No,A,Rectosigmoid,Tubular,35, x,moderate,72,M,x,MSS";
   	}

   	private List<Map<String, String>> parsePhenotypeFile(String phenotypeFileString,
			List<String> columns, String delimiter, boolean quoted) {
   		List<Map<String, String>> file = new ArrayList<Map<String, String>>();
   		String[] lines = phenotypeFileString.split("\\n"); // TODO : CHANGE THIS to \n after static data is gone
   		if(lines.length > 0) {	   		
   			for(int i=1; i<lines.length; i++) {
   				Map<String, String> row = new HashMap<String, String>();
   				String[] cols = lines[i].split(delimiter);
   				for(int c = 0; c<cols.length; c++) {
   					if(c < columns.size()) {
   						row.put(columns.get(c), cols[c]);
   					}
   				}
   				file.add(row);
   			}
   		}
   		return file;
	}

	private List<String> parseColumnsPhenotypeFile(String phenotypeFileString,
			String delimiter, boolean quoted) {
		List<String> columns = null;
   		String[] lines = phenotypeFileString.split("\\n"); // TODO : CHANGE THIS to \n after static data is gone
   		if(lines.length > 0) {
	   		columns = Arrays.asList(lines[0].split(delimiter));
   		}
   		return columns;
	}

}
