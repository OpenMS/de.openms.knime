/**
 * --------------------------------------------------------------------------
 *                   OpenMS -- Open-Source Mass Spectrometry
 * --------------------------------------------------------------------------
 * Copyright The OpenMS Team -- Eberhard Karls University Tuebingen,
 * ETH Zurich, and Freie Universitaet Berlin 2002-2014.
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
package de.openms.thirdparty.knime.startupcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.startup.StartupMessage;
import org.knime.workbench.ui.startup.StartupMessageProvider;

import de.openms.thirdparty.knime.startupcheck.registryaccess.WinRegistryQuery;

/**
 * This is a class to provide a startup message when the OpenMS Thirdparty plugin is loaded, based on some system checks.
 * @author jpfeuffer
 * 
 */
public class OpenMSThirdpartyStartupMessageProvider implements StartupMessageProvider {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OpenMSThirdpartyStartupMessageProvider.class);
	
	private static final String OPENMS_REQUIREMENTS_URI = "https://abibuilder.cs.uni-tuebingen.de/archive/openms/OpenMSInstaller/PrerequisitesInstaller/OpenMS-3.0-prerequisites-installer.exe";

	private static final String REG_DWORD_1 = "0x1";

	private static final String NET4_FULL_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full";
	private static final String NET4_CLIENT_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Client";
	
	// Those are deprecated dependencies. Pwiz now only requires .NET4 and now also ships redists.
	/*
	private static final int BLD_DWORD_VALUE = 0x6ddf; // since VS2015 the registry key is the same. We need to check the min. build number now for VS2019
	
	private static final String NET35_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v3.5";
	// keys from https://stackoverflow.com/questions/12206314/detect-if-visual-c-redistributable-for-visual-studio-2012-is-installed/27856142
	private static ArrayList<String> pwizkeys = new ArrayList<String>(
			Arrays.asList(
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Products\\67D6ECF5CD5FBA732B8B22BAC8DE1B4D", // VS2008
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Products\\1926E8D15D0BCE53481466615F760A7F")); // VS2010
	private static ArrayList<String> pwizkeysnew = new ArrayList<String>(
			Arrays.asList(
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Dependencies\\{ca67548a-5ebe-413a-b50c-4b9ceb6d66c6}", // VS2012
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Dependencies\\{050d4fc8-5d48-4b8f-8972-47c82c46020f}" // VS2013
				));
	*/
	
	@Override
	public List<StartupMessage> getMessages() {
		try {
			if (isWindows()) {
				if (is64BitSystem()) {
					boolean pwizok = true;
					boolean dotNet4ValueClientExists = WinRegistryQuery.checkValue(
							NET4_CLIENT_KEY, "REG_DWORD", "Install", REG_DWORD_1);
					LOGGER.debug(".NET4 Client Value exists: "
							+ dotNet4ValueClientExists);

					boolean dotNet4ValueFullExists = WinRegistryQuery.checkValue(
							NET4_FULL_KEY, "REG_DWORD", "Install", REG_DWORD_1);
					LOGGER.debug(".NET4 Full Value exists: "
							+ dotNet4ValueFullExists);

					/*
					boolean dotNet35ValueExists = WinRegistryQuery.checkValue(
							NET35_KEY, "REG_DWORD", "Install", REG_DWORD_1);
					LOGGER.debug(".NET3.5 1031 Value exists: "
							+ dotNet35ValueExists);*/

					pwizok = /*dotNet35ValueExists && */ dotNet4ValueClientExists && dotNet4ValueFullExists;

					/*
					for (String key : pwizkeys)
					{
						pwizok = pwizok && WinRegistryQuery
								.checkValue(key, "REG_DWORD", "Assignment",
										REG_DWORD_1);
					}
					
					for (String key : pwizkeysnew)
					{
						pwizok = pwizok && !WinRegistryQuery
								.getValue(key, "REG_SZ", "Version").equals("");
					}
					*/
					
					// This would be an alternative way to check for Redists that does not require reg. keys
					// But it is much slower to load
					/*String command = "Get-WmiObject -Class Win32_Product -Filter \\\"Name LIKE '%Visual C++ 2015%' OR Name LIKE '%Visual C++ 2017%' OR Name LIKE '%Visual C++ 2019%' OR Name LIKE '%Visual C++ 2022%'\\\"";
					boolean vc2015to22Exists;
					try {
						vc2015to22Exists = powershellCMD(command);
					} catch (IOException e) {
						e.printStackTrace();
						vc2015to22Exists = false;
					}
					if (!vc2015to22Exists) {
						return getWarning();
					}
					*/

					if (!pwizok) {
						return getPwizWarning();
					}
				}
			}
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Error when querying windows registry.", e);
		}
		return new ArrayList<StartupMessage>();
	}
	
	private List<StartupMessage> getPwizWarning() {
		final String longMessage = String
				.format("When using the OpenMS node 'FileConverter' with for conversion from vendor formats (e.g., Thermo RAW) it calls the packaged ProteoWizard. " +
						"But not all dependencies for ProteoWizard were found. If you are not intending to use this functionality, please ignore this message. " +
						"Otherwise, try our prerequisites installer <a href=\"%s\">here</a> first (and restart KNIME). If it does not help, " +
						"consider installing ALL redistributables from <a href=\"https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads\">here</a>. " +
						"Please note that our check might not always be uptodate. Try executing 'FileConverter' on a vendor file despite this warning first, before reporting any issues.",
						OPENMS_REQUIREMENTS_URI);
		final String shortMessage = "Some of the requirements for the FileConverter node from OpenMS might be missing. Double click for details.";

		StartupMessage message = new StartupMessage(longMessage, shortMessage,
				StartupMessage.WARNING, Activator.getInstance().getBundle());
		List<StartupMessage> messages = new ArrayList<StartupMessage>();
		messages.add(message);
		return messages;
	}

	private boolean is64BitSystem() {
		return "64".equals(System.getProperty("sun.arch.data.model"));
	}

	private boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}
	
	@SuppressWarnings("unused")
	private boolean powershellCMD(String command) throws IOException
	{
	  //String command = "powershell.exe  your command";
	  //Getting the version
	  String cmd = "powershell.exe " + command;
	  // Executing the command
	  Process powerShellProcess = Runtime.getRuntime().exec(cmd);
	  // Getting the results
	  powerShellProcess.getOutputStream().close();
	  String line;
	  LOGGER.debug("Standard Output:");
	  BufferedReader stdout = new BufferedReader(new InputStreamReader(
	    powerShellProcess.getInputStream()));
	  boolean result = false;
	  while ((line = stdout.readLine()) != null) {
		  LOGGER.debug(line);
		  result = true;
	  }
	  stdout.close();
	  LOGGER.debug("Standard Error:");
	  BufferedReader stderr = new BufferedReader(new InputStreamReader(
	    powerShellProcess.getErrorStream()));
	  while ((line = stderr.readLine()) != null) {
		  LOGGER.debug(line);
	  }
	  stderr.close();
	  return result;
	}

}
