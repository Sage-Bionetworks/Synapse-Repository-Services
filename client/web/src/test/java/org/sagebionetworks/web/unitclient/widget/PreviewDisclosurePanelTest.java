package org.sagebionetworks.web.unitclient.widget;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl;
import org.sagebionetworks.web.client.view.DynamicTableViewImpl.Binder;
import org.sagebionetworks.web.util.MockitoMockFactory;

import com.google.gwt.cell.client.widget.CustomWidgetImageBundle;
import com.google.gwt.cell.client.widget.PreviewDisclosurePanel;
import com.google.gwt.junit.GWTMockUtilities;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.tester.MockingBinder;

public class PreviewDisclosurePanelTest {

	public static class TestBinder extends MockingBinder<Widget, DynamicTableViewImpl> implements Binder {
		public TestBinder(MockitoMockFactory mockFactory) {
			super(Widget.class, mockFactory);
		}
	}

	PreviewDisclosurePanel previewDisclosurePanel;
	CustomWidgetImageBundle bundle;

	@Before
	public void setup() {
		GWTMockUtilities.disarm();
		TestBinder binder = new TestBinder(new MockitoMockFactory());
		// Mock the client bundle
		bundle = Mockito.mock(CustomWidgetImageBundle.class);
		FlexTable flexTable = Mockito.mock(FlexTable.class);
		HTML previewHtml = Mockito.mock(HTML.class);
		HTML contentHtml = Mockito.mock(HTML.class);
		previewDisclosurePanel = new PreviewDisclosurePanel(bundle, flexTable, previewHtml, contentHtml);
	}

	@Test
	public void testUninitialized() {
		previewDisclosurePanel.setCaption(null);
		String caption = previewDisclosurePanel.getCaption();
		assertEquals("", caption);
		
		previewDisclosurePanel.setPreview(null);
		String preview = previewDisclosurePanel.getPreview();
		assertEquals("", preview);
		
		previewDisclosurePanel.setContent(null);
		String content = previewDisclosurePanel.getContent();
		assertEquals("", content);
	}
	
	@Test
	public void testSettersAndGetters() {
		String caption = "caption";
		String preview = "preview";
		String content = "content";
		previewDisclosurePanel.setCaption(caption);
		String captionAct = previewDisclosurePanel.getCaption();
		assertEquals(caption, captionAct);
		
		previewDisclosurePanel.setPreview(preview);
		String previewAct = previewDisclosurePanel.getPreview();
		assertEquals(preview, previewAct);
		
		previewDisclosurePanel.setContent(content);
		String contentAct = previewDisclosurePanel.getContent();
		assertEquals(content, contentAct);
	}
	
	
	@After
	public void tearDown() {
		// Be kind to the next test
		GWTMockUtilities.restore();
	}

}
