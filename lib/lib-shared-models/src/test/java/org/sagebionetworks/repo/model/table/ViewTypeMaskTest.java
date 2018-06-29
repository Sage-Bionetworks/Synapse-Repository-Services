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
}
