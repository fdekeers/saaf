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
import java.util.regex.Pattern;


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

	private static final Object MUTEX = new Object();


	private static File[] getOtherSmaliDirs(File parentDir) {

		final Pattern pattern = Pattern.compile("smali_classes[0-9]+");
		FilenameFilter filenameFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return pattern.matcher(name).matches();
			}
		};

		return parentDir.listFiles(filenameFilter);

	}


	private static void copyFile(File targetFile, File sourceFile)
	throws IOException {
		
		// Create parent directories if they do not exist
		targetFile.getParentFile().mkdirs();

		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(sourceFile);
			fos = new FileOutputStream(targetFile);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer)) > 0) {
				fos.write(buffer, 0, length);
			}
			fis.close();
			fos.close();
		} finally {
			if (fis != null) {
				fis.close();
			}
			if (fos != null) {
				fos.close();
			}
		}

	}


	private static void mergeDirs(File targetDir, File sourceDir)
	throws IOException {
		
		File[] sourceFiles = sourceDir.listFiles();
		if (sourceFiles != null) {
			for (File sourceFile : sourceFiles) {
				File targetFile = new File(targetDir, sourceFile.getName());
				if (sourceFile.isDirectory()) {
					mergeDirs(targetFile, sourceFile);
				} else {
					if (! targetFile.exists()) {
						copyFile(targetFile, sourceFile);
					}
				}
			}
		}

	}


	private static void mergeSmaliDirs(File parentDir)
	throws IOException {

		File smaliBaseDir = new File(parentDir, "smali");
		File[] smaliOtherDirs = getOtherSmaliDirs(parentDir);

		if (smaliOtherDirs.length == 0) {
			LOGGER.info("No other smali directories found.");
			return;
		}

		for (File smaliOtherDir : smaliOtherDirs) {
			mergeDirs(smaliBaseDir, smaliOtherDir);
		}

	}


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


			// Merge smali directories
			try {
				mergeSmaliDirs(destination);
			} catch (IOException ex) {
				LOGGER.error(String.format("Error while merging smali directories of the APK %s.", apk.getName()), ex);
				throw new DecoderException(ex);
			}
	        
	    }

        return isDecodeSuccessful;
	}

}
