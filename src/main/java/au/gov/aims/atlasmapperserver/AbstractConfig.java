/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author glafond
 */
public abstract class AbstractConfig implements Cloneable {
	private static final Logger LOGGER = Logger.getLogger(AbstractConfig.class.getName());
	protected static final String SPLIT_PATTERN = "[,\r\n]";
	protected static final String SPLIT_ATTRIBUTES_PATTERN = "=";

	private static final int DEFAULT_NUMBER = -1;
	private static final boolean DEFAULT_BOOLEAN = false;

	private ConfigManager configManager;

	// Describe the needed constructors
	public AbstractConfig (ConfigManager configManager) {
		this.configManager = configManager;
	}

	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	/**
	 * This method is call to set the value of a JSONObject into a field
	 * of the config.
	 * Example:
	 * "blue": {
	 *     "hexa": "#0000FF"
	 * }
	 * will call:
	 *     instance.setHexa("#0000FF");
	 *     instance.setJSONObjectKey("blue");
	 * @param key
	 */
	public abstract void setJSONObjectKey(String key);
	public abstract String getJSONObjectKey();

	protected static Set<String> toSet(String setStr) {
		Set<String> set = new HashSet<String>();
		String[] strArray = setStr.split(SPLIT_PATTERN);
		if (strArray != null) {
			for (int i=0; i<strArray.length; i++) {
				String str = strArray[i];
				if (Utils.isNotBlank(str)) {
					set.add(str.trim());
				}
			}
		}
		return set;
	}

	/**
	 * This method use reflexion to get all field annoted with ConfigField,
	 * find the getter for the field, call the getter and fill the JSONObject
	 * using the values found.
	 * @return
	 * @throws JSONException
	 */
	public JSONObject toJSonObject() throws JSONException {
		JSONObject jsonObj = new JSONObject();

		// Get all fields; public, protected and private
		//Field fields[] = this.getClass().getDeclaredFields();
		List<Field> fields = Utils.getAllFields(this.getClass());
		for (Field field : fields) {
			ConfigField annotation = field.getAnnotation(ConfigField.class);

			if (annotation != null) {
				Method getter = _getFieldGetter(field, annotation);
				if (getter != null) {
					String configName = annotation.name();
					if (configName == null || configName.length() <= 0) {
						configName = field.getName();
					}
					Object rawValue = null;
					try {
						rawValue = getter.invoke(this);
					} catch (Exception ex) {
						LOGGER.log(Level.SEVERE, "Can not call the method ["+getter.getName()+"] for the field ["+field.getName()+"]", ex);
					}

					Object cleanValue = cleanValue(rawValue);
					if (cleanValue != null) {
						jsonObj.put(configName, cleanValue);
					}
				}
			}
		}

		return jsonObj;
	}

	/**
	 * Clean a value
	 * * return null for empty String,
	 * * remove null, JSON Null or empty String from JSONArrays / JSONObjects,
	 * * return null instead of empty JSONArrays / JSONObjects
	 * @param rawValue
	 * @return
	 */
	private Object cleanValue(Object rawValue) throws JSONException {
		if (rawValue == null) { return null; }

		if (rawValue instanceof String) {
			if (Utils.isNotBlank((String)rawValue)) {
				return rawValue;
			} else {
				return null;
			}

		} else if (rawValue instanceof JSONArray) {
			JSONArray rawJsonArray = (JSONArray)rawValue;
			JSONArray jsonArray = new JSONArray();
			// Remove null entries
			for (int i=0; i<rawJsonArray.length(); i++) {
				if (!rawJsonArray.isNull(i)) {
					Object value = cleanValue(rawJsonArray.opt(i));
					if (value != null) {
						jsonArray.put(value);
					}
				}
			}
			if (jsonArray.length() > 0) {
				return jsonArray;
			} else {
				return null;
			}

		} else if (rawValue instanceof JSONObject) {
			JSONObject rawJsonObject = (JSONObject)rawValue;
			JSONObject jsonObject = new JSONObject();
			// Remove null entries
			Iterator<String> keys = rawJsonObject.keys();
			while(keys.hasNext()) {
				String key = keys.next();
				if (!rawJsonObject.isNull(key)) {
					Object value = cleanValue(rawJsonObject.opt(key));
					if (value != null) {
						jsonObject.put(key, value);
					}
				}
			}

			if (jsonObject.length() > 0) {
				return jsonObject;
			} else {
				return null;
			}
		}

		return rawValue;
	}

	public void update(Map<String, String[]> parameters) {
		this.update(parameters, false);
	}
	/**
	 *
	 * @param parameters Map of values to be set in the bean.
	 * @param userUpdate This function is used to load value into new object,
	 * apply overrides and update values. Some value can not be modified by the
	 * user. The method has to know if the update was trigger by the system
	 * (userUpdate = false) or by the user (userUpdate = true).
	 */
	public void update(Map<String, String[]> parameters, boolean userUpdate) {
		Map<String, Object> cleanParameters = new HashMap<String, Object>();
		for (Map.Entry<String, String[]> parameterEntry : parameters.entrySet()) {
			String[] value = parameterEntry.getValue();
			if (value.length > 1) {
				cleanParameters.put(parameterEntry.getKey(), value);
			} else {
				cleanParameters.put(parameterEntry.getKey(), value[0]);
			}
		}
		this.update(new JSONObject(cleanParameters), userUpdate);
	}

	public void update(JSONObject jsonObj) {
		update(jsonObj, false);
	}
	/**
	 *
	 * @param jsonObj Map of values to be set in the bean.
	 * @param userUpdate This function is used to load value into new object,
	 * apply overrides and update values. Some value can not be modified by the
	 * user. The method has to know if the update was trigger by the system
	 * (userUpdate = false) or by the user (userUpdate = true).
	 */
	public void update(JSONObject jsonObj, boolean userUpdate) {
		// Get all fields; public, protected and private
		//Field fields[] = this.getClass().getDeclaredFields();
		List<Field> fields = Utils.getAllFields(this.getClass());
		for (Field field : fields) {
			ConfigField annotation = field.getAnnotation(ConfigField.class);
			if (annotation != null) {
				if (!userUpdate || !this.isReadOnly(annotation)) {
					Method setter = _getFieldSetter(field, annotation);
					if (setter != null) {
						String configName = annotation.name();
						if (configName == null || configName.length() <= 0) {
							configName = field.getName();
						}

						try {
							if (jsonObj.has(configName)) {
								Class fieldClass = field.getType();
								Type[] collectionTypes = null;
								if (Collection.class.isAssignableFrom(fieldClass)) {
									try {
										collectionTypes = ((ParameterizedType)setter.getGenericParameterTypes()[0]).getActualTypeArguments();
									} catch(Exception e) {
										LOGGER.log(Level.WARNING, "Can not find the types for the values in the collection ["+configName+"]", e);
									}
								}

								Object value = getValue(this.configManager, jsonObj, configName, fieldClass, collectionTypes);
								setter.invoke(this, value);
							}
						} catch (Exception ex) {
							LOGGER.log(Level.SEVERE, "Can not call the method ["+setter.getName()+"] for the field ["+field.getName()+"]", ex);
						}
					}
				}
			}
		}
	}

	private boolean isReadOnly(ConfigField annotation) {
		return this.configManager != null
				&& this.configManager.isDemoMode()
				&& annotation.demoReadOnly();
	}

	// Support (for now) int, float, double, boolean, JSONObject, JSONArray, String, sub classes of AbstractConfig, Map and other Collection.
	// Recursive for collections
	private static Object getValue(ConfigManager configManager, JSONObject jsonObj, String configName, Class fieldClass, Type[] collectionTypes) throws InstantiationException, IllegalAccessException, JSONException {
		if (jsonObj.isNull(configName)) {
			if(int.class.equals(fieldClass) || float.class.equals(fieldClass) || double.class.equals(fieldClass)) {
				return DEFAULT_NUMBER;
			}
			if (boolean.class.equals(fieldClass)) {
				return DEFAULT_BOOLEAN;
			}

			return null;
		}

		Object value = null;
		// This is equivalent to "if (fieldType instanceof AbstractConfig)",
		// if fieldType was an instance of the class that it represent.
		if (AbstractConfig.class.isAssignableFrom(fieldClass)) {
			JSONObject jsonValue = getJSONObject(jsonObj, configName);
			if (jsonValue != null && jsonValue.length() > 0) {
				try {
					AbstractConfig configValue =
							(AbstractConfig)fieldClass.getConstructor(ConfigManager.class).newInstance(configManager);

					configValue.update(jsonValue);
					// setJSONObjectKey: Only called here, when the JSON parameter is a JSONObject
					configValue.setJSONObjectKey(configName);
					value = configValue;
				} catch (NoSuchMethodException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " has no constructor using a ConfigManager as parameter.", ex);
				} catch (InvocationTargetException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " constructor generated an exception when called.", ex);
				} catch (SecurityException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " constructor can not be called due to security restrictions.", ex);
				}
			}
		} else if (Map.class.isAssignableFrom(fieldClass)) {
			// The field is a map, the value can only be a JSONObject, or a String that represent a JSONObject.
			JSONObject jsonValue = getJSONObject(jsonObj, configName);
			Class collectionClass = null;
			if (collectionTypes != null && collectionTypes.length >= 2 && collectionTypes[1] != null) {
				collectionClass = (Class)collectionTypes[1];
			}
			if (collectionClass != null && jsonValue != null && jsonValue.length() > 0) {
				Map<String, Object> configValue = new HashMap<String, Object>();
				Iterator<String> keys = jsonValue.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					Object val = getValue(
							configManager,
							jsonValue,
							key,
							collectionClass,
							null);
					if (val != null) {
						configValue.put(key, val);
					}
				}
				if (!configValue.isEmpty()) {
					value = configValue;
				}
			}
		} else if (Collection.class.isAssignableFrom(fieldClass)) {
			// The field is a collection, the value can be a JSONObject or a JSONArray, or a String that represent a JSONObject/JSONArray.
			Class collectionClass = null;
			if (collectionTypes != null && collectionTypes.length >= 1 && collectionTypes[0] != null) {
				collectionClass = (Class)collectionTypes[0];
			}
			if (collectionClass != null) {
				JSONObject jsonObjValue = getJSONObject(jsonObj, configName);
				Collection<Object> configValue = null;
				if (List.class.isAssignableFrom(fieldClass)) {
					configValue = new ArrayList<Object>();
				} else if (Set.class.isAssignableFrom(fieldClass)) {
					configValue = new HashSet<Object>();
				} else {
					throw new IllegalAccessException("Unsupported collection type ["+fieldClass.getName()+"]");
				}
				if (configValue != null) {
					if (jsonObjValue == null) {
						JSONArray jsonArrValue = getJSONArray(jsonObj, configName);
						if (jsonArrValue != null) {
							for (int i=0; i<jsonArrValue.length(); i++) {
								Object val = getValue(
										configManager,
										jsonArrValue,
										i,
										collectionClass,
										null);
								if (val != null) {
									configValue.add(val);
								}
							}
						}
					} else if(jsonObjValue.length() > 0) {
						Iterator<String> keys = jsonObjValue.keys();
						while(keys.hasNext()) {
							Object val = getValue(
									configManager,
									jsonObjValue,
									keys.next(),
									collectionClass,
									null);
							if (val != null) {
								configValue.add(val);
							}
						}
					}
					if (!configValue.isEmpty()) {
						value = configValue;
					}
				}
			}
		} else if(fieldClass.isArray()) {
			Class arrayType = fieldClass.getComponentType();
			if (arrayType != null) {
				// The field is an Array, the value can be a JSONObject or a JSONArray, or a String that represent a JSONObject/JSONArray.
				JSONObject jsonObjValue = getJSONObject(jsonObj, configName);
				ArrayList<Object> configValue = new ArrayList<Object>();
				if (jsonObjValue == null) {
					JSONArray jsonArrValue = getJSONArray(jsonObj, configName);
					if (jsonArrValue != null) {
						for (int i=0; i<jsonArrValue.length(); i++) {
							Object val = getValue(
									configManager,
									jsonArrValue,
									i,
									(Class)arrayType,
									null);
							if (val != null) {
								configValue.add(val);
							}
						}
					}
				} else if(jsonObjValue.length() > 0) {
					Iterator<String> keys = jsonObjValue.keys();
					while(keys.hasNext()) {
						Object val = getValue(
								configManager,
								jsonObjValue,
								keys.next(),
								(Class)arrayType,
								null);
						if (val != null) {
							configValue.add(val);
						}
					}
				}
				if (configValue != null && !configValue.isEmpty()) {
					value = Array.newInstance(arrayType, configValue.size());
					int i=0;
					for (Object configVal : configValue) {
						Array.set(value, i++, configVal);
					}
				}
			}
		} else if(Integer.class.equals(fieldClass) || int.class.equals(fieldClass)) {
			value = jsonObj.optInt(configName, DEFAULT_NUMBER);
		} else if (Float.class.equals(fieldClass) || float.class.equals(fieldClass)) {
			value = new Float(jsonObj.optDouble(configName, DEFAULT_NUMBER));
		} else if (Double.class.equals(fieldClass) || double.class.equals(fieldClass)) {
			value = jsonObj.optDouble(configName, DEFAULT_NUMBER);
		} else if (Boolean.class.equals(fieldClass) || boolean.class.equals(fieldClass)) {
			value = jsonObj.optBoolean(configName, DEFAULT_BOOLEAN);
		} else if (JSONObject.class.equals(fieldClass)) {
			value = getJSONObject(jsonObj, configName);
		} else if (JSONArray.class.equals(fieldClass)) {
			value = getJSONArray(jsonObj, configName);
		} else {
			// String
			String strValue = jsonObj.optString(configName, null);
			if (Utils.isNotBlank(strValue)) {
				value = strValue;
			}
		}

		return value;
	}

	// Duplicate of the previous function because JSONObject and JSONArray do not extends a common abstract class
	private static Object getValue(ConfigManager configManager, JSONArray jsonArr, int index, Class fieldClass, Type[] collectionTypes) throws JSONException, InstantiationException, IllegalAccessException {
		if (jsonArr.isNull(index)) {
			if(int.class.equals(fieldClass) || float.class.equals(fieldClass) || double.class.equals(fieldClass)) {
				return DEFAULT_NUMBER;
			}
			if (boolean.class.equals(fieldClass)) {
				return DEFAULT_BOOLEAN;
			}

			return null;
		}

		Object value = null;
		// This is equivalent to "if (fieldType instanceof AbstractConfig)",
		// if fieldType was an instance of the class that it represent.
		if (AbstractConfig.class.isAssignableFrom(fieldClass)) {
			JSONObject jsonValue = getJSONObject(jsonArr, index);
			if (jsonValue != null && jsonValue.length() > 0) {

				try {
					AbstractConfig configValue =
							(AbstractConfig)fieldClass.getConstructor(ConfigManager.class).newInstance(configManager);

					configValue.update(jsonValue);
					value = configValue;
				} catch (NoSuchMethodException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " has no constructor using a ConfigManager as parameter.", ex);
				} catch (InvocationTargetException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " constructor generated an exception when called.", ex);
				} catch (SecurityException ex) {
					LOGGER.log(Level.SEVERE, fieldClass.getName() + " constructor can not be called due to security restrictions.", ex);
				}
			}
		} else if (Map.class.isAssignableFrom(fieldClass)) {
			// The field is a map, the value can only be a JSONObject, or a String that represent a JSONObject.
			JSONObject jsonValue = getJSONObject(jsonArr, index);
			Class collectionClass = null;
			if (collectionTypes != null && collectionTypes.length >= 2 && collectionTypes[1] != null) {
				collectionClass = (Class)collectionTypes[1];
			}
			if (collectionClass != null && jsonValue != null && jsonValue.length() > 0) {
				Map<String, Object> configValue = new HashMap<String, Object>();
				Iterator<String> keys = jsonValue.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					Object val = getValue(
							configManager,
							jsonValue,
							key,
							collectionClass,
							null);

					if (val != null) {
						configValue.put(key, val);
					}
				}
				if (!configValue.isEmpty()) {
					value = configValue;
				}
			}
		} else if (Collection.class.isAssignableFrom(fieldClass)) {
			// The field is a collection, the value can be a JSONObject or a JSONArray, or a String that represent a JSONObject/JSONArray.
			Class collectionClass = null;
			if (collectionTypes != null && collectionTypes.length >= 1 && collectionTypes[0] != null) {
				collectionClass = (Class)collectionTypes[0];
			}
			if (collectionClass != null) {
				JSONObject jsonObjValue = getJSONObject(jsonArr, index);
				Collection<Object> configValue = null;
				if (List.class.isAssignableFrom(fieldClass)) {
					configValue = new ArrayList<Object>();
				} else if (Set.class.isAssignableFrom(fieldClass)) {
					configValue = new HashSet<Object>();
				} else {
					throw new IllegalAccessException("Unsupported collection type ["+fieldClass.getName()+"]");
				}
				if (configValue != null) {
					if (jsonObjValue == null) {
						JSONArray jsonArrValue = getJSONArray(jsonArr, index);
						if (jsonArrValue != null) {
							for (int i=0; i<jsonArrValue.length(); i++) {
								Object val = getValue(
										configManager,
										jsonArrValue,
										i,
										collectionClass,
										null);
								if (val != null) {
									configValue.add(val);
								}
							}
						}
					} else if(jsonObjValue.length() > 0) {
						Iterator<String> keys = jsonObjValue.keys();
						while(keys.hasNext()) {
							Object val = getValue(
									configManager,
									jsonObjValue,
									keys.next(),
									collectionClass,
									null);
							if (val != null) {
								configValue.add(val);
							}
						}
					}
					if (!configValue.isEmpty()) {
						value = configValue;
					}
				}
			}
		} else if(fieldClass.isArray()) {
			Class arrayType = fieldClass.getComponentType();
			if (arrayType != null) {
				// The field is an Array, the value can be a JSONObject or a JSONArray, or a String that represent a JSONObject/JSONArray.
				JSONObject jsonObjValue = getJSONObject(jsonArr, index);
				ArrayList<Object> configValue = new ArrayList<Object>();
				if (jsonObjValue == null) {
					JSONArray jsonArrValue = getJSONArray(jsonArr, index);
					if (jsonArrValue != null) {
						for (int i=0; i<jsonArrValue.length(); i++) {
							Object val = getValue(
									configManager,
									jsonArrValue,
									i,
									(Class)arrayType,
									null);
							if (val != null) {
								configValue.add(val);
							}
						}
					}
				} else if(jsonObjValue.length() > 0) {
					Iterator<String> keys = jsonObjValue.keys();
					while(keys.hasNext()) {
						Object val = getValue(
								configManager,
								jsonObjValue,
								keys.next(),
								(Class)arrayType,
								null);
						if (val != null) {
							configValue.add(val);
						}
					}
				}
				if (configValue != null && !configValue.isEmpty()) {
					value = Array.newInstance(arrayType, configValue.size());
					int i=0;
					for (Object configVal : configValue) {
						Array.set(value, i++, configVal);
					}
				}
			}
		} else if(Integer.class.equals(fieldClass) || int.class.equals(fieldClass)) {
			value = jsonArr.optInt(index, DEFAULT_NUMBER);
		} else if (Float.class.equals(fieldClass) || float.class.equals(fieldClass)) {
			value = new Float(jsonArr.optDouble(index, DEFAULT_NUMBER));
		} else if (Double.class.equals(fieldClass) || double.class.equals(fieldClass)) {
			value = jsonArr.optDouble(index, DEFAULT_NUMBER);
		} else if (Boolean.class.equals(fieldClass) || boolean.class.equals(fieldClass)) {
			value = jsonArr.optBoolean(index, DEFAULT_BOOLEAN);
		} else if (JSONObject.class.equals(fieldClass)) {
			value = getJSONObject(jsonArr, index);
		} else if (JSONArray.class.equals(fieldClass)) {
			value = getJSONArray(jsonArr, index);
		} else {
			// String
			String strValue = jsonArr.optString(index, null);
			if (Utils.isNotBlank(strValue)) {
				value = strValue;
			}
		}

		return value;
	}


	private static JSONObject getJSONObject(JSONObject jsonObj, String configName) {
		JSONObject jsonValue = null;
		try {
			jsonValue = jsonObj.optJSONObject(configName);
		} catch(Exception e) {}
		// The value will be a String if it come from a raw form
		if (jsonValue == null) {
			try {
				jsonValue = new JSONObject(jsonObj.optString(configName, null));
			} catch(Exception e) {}
		}
		return jsonValue;
	}
	private static JSONObject getJSONObject(JSONArray jsonArr, int index) {
		JSONObject jsonValue = null;
		try {
			jsonValue = jsonArr.optJSONObject(index);
		} catch(Exception e) {}
		// The value will be a String if it come from a raw form
		if (jsonValue == null) {
			try {
				jsonValue = new JSONObject(jsonArr.optString(index, null));
			} catch(Exception e) {}
		}
		return jsonValue;
	}

	private static JSONArray getJSONArray(JSONObject jsonObj, String configName) {
		JSONArray value = null;
		try {
			value = jsonObj.optJSONArray(configName);
		} catch(Exception e) {}
		// The value will be a String if it come from a raw form
		if (value == null) {
			String valueStr = jsonObj.optString(configName, null);
			if (valueStr != null && !valueStr.isEmpty()) {
				try {
					value = new JSONArray(valueStr);
				} catch(Exception e) {}
				if (value == null) {
					// The value may contains only one value, returned as a single String without braquets
					JSONArray jsonArray = new JSONArray();
					jsonArray.put(valueStr);
					value = jsonArray;
				}
			}
		}
		return value;
	}
	private static JSONArray getJSONArray(JSONArray jsonArr, int index) {
		JSONArray value = null;
		try {
			value = jsonArr.optJSONArray(index);
		} catch(Exception e) {}
		// The value will be a String if it come from a raw form
		if (value == null) {
			String valueStr = jsonArr.optString(index, null);
			if (valueStr != null && !valueStr.isEmpty()) {
				try {
					value = new JSONArray(valueStr);
				} catch(Exception e) {}
				if (value == null) {
					// The value may contains only one value, returned as a single String without braquets
					JSONArray jsonArray = new JSONArray();
					jsonArray.put(valueStr);
					value = jsonArray;
				}
			}
		}
		return value;
	}

	// Helper
	protected String _getParameter(Map<String, String[]> parameters, String name) {
		return parameters.get(name) == null ? null : parameters.get(name)[0];
	}

	private Method _getFieldGetter(Field field, ConfigField annotation) {
		String getter = annotation.getter();
		if (getter == null) {
			return null;
		}
		if (getter.length() <= 0) {
			Class fieldType = field.getType();
			if (fieldType != null) {
				String capitalizedFieldName = Utils.capitalizeFirst(field.getName());
				if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
					getter = "is" + capitalizedFieldName;
				} else {
					getter = "get" + capitalizedFieldName;
				}
			}
		}
		Method getterMethod = null;
		if (Utils.isNotBlank(getter)) {
			try {
				//getterMethod = this.getClass().getDeclaredMethod(getter);
				getterMethod = Utils.getMethod(field.getDeclaringClass(), getter);
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Can not find the method ["+getter+"] for class ["+field.getDeclaringClass()+"]", ex);
			}
		}
		return getterMethod;
	}

	private Method _getFieldSetter(Field field, ConfigField annotation) {
		return this._getFieldSetter(field, field.getType(), annotation);
	}
	private Method _getFieldSetter(Field field, Class type, ConfigField annotation) {
		String setter = annotation.setter();
		if (setter == null) {
			return null;
		}
		if (setter.length() <= 0) {
			String capitalizedFieldName = Utils.capitalizeFirst(field.getName());
			setter = "set" + capitalizedFieldName;
		}
		Method setterMethod = null;
		if (setter != null && setter.length() > 0) {
			try {
				//setterMethod = this.getClass().getDeclaredMethod(setter, type);
				setterMethod = Utils.getMethod(field.getDeclaringClass(), setter, type);
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Can not find the method ["+setter+"] for class ["+field.getDeclaringClass()+"]", ex);
			}
		}
		return setterMethod;
	}

	/**
	 * Abstract cloning method, copy the value of each fields from this to
	 * a new instance.
	 * This method do not clone the attributes itself. This may cause
	 * problems with collections and complex objects like JSONObject,
	 * JSONArray, etc.
	 * @return clone
	 */
	@Override
	public Object clone() {
		Object clone = null;
		try {
			// Get the basic constructor that required only a ConfigManager and instanciate it.
			// Since the object extend AbstractConfig, such a constructor IS present.
			clone = this.getClass().getConstructor(ConfigManager.class).newInstance(this.configManager);

			// Get all fields; public, protected and private
			//Field fields[] = this.getClass().getDeclaredFields();
			List<Field> fields = Utils.getAllFields(this.getClass());
			for (Field field : fields) {
				// Don't change the value of static or final fields
				if (!Utils.isStatic(field) && !Utils.isFinal(field)) {
					// This wont work if java run with a Policy Manager
					// that blocks ACCESS_PERMISSION.
					field.setAccessible(true);
					field.set(clone, field.get(this));
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can not clone Object of class: " + this.getClass().getName(), e);
		}

		return clone;
	}

	public void applyOverrides(AbstractConfig overrides) {
		if (overrides == null) {
			return;
		}
		// Get all fields; public, protected and private
		//Field fields[] = this.getClass().getDeclaredFields();
		List<Field> fields = Utils.getAllFields(overrides.getClass());
		for (Field field : fields) {
			ConfigField annotation = field.getAnnotation(ConfigField.class);
			if (annotation != null) {
				Method getter = _getFieldGetter(field, annotation);
				Method setter = _getFieldSetter(field, annotation);
				if (getter != null && setter != null) {

					try {
						Object rawValue = getter.invoke(overrides);
						if (rawValue != null) {

							if(rawValue instanceof Collection) {
								// Merge Collection values
								Collection newColl = (Collection)rawValue;
								Collection curColl = (Collection)getter.invoke(this);
								if (curColl == null) {
									curColl = newColl.getClass().newInstance();
									setter.invoke(this, curColl);
								}

								ParameterizedType collElType = (ParameterizedType)field.getGenericType();
								Class collElClass = (Class)collElType.getActualTypeArguments()[0];
								// collElClass instanceof AbstractConfig
								if (AbstractConfig.class.isAssignableFrom(collElClass)) {
									// Apply needed overrides
									for (Object el : curColl) {
										if (el != null) {
											AbstractConfig configEl = (AbstractConfig)el;
											String currentKey = configEl.getJSONObjectKey();
											if (currentKey != null) {
												for (Object newEl : newColl) {
													if (newEl != null) {
														AbstractConfig newConfigEl = (AbstractConfig)newEl;
														if (currentKey.equals(newConfigEl.getJSONObjectKey())) {
															// Refer to the same object, need override
															configEl.applyOverrides(newConfigEl);
														}
													}
												}
											}
										}
									}
									// Add new elements
									for (Object newEl : newColl) {
										if (newEl != null) {
											AbstractConfig newConfigEl = (AbstractConfig)newEl;
											String newKey = newConfigEl.getJSONObjectKey();
											if (newKey != null) {
												boolean newElFound = false;
												for (Object el : curColl) {
													if (el != null) {
														AbstractConfig configEl = (AbstractConfig)el;
														if (newKey.equals(configEl.getJSONObjectKey())) {
															newElFound = true;
															break;
														}
													}
												}
												if (!newElFound) {
													// The new object can not be found, need to be added
													curColl.add(newEl);
												}
											}
										}
									}
								} else {
									// For Collection of something else than AbstractConfig,
									// Only add elements that are not already present in the collection
									// I.E. equals return false
									for (Object newEl : newColl) {
										if (newEl != null) {
											boolean newElFound = false;
											for (Object el : curColl) {
												if (newEl.equals(el)) {
													newElFound = true;
													break;
												}
											}
											if (!newElFound) {
												curColl.add(newEl);
											}
										}
									}
								}

							} else if (rawValue.getClass().isArray()) {
								// Merge Array values
								Object newArray = rawValue;
								int newLength = Array.getLength(newArray);
								if (newLength > 0) {
									Object curArray = getter.invoke(this);
									int curLength = 0;
									if (curArray != null) {
										curLength = Array.getLength(curArray);
									}
									// Create an abstract object that represent an array of the required type with the required dimensions
									Object mergedArray = Array.newInstance(
											newArray.getClass().getComponentType(),
											curLength + newLength);

									// Fill the values into the new abstract array
									for (int i=0; i<curLength; i++) {
										Array.set(mergedArray, i, Array.get(curArray, i));
									}
									for (int i=0; i<newLength; i++) {
										Array.set(mergedArray, curLength+i, Array.get(newArray, i));
									}

									setter.invoke(this, mergedArray);
								}
							} else {
								setter.invoke(this, rawValue);
							}
						}
					} catch (Exception ex) {
						LOGGER.log(Level.SEVERE, "Can not call the method ["+getter.getName()+"] or ["+setter.getName()+"] for the field ["+field.getName()+"]", ex);
					}
				}
			}
		}
	}
}
