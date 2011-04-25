package javax.jdo;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * A helper class that allows us to load the JDO properties from the "jdoconfig.xml" file.
 * @author jmhill
 *
 */
public class SageJDOHelper extends JDOHelper{
	
	/**
	 * Get the JDO properties from the "jdoconfig.xml" file.
	 * @param name
	 * @return
	 */
	public static Map<Object,Object> getPropertiesFromJDOConfig(String name){
		ClassLoader cl = SageJDOHelper.getContextClassLoader();
		return JDOHelper.getPropertiesFromJdoconfig(name, cl);
	}
	
	/**
	 * @See {@link JDOHelper#getContextClassLoader()}
	 * @return
	 */
    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader> () {
                    public ClassLoader run () {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
    }

}
