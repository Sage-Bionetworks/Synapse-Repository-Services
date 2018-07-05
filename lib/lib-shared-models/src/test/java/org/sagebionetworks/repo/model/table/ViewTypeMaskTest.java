package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ViewTypeMaskTest {

	
	@Test
	public void testGetMaskForDepricatedTypeFile() {
		long expected = ViewTypeMask.File.getMask();
		assertEquals(expected, ViewTypeMask.getMaskForDepricatedType(ViewType.file));
	}
	
	@Test
	public void testGetMaskForDepricatedTypeProject() {
		long expected = ViewTypeMask.Project.getMask();
		assertEquals(expected, ViewTypeMask.getMaskForDepricatedType(ViewType.project));
	}
	
	@Test
	public void testGetMaskForDepricatedTypeFileAndTable() {
		long expected = ViewTypeMask.File.getMask() | ViewTypeMask.Table.getMask();
		assertEquals(expected, ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetViewTypeMaskBothNull() {
		ViewType viewType = null;
		Long viewTypeMask = null;
		// call under test
		ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetViewTypeMaskBothNotNull() {
		ViewType viewType = ViewType.file;
		Long viewTypeMask = ViewTypeMask.File.getMask();
		// call under test
		ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
	}
	
	@Test
	public void testGetViewTypeMaskOldType() {
		ViewType viewType = ViewType.project;
		Long viewTypeMask = null;
		// call under test
		long result = ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
		assertEquals(ViewTypeMask.Project.getMask(), result);
	}
	
	@Test
	public void testGetViewTypeMaskMask() {
		ViewType viewType = null;
		Long viewTypeMask = ViewTypeMask.Project.getMask();
		// call under test
		long result = ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
		assertEquals(ViewTypeMask.Project.getMask(), result);
	}
	
	@Test
	public void testGetViewTypeMaskViewScope() {
		ViewScope scope = new ViewScope();
		scope.setViewTypeMask(ViewTypeMask.Project.getMask());
		// call under test
		long result = ViewTypeMask.getViewTypeMask(scope);
		assertEquals(ViewTypeMask.Project.getMask(), result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetViewTypeMaskViewScopeNull() {
		ViewScope scope = null;
		// call under test
		ViewTypeMask.getViewTypeMask(scope);
	}
}
