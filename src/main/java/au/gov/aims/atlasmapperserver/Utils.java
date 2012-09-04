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

package au.gov.aims.atlasmapperserver;

import com.vividsolutions.jts.geom.Coordinate;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.geometry.jts.JTS;
import org.geotools.measure.Latitude;
import org.geotools.measure.Longitude;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author glafond
 */
public class Utils {
	private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
	private static final int INDENT = 4;
	// Copied from org.geotools.referencing.operation.projection.MapProjection
	private static final double ANGLE_TOLERANCE = 1E-4;

	private static final Map<String, JSONObject> SUPPORTED_PROJECTIONS = new HashMap<String, JSONObject>();
	static {
		try {
			// Allow values for 'units': 'degrees' (or 'dd'), 'm', 'ft', 'km', 'mi', 'inches'
			SUPPORTED_PROJECTIONS.put("EPSG:4326", new JSONObject()
					.put("projectionName", "Standard WMS (EPSG:4326)")
					.put("maxExtent", new JSONArray("[-180.0, -90.0, 180.0, 90.0]"))
					.put("units", "degrees")
					.put("numZoomLevels", 16)
					.put("maxResolution", 0.703125));

			SUPPORTED_PROJECTIONS.put("EPSG:900913", new JSONObject()
					.put("projectionName", "Google maps (EPSG:900913)")
					.put("maxExtent", new JSONArray("[-20037508.342787, -20037508.342787, 20037508.342787, 20037508.342787]"))
					.put("units", "m")
					.put("numZoomLevels", 20)
					.put("maxResolution", 156543.033928)
					.put("resolutions", new JSONArray("[156543.033928, 78271.5169639999, 39135.7584820001, 19567.8792409999, 9783.93962049996, 4891.96981024998, 2445.98490512499, 1222.99245256249, 611.49622628138, 305.748113140558, 152.874056570411, 76.4370282850732, 38.2185141425366, 19.1092570712683, 9.55462853563415, 4.77731426794937, 2.38865713397468, 1.19432856685505, 0.597164283559817, 0.298582141647617]")));

			/*
			// OpenLayers is not able to do the re-projection of EPSG:3785.
			SUPPORTED_PROJECTIONS.put("EPSG:3785", new JSONObject()
					.put("projectionName", "Mercator (EPSG:3785)")
					.put("maxExtent", new JSONArray("[-20037508.342787, -20037508.342787, 20037508.342787, 20037508.342787]"))
					.put("units", "m")
					.put("numZoomLevels", 20)
					.put("maxResolution", 156543.033928)
					.put("resolutions", new JSONArray("[156543.033928, 78271.5169639999, 39135.7584820001, 19567.8792409999, 9783.93962049996, 4891.96981024998, 2445.98490512499, 1222.99245256249, 611.49622628138, 305.748113140558, 152.874056570411, 76.4370282850732, 38.2185141425366, 19.1092570712683, 9.55462853563415, 4.77731426794937, 2.38865713397468, 1.19432856685505, 0.597164283559817, 0.298582141647617]")));
			*/

			// TODO Add more projections
		} catch (JSONException ex) {
			LOGGER.log(Level.SEVERE, "Can not create the JSON map of supported projections.", ex);
		}
	}

	public static boolean isBlank(String str) {
		return str==null || str.trim().isEmpty();
	}
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	public static String capitalizeFirst(String rawStr) {
		String str = rawStr.trim();
		char[] chars = str.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);

		return String.valueOf(chars);
	}

	/**
	 * Make String safe to use in as a JavaScript String by
	 * replacing [\] by [\\], ['] by [\'] and ["] by [\"].
	 * @param jsObj
	 * @return
	 */
	public static String safeJsStr(Object jsObj) {
		if (jsObj == null) { return null; }
		return safeJsStr(jsObj.toString());
	}
	public static String safeJsStr(String jsStr) {
		if (jsStr == null) { return null; }
		// NOTE: replace is equivalent to replaceAll, using String instead of regexp.
		return jsStr.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'").replace("\"", "\\\"");
	}

	public static String safeHTMLStr(String jsStr) {
		if (jsStr == null) { return null; }
		// NOTE: replace is equivalent to replaceAll, using String instead of regexp.
		return jsStr.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static String jsonToStr(JSONObject json) throws JSONException {
		if (json == null) { return null; }
		return json.toString(INDENT);
	}
	public static String jsonToStr(JSONArray json) throws JSONException {
		if (json == null) { return null; }
		return json.toString(INDENT);
	}

	public static String getUrlParameter(String urlStr, String parameterName, boolean ignoreCase) throws UnsupportedEncodingException {
		int queryStrStart = urlStr.indexOf('?');

		if (queryStrStart > 0) {
			// Do not include the "?"
			String queryStr = urlStr.substring(queryStrStart+1);
			String searchStr = URLEncoder.encode(parameterName, "UTF-8") + "=";

			if (!queryStr.isEmpty()) {
				String[] params = queryStr.split("&");
				for (int i=0; i<params.length; i++) {
					String param = params[i];

					if ((ignoreCase && param.toUpperCase().startsWith(searchStr.toUpperCase())) ||
							(!ignoreCase && param.startsWith(searchStr))) {
						return URLDecoder.decode(param.substring(searchStr.length()), "UTF-8");
					}
				}
			}
		}

		return null;
	}

	public static String setUrlParameter(String urlStr, String parameterName, String parameterValue) throws UnsupportedEncodingException {
		int queryStrStart = urlStr.indexOf('?');

		if (queryStrStart > 0) {
			// Do not include the "?"
			String baseStr = urlStr.substring(0, queryStrStart);
			String queryStr = urlStr.substring(queryStrStart+1);
			String searchStr = URLEncoder.encode(parameterName, "UTF-8") + "=";

			StringBuilder newQuerySb = new StringBuilder(baseStr);
			if (!queryStr.isEmpty()) {
				String[] params = queryStr.split("&");
				boolean found = false;
				for (int i=0; i<params.length; i++) {
					String param = params[i];

					newQuerySb.append(i > 0 ? "&" : "?");
					if (param.startsWith(searchStr)) {
						newQuerySb.append(URLEncoder.encode(parameterName, "UTF-8"));
						newQuerySb.append("=");
						newQuerySb.append(URLEncoder.encode(parameterValue, "UTF-8"));
						found = true;
					} else {
						newQuerySb.append(param);
					}
				}

				if (found) {
					return newQuerySb.toString();
				}
			}
		}

		return Utils.addUrlParameter(urlStr, parameterName, parameterValue);
	}

	// Same result as the above method, but significantly slower.
	/*
	public static String setUrlParameter(String urlStr, String parameterName, String parameterValue) throws UnsupportedEncodingException {
		Matcher matcher = Pattern.compile("([?&])" + Pattern.quote(URLEncoder.encode(parameterName, "UTF-8")) + "=[^&]*").matcher(urlStr);
		if (matcher.find()) {
			return matcher.replaceFirst(matcher.group(1) + URLEncoder.encode(parameterName, "UTF-8") + "=" + URLEncoder.encode(parameterValue, "UTF-8"));
		}
		return Utils.addUrlParameter(urlStr, parameterName, parameterValue);
	}
	*/

	public static String addUrlParameter(String urlStr, String parameterName, String parameterValue) throws UnsupportedEncodingException {
		String delimiter = urlStr.contains("?") ? "&" : "?";
		return urlStr + delimiter + URLEncoder.encode(parameterName, "UTF-8") + "=" + URLEncoder.encode(parameterValue, "UTF-8");
	}

	public static String removeUrlParameter(String urlStr, String parameterName) throws UnsupportedEncodingException {
		int queryStrStart = urlStr.indexOf('?');

		if (queryStrStart > 0) {
			// Do not include the "?"
			String baseStr = urlStr.substring(0, queryStrStart);
			String queryStr = urlStr.substring(queryStrStart+1);
			String searchStr = URLEncoder.encode(parameterName, "UTF-8") + "=";

			StringBuilder newQuerySb = new StringBuilder(baseStr);
			if (!queryStr.isEmpty()) {
				String[] params = queryStr.split("&");
				boolean found = false;
				boolean first = true;
				for (String param : params) {
					if (param.startsWith(searchStr)) {
						found = true;
					} else {
						newQuerySb.append(first ? "?" : "&");
						newQuerySb.append(param);
						first = false;
					}
				}

				if (found) {
					return newQuerySb.toString();
				}
			}
		}

		return urlStr;
	}

	/**
	 * Return a Base64 encoding of the MD5 of the parameter.
	 * @param pass
	 * @return
	 */
	public static String encrypt(String pass) {
		try {
			byte[] encryptPass = md5sum(pass);
			return toHex(encryptPass);
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.log(Level.SEVERE, "Can not encrypt the password.", ex);
		}
		// Unlikely to append
		return pass;
	}

	public static byte[] md5sum(String data) throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("MD5").digest(data.getBytes());
	}

	public static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte abyte : bytes) {
			sb.append(String.format("%02X", abyte));
		}
		return sb.toString();
	}

	public static void recursiveFileCopy(File src, File dest, boolean overwrite) throws IOException {
		if (src == null || dest == null) {
			return;
		}
		if (!src.exists()) {
			throw new IOException("The source file ["+src.getAbsolutePath()+"] do not exists.");
		}

		if (src.isFile()) {
			if (overwrite || !dest.exists()) {
				binaryfileCopy(src, dest);
			}

		} else if (src.isDirectory()) {
			if (dest.exists()) {
				if (!dest.isDirectory()) {
					throw new IOException("The destination file ["+dest.getAbsolutePath()+"] already exists and it is not a directory.");
				}
			} else {
				dest.mkdirs();
			}
			for (File subFile : src.listFiles()) {
				recursiveFileCopy(subFile, new File(dest, subFile.getName()), overwrite);
			}
		}
	}

	private static void binaryfileCopy(File src, File dest) throws IOException {
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(dest);

			byte[] buf = new byte[32 * 1024];  // 32K buffer
			int bytesRead;
			while ((bytesRead = in.read(buf)) != -1) {
				out.write(buf, 0, bytesRead);
			}
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the file", e);
				}
			}
			if (out != null) {
				try { out.flush(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while flushing the file", e);
				}
				try { out.close(); } catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error occur while closing the file", e);
				}
			}
		}
	}

	public static boolean recursiveFileDelete(File file) {
		// If the parameter is null, the file do not exists: it's already absent from the file system, which is considered as a success.
		if (file == null) { return true; }

		boolean success = true;
		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				success = Utils.recursiveFileDelete(subFile) && success;
			}
		}
		return file.delete() && success;
	}

	/**
	 * Return true if the file is writable, or can be created.
	 * It recursively go back to the file parents until one folder actually exists, and return true if that
	 * folder is writable, therefor the file could be create if all missing parent folder were created.
	 * @param file
	 * @return
	 */
	public static boolean recursiveIsWritable(File file) {
		if (file == null) { return false; }
		if (file.exists()) {
			return file.canWrite();
		} else {
			return recursiveIsWritable(file.getParentFile());
		}
	}


	/**
	 * freemarker
	 * StringTemplate
	 * http://www.stringtemplate.org/
	 * http://www.cs.usfca.edu/~parrt/course/601/lectures/stringtemplate.html
	 */
	public static Configuration getTemplatesConfig(File templatesFolder) throws IOException {
		Configuration config = new Configuration();
		config.setDirectoryForTemplateLoading(templatesFolder);
		return config;
	}

	public static void processTemplate(Configuration templatesConfig, String templateName, Map<String, Object> values, File destFolder) throws IOException, TemplateException {
		String templateFilename = templateName + ".ftl";
		File outputFile = new File(destFolder, templateName);
		OutputStreamWriter output = null;

		try {
			Template tpl = templatesConfig.getTemplate(templateFilename);
			output = new OutputStreamWriter(new FileOutputStream(outputFile));
			tpl.process(values, output);
		} finally {
			if (output != null) {
				try {
					output.flush();
				} finally {
					output.close();
				}
			}
		}
	}

	// Reflexion helpers

	/**
	 * Return all fields (public, protected and private), including the
	 * fields of all the parent classes (recursively).
	 * @param clazz
	 * @return
	 */
	public static List<Field> getAllFields(Class clazz) {
		List<Field> fields = new ArrayList<Field>();
		if (clazz == null) {
			return fields;
		}

		fields.addAll(getAllFields(clazz.getSuperclass()));
		for (Field field : Arrays.asList(clazz.getDeclaredFields())) {
			fields.add(field);
		}

		return fields;
	}

	public static boolean isStatic(Field field) {
		return (field.getModifiers() & Modifier.STATIC) > 0;
	}

	public static boolean isFinal(Field field) {
		return (field.getModifiers() & Modifier.FINAL) > 0;
	}

	/**
	 * Search the method in the class and it's parent classes.
	 * @param clazz
	 * @return
	 */
	public static Method getMethod(Class clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		return _getMethod(null, clazz, methodName, parameterTypes);
	}
	// Keep the first exception and re-throw it if the class can not be found.
	private static Method _getMethod(NoSuchMethodException noSuchMethodException, Class clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		if (clazz == null) {
			// The method is realy not found...
			throw noSuchMethodException;
		}

		Method foundMethod = null;
		try {
			foundMethod = clazz.getDeclaredMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException ex) {
			// Don't panic, the method may be in the parent class
			if (noSuchMethodException == null) {
				// Keep the first exception, it's the more explicit.
				noSuchMethodException = ex;
			}
		}

		if (foundMethod == null) {
			foundMethod = _getMethod(noSuchMethodException, clazz.getSuperclass(), methodName, parameterTypes);
		}

		return foundMethod;
	}

	/**
	 * Search the method in the class and it's parent classes.
	 * @param clazz
	 * @return
	 */
	public static boolean hasMethod(Class clazz, Method method) {
		return _hasMethod(clazz, method);
	}
	private static boolean _hasMethod(Class clazz, Method method) {
		if (clazz == null) {
			// The method is really not found...
			return false;
		}

		for (Method declaredMethod : clazz.getDeclaredMethods()) {
			if (method.equals(declaredMethod)) {
				return true;
			}
		}

		// Don't panic, the method may be in the parent class
		return _hasMethod(clazz.getSuperclass(), method);
	}

	// Return a String representing the current millisecond
	// Usefull to force the browser to refresh its cache, when used as a URL parameter
	public static long getCurrentTimestamp() {
		return new Date().getTime();
	}

	/**
	 * @return Map of "projection code", "display name".
	 */
	public static JSONArray getSupportedProjections() throws JSONException {
		JSONArray projections = new JSONArray();
		for (Map.Entry<String, JSONObject> projectionEntry : SUPPORTED_PROJECTIONS.entrySet()) {
			JSONObject projection = new JSONObject();
			projection.put("name", projectionEntry.getKey());
			projection.put("title", projectionEntry.getValue().optString("projectionName", "UNKNOWN"));

			projections.put(projection);
		}
		return projections;
	}

	public static double[] reprojectWKTCoordinatesToDegrees(double[] coordinates, String sourceWKT) throws FactoryException, TransformException {
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");

		CoordinateReferenceSystem sourceCRS = CRS.parseWKT(sourceWKT);

		double[] reprojectedCoordinates = reprojectCoordinates(coordinates, sourceCRS, targetCRS);

		// Verify out of range coordinates
		if (!validateDegreesCoordinates(reprojectedCoordinates)) {
			// Out of bound coordinates are usually due to invalid input. No data is better than wrong data.
			throw new TransformException("Coordinates out of bounds");
		}

		return reprojectedCoordinates;
	}

	public static double[] reprojectWKIDCoordinatesToDegrees(double[] coordinates, String sourceWKID) throws FactoryException, TransformException {
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");

		// ESRI CRS 102100 is not supported, but it's the same as the google projection EPSG:900913
		if ("EPSG:102100".equals(sourceWKID)) {
			sourceWKID = "EPSG:900913";
		}

		// ESRI CRS 104199 is not supported, and it seems to look pretty similar to EPSG:4326
		if ("EPSG:104199".equals(sourceWKID)) {
			sourceWKID = "EPSG:4326";
		}

		CoordinateReferenceSystem sourceCRS = CRS.decode(sourceWKID);

		double[] reprojectedCoordinates = reprojectCoordinates(coordinates, sourceCRS, targetCRS);

		// Verify out of range coordinates
		if (!validateDegreesCoordinates(reprojectedCoordinates)) {
			// Out of bound coordinates are usually due to invalid input. No data is better than wrong data.
			throw new TransformException("Coordinates out of bounds");
		}

		return reprojectedCoordinates;
	}

	private static boolean validateDegreesCoordinates(double[] coordinates) {
		// Verify out of range coordinates
		boolean valid = true;
		for (int i=0; i+1 < coordinates.length; i += 2) {

			// GeoTools coordinates are in [Lat, Lon], OpenLayers are in [Lon, Lat]
			double x = coordinates[i+1]; // Longitude
			double y = coordinates[i];   // Latitude

			final boolean xOut, yOut;
			xOut = (Double.isNaN(x) || x < (Longitude.MIN_VALUE - ANGLE_TOLERANCE) || x > (Longitude.MAX_VALUE + ANGLE_TOLERANCE));
			yOut = (Double.isNaN(y) || y < (Latitude.MIN_VALUE - ANGLE_TOLERANCE) || y > (Latitude.MAX_VALUE + ANGLE_TOLERANCE));

			if (xOut || yOut) {
				// Out of bound coordinates are usually due to invalid input. No data is better than wrong data.
				LOGGER.log(Level.WARNING, "Coordinates out of bounds: ["+x+", "+y+"] minimum values: ["+Longitude.MIN_VALUE+", "+Latitude.MIN_VALUE+"] maximum values: ["+Longitude.MAX_VALUE+", "+Latitude.MAX_VALUE+"]");
				valid = false;
			}
		}
		return valid;
	}

	/**
	 * NOTE: This class use GeoTools library to do the re-projection, which bound connections to a HSQL DB. The connections
	 *     seems to not be managed properly, which lead to potential memory leak and random error messages in the server logs.
	 * @param coordinates Array of coordinates [x1, y1, x2, y2, ...]
	 * @param sourceCRS
	 * @param targetCRS
	 * @return
	 * @throws FactoryException
	 * @throws TransformException
	 */
	public static double[] reprojectCoordinates(double[] coordinates, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) throws FactoryException, TransformException {
		if (coordinates == null || coordinates.length < 2 || sourceCRS == null || targetCRS == null) {
			throw new IllegalArgumentException();
		}

		if (sourceCRS.equals(targetCRS)) {
			// No conversion is needed
			return coordinates;
		}

		double[] reprojectedCoordinates = new double[coordinates.length];

		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS) ;

		for (int i=0; i+1 < coordinates.length; i += 2) {
			Coordinate source = new Coordinate(coordinates[i], coordinates[i+1]);
			Coordinate destination = new Coordinate();
			try {
				JTS.transform(source, destination, transform);
				reprojectedCoordinates[i] = destination.x;
				reprojectedCoordinates[i+1] = destination.y;
			} catch (Exception ex) {
				reprojectedCoordinates[i] = Double.NaN;
				reprojectedCoordinates[i+1] = Double.NaN;
			}
		}

		return reprojectedCoordinates;
	}

	public static JSONObject getMapOptions(String projectionCode) throws JSONException {
		JSONObject projection = SUPPORTED_PROJECTIONS.get(projectionCode);
		JSONObject mapOptions = new JSONObject();

		// Clone the JSONObject and remove the projection name
		Iterator<String> keys = projection.keys();
		while(keys.hasNext()) {
			String key = keys.next();
			if (!"projectionName".equals(key) && !projection.isNull(key)) {
				mapOptions.put(key.trim(), projection.opt(key));
			}
		}

		return mapOptions;
	}
}
