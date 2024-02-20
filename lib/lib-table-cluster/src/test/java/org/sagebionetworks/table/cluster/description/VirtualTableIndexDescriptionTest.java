package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;

@ExtendWith(MockitoExtension.class)
public class VirtualTableIndexDescriptionTest {
	
	@Mock
	private IndexDescriptionLookup mockLookup;
	@Mock
	private IndexDescription mockIndexDescription;
	
	private IdAndVersion idAndVersion;
	private String definingSql;
	
	@BeforeEach
	public void before() {
		idAndVersion = IdAndVersion.parse("syn1");
		definingSql = "select * from syn2";
	}

	@Test
	public void testConstructor() {
		when(mockLookup.getIndexDescription(any())).thenReturn(mockIndexDescription);
		
		// call under test
		VirtualTableIndexDescription vtd = new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		
		assertEquals(idAndVersion, vtd.getIdAndVersion());
		assertEquals(TableType.virtualtable, vtd.getTableType());
		assertEquals(List.of(mockIndexDescription), vtd.getDependencies());
		assertEquals("WITH syn1 AS (select * from syn2) select * from syn1", vtd.preprocessQuery("select * from syn1"));
		
		verify(mockLookup).getIndexDescription(IdAndVersion.parse("syn2"));
		
	}
	
	@Test
	public void testConstructorWithJoin() {
		definingSql = "select * from syn2 join syn2 on syn2.id = syn2.id";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);
	}
	
	@Test
	public void testConstructorWithRecursiveRefrence() {
		definingSql = "select * from syn1";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		}).getMessage();
		assertEquals("Defining SQL cannot reference itself", message);
		verifyZeroInteractions(mockLookup);
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
	
	@Test
	public void testSupportQueryCache() {
		when(mockLookup.getIndexDescription(any())).thenReturn(mockIndexDescription);
		
		VirtualTableIndexDescription vtd = new VirtualTableIndexDescription(idAndVersion, definingSql, mockLookup);
		
		// call under test
		assertTrue(vtd.supportQueryCache());
	}
}
