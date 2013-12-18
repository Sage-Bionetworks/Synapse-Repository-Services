package org.sagebionetworks.evaluation.dao.principal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;

public class AliasUtilsTest {

    @Test
    public void testGetUniquePrincipalNameCase(){
            String input = "BigTop";
            String expected = "bigtop";
            String result = AliasUtils.getUniqueAliasName(input);
            assertEquals(expected, result);
    }
    
    @Test
    public void testGetUniquePrincipalNameSpace(){
            String input = "Big Top";
            String expected = "bigtop";
            String result = AliasUtils.getUniqueAliasName(input);
            assertEquals(expected, result);
    }
    
    @Test
    public void testGetUniquePrincipalNameDash(){
            String input = "Big-Top";
            String expected = "bigtop";
            String result = AliasUtils.getUniqueAliasName(input);
            assertEquals(expected, result);
    }
    
    @Test
    public void testGetUniquePrincipalNameAll(){
            String input = "1.2 3-4_567890AbCdEfGhIJklmnoPqRSTUvwxyz";
            String expected = "1234567890abcdefghijklmnopqrstuvwxyz";
            String result = AliasUtils.getUniqueAliasName(input);
            assertEquals(expected, result);
    }

}
