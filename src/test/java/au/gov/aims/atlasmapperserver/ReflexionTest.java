/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.gov.aims.atlasmapperserver;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author glafond
 */
public class ReflexionTest {
    public Map<String, Boolean> map;

    @Test
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
    @Test
    public void testGetCollectionValueType() throws NoSuchFieldException, NoSuchMethodException {
        Method m = this.getClass().getMethod("getCollection");
        Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];

        System.out.println("Collection Parameter: " + parameter);
    }

    @Test
    public void testGetCollectionListValueType() throws NoSuchFieldException, NoSuchMethodException {
        Method m = this.getClass().getMethod("getCollectionList");
        Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];

        System.out.println("Collection List Parameter: " + parameter);
    }

    @Test
    public void testGetMapValueType() throws NoSuchFieldException, NoSuchMethodException {
        Method m = this.getClass().getMethod("getMap");
        Type parameter = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[1];

        System.out.println("Map Parameter: " + parameter);
    }

    // TODO Implement
    public Collection<Boolean> getCollection() { return null; }
    public Map<String, Boolean> getMap() { return null; }
    public Collection<List<Boolean>> getCollectionList() { return null; }
}
