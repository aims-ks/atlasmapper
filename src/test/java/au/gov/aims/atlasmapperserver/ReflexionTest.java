/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author glafond
 */
public class ReflexionTest extends TestCase {
	public Map<String, Boolean> map;

	public void testAutonomeGetMapValueType() throws NoSuchFieldException, NoSuchMethodException {
		this.map = new HashMap<String, Boolean>();

		Field mapField = this.getClass().getDeclaredField("map");
		Class mapClass = mapField.getType();
		Method mapGetter = mapClass.getDeclaredMethod("get", Object.class);

		Type mapValueType = mapGetter.getReturnType(); // Object
		Type mapValueGenericType = mapGetter.getGenericReturnType(); // V
		// V can not be cast into ParameterizedType
		//Type[] mapValueActualTypes = ((ParameterizedType)mapValueGenericType).getActualTypeArguments();

		System.out.println("mapField: " + mapField);
		System.out.println("mapClass: " + mapClass);
		System.out.println("mapGetter: " + mapGetter);
		System.out.println("mapValueType: " + mapValueType);
		System.out.println("mapValueGenericType: " + mapValueGenericType);
		//System.out.println("mapValueActualTypes: " + mapValueActualTypes);
	}

	// http://stackoverflow.com/questions/1764586/java-reflection-what-does-my-collection-contain
	public void testGetCollectionValueType() throws NoSuchFieldException, NoSuchMethodException {
		Method m = this.getClass().getMethod("getCollection");
		Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];

		System.out.println("Collection Parameter: " + parameter);
	}

	public void testGetCollectionListValueType() throws NoSuchFieldException, NoSuchMethodException {
		Method m = this.getClass().getMethod("getCollectionList");
		Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];

		System.out.println("Collection List Parameter: " + parameter);
	}

	public void testGetMapValueType() throws NoSuchFieldException, NoSuchMethodException {
		Method m = this.getClass().getMethod("getMap");
		Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[1];

		System.out.println("Map Parameter: " + parameter);
	}

	public Collection<Boolean> getCollection() { return null; }
	public Map<String, Boolean> getMap() { return null; }
	public Collection<List<Boolean>> getCollectionList() { return null; }
}
