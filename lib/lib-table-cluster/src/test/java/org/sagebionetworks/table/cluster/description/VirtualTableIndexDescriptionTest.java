package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

@ExtendWith(MockitoExtension.class)
public class VirtualTableIndexDescriptionTest {
	
	@Mock
	private IndexDescriptionLookup mockLookup;
	
	private IdAndVersion idAndVersion;
	private String definingSql;
	private IndexDescription sourceIndexDescription;
	
	@BeforeEach
	public void before() {
		idAndVersion = IdAndVersion.parse("syn1");
		definingSql = "select * from syn2";
		sourceIndexDescription = new TableIndexDescription(IdAndVersion.parse("syn2"));
	}

	@Test
	public void testConstructor() {
		when(mockLookup.getIndexDescription(any())).thenReturn(sourceIndexDescription);
		
		// call under test
		VirtualTableIndexDescription vtd = new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		
		assertEquals(idAndVersion, vtd.getIdAndVersion());
		assertEquals(TableType.virtualtable, vtd.getTableType());
		assertEquals(List.of(sourceIndexDescription), vtd.getDependencies());
		assertEquals("WITH syn1 AS (select * from syn2) select * from syn1", vtd.preprocessQuery("select * from syn1"));
		
		verify(mockLookup).getIndexDescription(IdAndVersion.parse("syn2"));
		
	}
	
	@Test
	public void testConstructorWithNullIdAndVersion() {
		idAndVersion = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		}).getMessage();
		assertEquals("idAndVersion is required.", message);
	}
	
	@Test
	public void testConstructorWithNullDefiningSql() {
		definingSql = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		}).getMessage();
		assertEquals("definingSql is required.", message);
	}
	
	@Test
	public void testConstructorWithNullLookup() {
		mockLookup = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		}).getMessage();
		assertEquals("IndexDescriptionLookup is required.", message);
	}
}
