/**
 * --------------------------------------------------------------------------
 *                   OpenMS -- Open-Source Mass Spectrometry
 * --------------------------------------------------------------------------
 * Copyright The OpenMS Team -- Eberhard Karls University Tuebingen,
 * ETH Zurich, and Freie Universitaet Berlin 2002-2013.
 * 
 * This software is released under a three-clause BSD license:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of any author or any participating institution
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * For a full list of authors, refer to the file AUTHORS.
 * --------------------------------------------------------------------------
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL ANY OF THE AUTHORS OR THE CONTRIBUTING
 * INSTITUTIONS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.openms.thirdparty.knime.startupcheck.registryaccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * We utilize the reg command to check if certain values are present in the
 * windows registry.
 * 
 * @author aiche
 */
public class WinRegistryQuery {

	public static String extractFromStdout(final String key, final String valueName, String stdOut) {
		// parse the output line by line and check if we find the hit we
		// were looking for
		String[] lines = stdOut.split(System.getProperty("line.separator"));
		for (int i = 0; i < lines.length; ++i) {
			if (key.equals(lines[i].trim())) {
				++i; // check the next line for the value
				if (lines[i].trim().startsWith(valueName)) {
					return lines[i].trim().split("\\s+")[2];
				}
			}
		}
		return "";
	}
	
	/**
	 * Check
	 * 
	 * @param key
	 *            The registry key.
	 * @param valueName
	 *            The registry value.
	 * @return
	 */
	public static String getValue(final String key, final String type, final String valueName) {

		try {
			String keyAsCmdParameter = "\"" + key + "\"";

			ProcessBuilder processBuilder = new ProcessBuilder("reg", "query",
					keyAsCmdParameter, "/v", valueName, "/t", type);
			Process process = processBuilder.start();

			// fetch return code
			int returnCode = process.waitFor();

			String stdOut = extractStdMessages(process.getInputStream());
			String stdErr = extractStdMessages(process.getErrorStream());

			if (returnCode != 0)
				return "";

			// we expect the error output to be empty
			if (stdErr.trim().length() > 0)
				return "";

			return extractFromStdout(key, valueName, stdOut);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "";
		}
	}
	

	
	/**
	 * Check if the given key/value pair has a dword value of 0x1.
	 * 
	 * @param key
	 *            The registry key.
	 * @param value
	 *            The registry value.
	 * @return
	 */
	public static boolean checkValue(final String key, final String type, final String valueName,
			final String expectedValue) {
		return getValue(key, type, valueName).equals(expectedValue);
	}
	
	/**
	 * Check if the given key/value pair has a dword value of 0x1.
	 * 
	 * @param key
	 *            The registry key.
	 * @param value
	 *            The registry value.
	 * @return
	 */
	public static boolean checkValueGreater(final String key, final String type, final String valueName,
			final int expectedValue, boolean checkGreaterEq) {
		Integer actual = Integer.decode(getValue(key, type, valueName));
		if (checkGreaterEq) {
			return actual >= expectedValue;
		} else {
			return actual < expectedValue;
		}
	}

	private static String extractStdMessages(InputStream stream)
			throws IOException {
		InputStreamReader isr = new InputStreamReader(stream);
		BufferedReader br = new BufferedReader(isr);

		String line = null;
		StringBuffer out = new StringBuffer();

		while ((line = br.readLine()) != null) {
			out.append(line + System.getProperty("line.separator"));
		}

		return out.toString();
	}
}
