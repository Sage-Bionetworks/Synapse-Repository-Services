package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StoredLayerPreview;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider.EventType;

/**
 * 
 * @author jmhill
 *
 */
public class StoredLayerPreviewMetadataProviderTest {
	
	UserInfo mockUser;
	HttpServletRequest mockRequest;
	private int numberCols = 5;
	private int numberRows = 5;
	private String previewString;
	@Before
	public void before(){
		// Setup the mocks
		mockUser = Mockito.mock(UserInfo.class);
		mockRequest = Mockito.mock(HttpServletRequest.class);
		// Build up a preview string.
		StringBuilder builder = new StringBuilder();
		// Create the headers
		for(int col=0; col<numberCols; col++){
			if(col > 0) {
				builder.append("\t");
			}
			builder.append("header");
			builder.append(col);
		}
		builder.append("\n");
		// Now add the cells
		for(int col=0; col<numberCols; col++){
			for(int row=0; row<numberRows; row++){
				if(row > 0){
					builder.append("\t");
				}
				builder.append(col);
				builder.append(".");
				builder.append(row);
				if(row == numberRows-1){
					builder.append("\n");
				}
			}
		}
		previewString = builder.toString();
	}
	
	@Test
	public void testCreatePreviewMap() throws DatastoreException{
		// Seup the preview
		StoredLayerPreview preview = new StoredLayerPreview();
		preview.setPreviewString(previewString);
		// Now create the map and headers
		StoredLayerPreviewMetadataProvider.createPreviewMap(preview);
		assertNotNull(preview.getHeaders());
		assertEquals(numberCols, preview.getHeaders().length);
		assertNotNull(preview.getRows());
		assertEquals(numberRows, preview.getRows().size());
		// Check the headers
		assertEquals("header3", preview.getHeaders()[3]);
		Map<String, String> row = preview.getRows().get(0);
		assertNotNull(row);
		assertEquals("0.3",row.get("header3"));
	}
	
	@Test
	public void testValidateEntity() throws UnsupportedEncodingException{
		StoredLayerPreview preview = new StoredLayerPreview();
		preview.setPreviewString(previewString);
		StoredLayerPreviewMetadataProvider provider = new StoredLayerPreviewMetadataProvider();
		provider.validateEntity(preview, null, EventType.GET);
		assertNotNull(preview.getPreviewBlob());
		assertEquals(previewString, new String(preview.getPreviewBlob(), "UTF-8"));
		assertTrue(preview.getPreviewString() == null);
	}
	
	@Test
	public void testAddTypeSpecificMetadata() throws UnsupportedEncodingException{
		StoredLayerPreview preview = new StoredLayerPreview();
		preview.setPreviewBlob(previewString.getBytes("UTF-8"));
		StoredLayerPreviewMetadataProvider provider = new StoredLayerPreviewMetadataProvider();
		provider.addTypeSpecificMetadata(preview, mockRequest, mockUser, EventType.GET);
		assertTrue(preview.getPreviewBlob() == null);
		assertEquals(previewString, preview.getPreviewString());
		assertNotNull(preview.getHeaders());
		assertEquals(numberCols, preview.getHeaders().length);
		assertNotNull(preview.getRows());
		assertEquals(numberRows, preview.getRows().size());
	}

}
