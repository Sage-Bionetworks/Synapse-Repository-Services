package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.TextMatchesMySQLPredicate;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.UnsignedLiteral;

@ExtendWith(MockitoExtension.class)
public class TextMatchesMySQLPredicateTest {
	
	@Mock
	private TextMatchesPredicate mockInputPredicate;
	
	@Mock
	private UnsignedLiteral mockLiteral;
	
	@Mock
	private ColumnReference mockColumnReference;
	
	@InjectMocks
	private TextMatchesMySQLPredicate predicate;
	
	@Test
	public void testToSQL() {
		when(mockInputPredicate.getSearchExpression()).thenReturn(mockLiteral);
		doNothing().when(mockLiteral).toSql(any(), any());
		
		// Call under test
		String sql = predicate.toSql();
		
		assertEquals("MATCH(ROW_SEARCH_CONTENT) AGAINST()", sql);
		
		verify(mockInputPredicate).getSearchExpression();
		verify(mockLiteral).toSql(any(), any());
	}
	
	@Test
	public void testGetChildren() {		
		Iterable<Element> expectedChildren = Collections.singleton(mockLiteral);
		when(mockInputPredicate.getChildren()).thenReturn(expectedChildren);
		
		// Call under test
		Iterable<Element> children = predicate.getChildren();
		
		assertEquals(children, children);
		
		verify(mockInputPredicate).getChildren();
	}
	
	@Test
	public void testGetLeftHandSide() throws ParseException {
		when(mockInputPredicate.getLeftHandSide()).thenReturn(mockColumnReference);
				
		// Call under test
		ColumnReference columnReference = predicate.getLeftHandSide();

		assertEquals(mockColumnReference, columnReference);

		verify(mockInputPredicate).getLeftHandSide();
	}
	
	@Test
	public void testGetRightHandSideValues() {
		Iterable<UnsignedLiteral> expectedRightHandSideValues = Collections.singleton(mockLiteral);
		when(mockInputPredicate.getRightHandSideValues()).thenReturn(expectedRightHandSideValues);
		
		// Call under test
		Iterable<UnsignedLiteral> rightHandSideValues = predicate.getRightHandSideValues();

		assertEquals(expectedRightHandSideValues, rightHandSideValues);
		
		verify(mockInputPredicate).getRightHandSideValues();
	}

}
