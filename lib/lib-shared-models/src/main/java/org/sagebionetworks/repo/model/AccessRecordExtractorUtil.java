package org.sagebionetworks.repo.model;

import java.lang.reflect.Field;

/**
 * Extract the ID and concreteType from objects.
 *
 * @author John
 */
public class AccessRecordExtractorUtil {
    /**
     * Exact the ID from an object
     *
     * @param object
     * @return
     */
    public static String getObjectId(Object object) {
        if (object == null) return null;
        try {
            Field field = object.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object ob = field.get(object);
            if (ob instanceof String) {
                return (String) ob;
            } else if (ob instanceof Long) {
                return ((Long) ob).toString();
            } else return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Exact the concrete type from an object
     *
     * @param object
     * @return
     */
    public static String getConcreteType(Object object) {
        if (object == null) return null;
        try {
            Field field = object.getClass().getDeclaredField("concreteType");
            field.setAccessible(true);
            Object ob = field.get(object);
            if (ob instanceof String) {
                return (String) ob;
            } else if (ob instanceof Long) {
                return ((Long) ob).toString();
            } else return null;
        } catch (Exception e) {
            return null;
        }
    }
}
