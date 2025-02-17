/* SAAF: A static analyzer for APK files.
 * Copyright (C) 2013  syssec.rub.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.rub.syssec.saaf.analysis.steps.extract;

import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import de.rub.syssec.saaf.misc.config.Config;
import de.rub.syssec.saaf.misc.config.ConfigKeys;


/**
 * Interface to the Android-APKtool
 * 
 * @author Martin Ussath
 * @author Hanno Lemoine <hanno.lemoine@gdata.de>
 *
 */
public class ApkDecoderInterface {


	/**
	 * What to do if the apktool installed on the users system is older than ours.
	 * 
	 * @author Tilman Bender <tilman.bender@rub.de>
	 *
	 */
	public enum Treatment{
		DONT_TOUCH,
		RENAME,
		DELETE
	}

	private static final Logger LOGGER = Logger.getLogger(ApkDecoderInterface.class);

	private static final Object MUTEX= new Object();


	/**
	 * Decodes the AndroidManifest.xml file of an APK.
	 * Overwrite the existing encoded AndroidManifest.xml file with the decoded one.
	 * 
	 * @param manifestPath path to the encoded AndroidManifest.xml file
	 */
	// private static void decodeManifest(String manifestPath) throws DecoderException {

	// 	File manifestFile = new File(manifestPath);

	// 	try {
	// 		// Execute AXMLPrinter2.jar to decode the manifest file
	// 		Process process = Runtime.getRuntime().exec(String.format("java -jar lib/AXMLPrinter2.jar %s", manifestPath));
	// 		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	// 		StringBuilder decodedXML = new StringBuilder();
	// 		String line;
	// 		while ((line = reader.readLine()) != null) {
	// 			decodedXML.append(line).append("\n");
	// 		}
	// 		reader.close();
	// 		int exitCode = process.waitFor();
	// 		LOGGER.info(String.format("AXMLPrinter2 exited with code %d.", exitCode));

	// 		// Write the decoded manifest to the manifest file
	// 		BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFile));
	// 		writer.write(decodedXML.toString());
	// 		writer.close();
	// 	} catch (IOException ex) {
	// 		LOGGER.error(String.format("Error while writing the decoded manifest file to %s.", manifestPath), ex);
	// 		throw new DecoderException(ex);
	// 	} catch (InterruptedException ex) {
	// 		LOGGER.error(String.format("Error while decoding the manifest file %s.", manifestPath), ex);
	// 		throw new DecoderException(ex);
	// 	}

	// }


	/**
     * Decodes an APK file.
     *
     * @param apk the given APK file to decode
     * @param destination the destination dir (if already existing, it will be
     * removed firstly)
     * @return false if the decoding crashes
     */
	public static boolean decode(File apk, File destination) throws DecoderException {
		
		boolean isDecodeSuccessful = false;

		synchronized(MUTEX){

			try {

				LOGGER.info(String.format("Decoding APK %s to %s.", apk.getName(), destination.getAbsolutePath()));

				// Execute apktool.jar to decode the given APK
				String cmdApktool = String.format("java -jar lib/static/apktool-2.11.0.jar decode -f -o %s %s", destination.getAbsolutePath(), apk.getAbsolutePath());
				Process process = Runtime.getRuntime().exec(cmdApktool);

				// Read apktool process stdout
				BufferedReader readerStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
				StringBuilder stdout = new StringBuilder();
				String line;
				while ((line = readerStdout.readLine()) != null) {
					stdout.append(line).append("\n");
				}
				readerStdout.close();

				// Read apktool process stderr
				BufferedReader readerStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				StringBuilder stderr = new StringBuilder();
				while ((line = readerStderr.readLine()) != null) {
					stderr.append(line).append("\n");
				}
				readerStderr.close();
                
				// Output
				int exitCode = process.waitFor();
				LOGGER.info(String.format("apktool exited with code %d.", exitCode));
				LOGGER.info("apktool's stdout:\n");
				LOGGER.info(stdout.toString());
				LOGGER.info("apktool's stderr:\n");
				LOGGER.info(stderr.toString());

				isDecodeSuccessful = true;

			} catch (IOException ex) {
				LOGGER.error(String.format("Error while decoding the APK %s.", apk.getName()), ex);
				throw new DecoderException(ex);
			} catch (InterruptedException ex) {
				LOGGER.error(String.format("Error while decoding the APK %s.", apk.getName()), ex);
				throw new DecoderException(ex);
			}
	        
	    }

        return isDecodeSuccessful;
	}

}
