package org.sagebionetworks.table.query;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CurrentUserFunction;



import static org.junit.jupiter.api.Assertions.assertEquals;


public class CurrentUserTest {

        @Test
        public void testCurrentUser() throws ParseException{
            CurrentUserFunction element = new TableQueryParser("current_user()").currentUserFunction();
            assertEquals("CURRENT_USER()", element.toSql());
            //assertEquals(FunctionReturnType.STRING, element.getFunctionReturnType());
        }
}
