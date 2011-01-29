package org.sagebionetworks.web.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface SageImageBundle extends ClientBundle {

	@Source("images/icon-gene-expression-16.png")
	ImageResource iconGeneExpression16();

	@Source("images/icon-phenotypes-16.png")
	ImageResource iconPhenotypes16();
	
	@Source("images/icon-genotypes-16.png")
	ImageResource iconGenotype16();

	@Source("images/SagebionetworksLogo.png")
	ImageResource sageLogo();
	
	@Source("images/SagebionetworksLogo&Title.png")
	ImageResource sageLogoAndTitle();
	
	@Source("images/transparent-16.png")
	ImageResource iconTransparent16();
	
	@Source("images/upArrow.png")
	ImageResource iconUpArrow();
	
	@Source("images/downArrow.png")
	ImageResource iconDownArrow();

}
