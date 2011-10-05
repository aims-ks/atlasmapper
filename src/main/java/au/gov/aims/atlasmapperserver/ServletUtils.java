/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This utility class provide tools to simplify some operation related
 * to servlets. Unfortunately, this class can not be test since the servlet
 * libraries can not be load in the context of a Test Case.
 *
 * @author glafond
 */
public class ServletUtils {
	private static final Logger LOGGER = Logger.getLogger(ServletUtils.class.getName());

	public static void sendResponse(
			HttpServletRequest request,
			HttpServletResponse response,
			File file) throws IOException {

		if (response == null || file == null) {
			return;
		}

		InputStream responseStream = null;
		try {
			responseStream = new FileInputStream(file);
			ServletUtils.sendResponse(request, response, responseStream);
		} finally {
			if (responseStream != null) {
				try { responseStream.close(); } catch (Exception ex) { LOGGER.log(Level.WARNING, "Cant close the FileInputStream.", ex); }
			}
		}
	}

	public static void sendResponse(
			HttpServletRequest request,
			HttpServletResponse response,
			String responseTxt) throws IOException {

		if (response == null || responseTxt == null) {
			return;
		}

		InputStream responseStream = null;
		try {
			responseStream = new ByteArrayInputStream(responseTxt.getBytes());
			ServletUtils.sendResponse(request, response, responseStream);
		} finally {
			if (responseStream != null) {
				try { responseStream.close(); } catch (Exception ex) { LOGGER.log(Level.WARNING, "Cant close the ByteArrayInputStream.", ex); }
			}
		}
	}

	public static void sendResponse(
			HttpServletRequest request,
			HttpServletResponse response,
			InputStream responseStream) throws IOException {

		if (response == null || responseStream == null) {
			return;
		}

		OutputStream out = null;

		try {
			out = response.getOutputStream();
			byte[] buf = new byte[32 * 1024];  // 32K buffer
			int bytesRead;
			while ((bytesRead = responseStream.read(buf)) != -1) {
				out.write(buf, 0, bytesRead);
			}
		}
		finally {
			if (out != null) {
				try { out.flush(); } catch(Exception e) { LOGGER.log(Level.SEVERE, "Cant flush the output.", e); }
				try { out.close(); } catch(Exception e) { LOGGER.log(Level.SEVERE, "Cant close the output.", e); }
			}
		}
	}
}
