package org.sagebionetworks.repo.model.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
	
	@Test
	public void testGetViewTypeMaskBothNull() {
		ViewType viewType = null;
		Long viewTypeMask = null;

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
		});
	}
	
	@Test
	public void testGetViewTypeMaskBothNotNull() {
		ViewType viewType = ViewType.file_and_table;
		Long viewTypeMask = ViewTypeMask.File.getMask();
		// call under test
		long result = ViewTypeMask.getViewTypeMask(viewType, viewTypeMask);
		// for this case 
		assertEquals(ViewTypeMask.File.getMask(), result);
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
	
	@Test
	public void testGetViewTypeMaskViewScopeNull() {
		ViewScope scope = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			ViewTypeMask.getViewTypeMask(scope);
		});
	}
}
