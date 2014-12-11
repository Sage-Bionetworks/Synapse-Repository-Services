package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Row;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.sagebionetworks.repo.web.service.metadata.PreviewMetadataProvider;

/**
 * 
 * @author jmhill
 *
 */
public class PreviewMetadataProviderTest {
	
	UserInfo mockUser;
	HttpServletRequest mockRequest;
	private int numberCols = 5;
	private int numberRows = 5;
	private String previewString;
	
	private String examplePreview64 = "cGhlbm90eXBlX2lkCXNhbXBsZV90eXBlCW1ldGFzdGF0aWNfc2l0ZQlldGhuaWNpdHkJcHJlZHhieHBzYQlhZ2UJY2xpbmljYWxfcHJpbWFyeV9nbGVhc29uCWNsaW5pY2FsX3NlY29uZGFyeV9nbGVhc29uCWNsaW5pY2FsX2dsZWFzb25fc2NvcmUJcHJlX3RyZWF0bWVudF9wc2EJY2xpbmljYWxfdG5tX3N0YWdlX3QJbmVvYWRqcmFkdHgJY2hlbW90eAlob3JtdHgJcmFkdHh0eXBlCXJwX3R5cGUJc21zCWV4dHJhX2NhcHN1bGFyX2V4dGVuc2lvbglzZW1pbmFsX3Zlc2ljbGVfaW52YXNpb24JdG5tX3N0YWdlX24JbnVtYmVyX25vZGVzX3JlbW92ZWQJbnVtYmVyX25vZGVzX3Bvc2l0aXZlCXBhdGhvbG9naWNfdG5tX3N0YWdlX3QJcGF0aG9sb2dpY19wcmltYXJ5X2dsZWFzb24JcGF0aG9sb2dpY19zZWNvbmRhcnlfZ2xlYXNvbglwYXRob2xvZ2ljX2dsZWFzb25fc2NvcmUJYmNyX2ZyZWV0aW1lCWJjcl9ldmVudAltZXRzZXZlbnQJc3VydnRpbWUJZXZlbnQJbm9tb2dyYW1fcGZwX3Bvc3RycAlub21vZ3JhbV9ub21vcHJlZF9leHRyYV9jYXBzdWxhcl9leHRlbnNpb24Jbm9tb2dyYW1fbm9tb3ByZWRfbG5pCW5vbW9ncmFtX25vbW9wcmVkX29jZAlub21vZ3JhbV9ub21vcHJlZF9zZW1pbmFsX3Zlc2ljbGVfaW52YXNpb24JY29weV9udW1iZXJfY2x1c3RlcglleHByZXNzaW9uX2FycmF5X3Rpc3N1ZV9zb3VyY2UNClBDQTAwMDQJUFJJTUFSWQlOQQlXaGl0ZSBOb24gSGlzcGFuaWMJMjcuNQk2OC45MwkzCTIJNQkxMS44CVQyQglOQQlOQQlOQQlOQQlSUAlOZWdhdGl2ZQlFU1RBQkxJU0hFRAlOZWdhdGl2ZQlOb3JtYWxfTjAJMTMJMAlUM0EJMwk0CTcJMTUyLjU1CU5PCU5PCTE1Mi41NQlOTwlOQQkzNy45Mzc4NDYJMy41OTM5NzQJNTUuMDgyOTM5CU5BCTEJTkENClBDQTAwMDYJUFJJTUFSWQlOQQlXaGl0ZSBOb24gSGlzcGFuaWMJMTUuNwk1Ni42NAkzCTMJNgk4LjIJVDJCCU5BCU5BCU5lb2FkanV2YW50IEhPUk0JTkEJUlAJTmVnYXRpdmUJTk9ORQlOZWdhdGl2ZQlOb3JtYWxfTjAJNAkwCVQyQwkzCTMJNgkxNjAuOTYJTk8JTk8JMTYwLjk2CU5PCU5BCU5BCU5BCU5BCU5BCTQJTkENClBDQTAwMTYJUFJJTUFSWQlOQQlXaGl0ZSBOb24gSGlzcGFuaWMJMTIJNjcuMzYJMwkzCTYJMTIJVDJCCU5BCU5BCU5lb2FkanV2YW50IEhPUk0JTkEJUlAJTmVnYXRpdmUJTk9ORQlOZWdhdGl2ZQlOb3JtYWxfTjAJMgkwCVQyQwk0CTQJOAk3NC4yMglOTwlOTwk3NC4yMglOTwk5OQlOQQlOQQlOQQk5Ny4xMTAxNTQ2NQkyCU5BDQpQQ0EwMDE5CVBSSU1BUlkJTkEJV2hpdGUgTm9uIEhpc3BhbmljCTYuNgk2OC4xMgkzCTQJNwk2LjYJVDFDCU5BCU5BCU5BCU5BCVJQCU5lZ2F0aXZlCU5PTkUJTmVnYXRpdmUJTm9ybWFsX04wCTEJMAlUMkMJMwkzCTYJMTEwLjMzCUJDUl9BbGdvcml0aG0JTk8JMTIzLjY3CU5PCU5BCU5BCU5BCU5BCTc5Ljg1NTQ1NjUyCTIJTkENClBDQTAwMjMJUFJJTUFSWQlOQQlCbGFjayBOb24gSGlzcGFuaWMJNC4zCTYwLjU3CTQJMwk3CTMuODgJVDFDCU5BCU5BCVBvc3RIT1JNCU5BCVJQCVBvc2l0aXZlCU5PTkUJTmVnYXRpdmUJTm9ybWFsX04wCTIJMAlUMkMJNAk1CTkJMTAuNjEJQkNSX0FsZ29yaXRobQlOTwk3Mi44NAlERUFUSCBGUk9NIE9USEVSIENBTkNFUgk3OS44NTU0NgkxOS4xOTAyMDgJMi4xMzg5MzgJNzcuMjQwMDQ1CTk5CTQJTkENCg==";
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
		Preview preview = new Preview();
		preview.setPreviewString(previewString);
		// Now create the map and headers
		PreviewMetadataProvider.createPreviewMap(preview);
		assertNotNull(preview.getHeaders());
		assertEquals(numberCols, preview.getHeaders().size());
		assertNotNull(preview.getRows());
		assertEquals(numberRows, preview.getRows().size());
		// Check the headers
		assertEquals("header3", preview.getHeaders().get(3));
		Row row = preview.getRows().get(0);
		assertNotNull(row);
		assertEquals("0.3",row.getCells().get(3));
	}
	
//	@Test
//	public void testValidateEntity() throws UnsupportedEncodingException{
//		Preview preview = new Preview();
//		preview.setPreviewString(previewString);
//		PreviewMetadataProvider provider = new PreviewMetadataProvider();
//		provider.validateEntity(preview,  new EntityEvent(EventType.GET, null, null));
//		assertNotNull(preview.getPreviewBlob());
//		assertEquals(previewString, new String(preview.getPreviewBlob(), "UTF-8"));
//		assertTrue(preview.getPreviewString() == null);
//	}
	
	@Test
	public void testAddTypeSpecificMetadata() throws UnsupportedEncodingException{
		Preview preview = new Preview();
		preview.setPreviewString(previewString);
//		preview.setPreviewString(previewString.getBytes("UTF-8"));
		PreviewMetadataProvider provider = new PreviewMetadataProvider();
		provider.addTypeSpecificMetadata(preview, mockRequest, mockUser, EventType.GET);
//		assertTrue(preview.getPreviewBlob() == null);
		assertEquals(previewString, preview.getPreviewString());
		assertNotNull(preview.getHeaders());
		assertEquals(numberCols, preview.getHeaders().size());
		assertNotNull(preview.getRows());
		assertEquals(numberRows, preview.getRows().size());
	}
	
	@Test
	public void testAddTypeSpecificMetadataWithExample() throws UnsupportedEncodingException{
		String example = new String(Base64.decodeBase64(examplePreview64.getBytes("UTF-8")),"UTF-8");
		Preview preview = new Preview();
		preview.setPreviewString(example);
//		preview.setPreviewString(previewString.getBytes("UTF-8"));
		PreviewMetadataProvider provider = new PreviewMetadataProvider();
		provider.addTypeSpecificMetadata(preview, mockRequest, mockUser, EventType.GET);
//		assertTrue(preview.getPreviewBlob() == null);
		assertEquals(example, preview.getPreviewString());
		assertNotNull(preview.getHeaders());
		assertEquals(38, preview.getHeaders().size());
		assertNotNull(preview.getRows());
		assertEquals(numberRows, preview.getRows().size());
	}

}
