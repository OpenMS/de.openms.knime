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
package de.openms.knime.startupcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.startup.StartupMessage;
import org.knime.workbench.ui.startup.StartupMessageProvider;

//import de.openms.knime.startupcheck.registryaccess.WinRegistryQuery;

/**
 * This is a class to provide a startup message when the OpenMS plugin is loaded, based on some system checks.
 * @deprecated Since OpenMS ships ALL dependencies now, this is not needed anymore. For thirdparty tools,
 * 	please see the de.openms.thirdparty.knime.startpCheck plugin.
 * @author jpfeuffer
 * 
 */
public class OpenMSStartupMessageProvider implements StartupMessageProvider {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OpenMSStartupMessageProvider.class);
	
	private static final String OPENMS_REQUIREMENTS_URI = "https://abibuilder.cs.uni-tuebingen.de/archive/openms/OpenMSInstaller/PrerequisitesInstaller/OpenMS-3.0-prerequisites-installer.exe";
	//private static final String REG_DWORD_1 = "0x1";

	// Those are deprecated dependencies. OpenMS now ships its redist. Pwiz now only requires .NET4 and now also ships redists.
	/*
	private static final String VCREDIST14_OPENMS_X64_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\x64"; //Bundle VS2015,2017,2019
	private static final int BLD_DWORD_VALUE = 0x6ddf; // since VS2015 the registry key is the same. We need to check the min. build number now for VS2019
	*/
	
	@Override
	public List<StartupMessage> getMessages() {
		if (isWindows()) {
				// This would be an alternative way to check for Redists that does not require reg. keys
				// But it is much slower to load
				/*
				String command = "Get-WmiObject -Class Win32_Product -Filter \\\"Name LIKE '%Visual C++ 2015%' OR Name LIKE '%Visual C++ 2017%' OR Name LIKE '%Visual C++ 2019%' OR Name LIKE '%Visual C++ 2022%'\\\"";
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
				
				/* OpenMS now ships the redist in the lib folder
				boolean vcRedist2014_x64ValueExists = WinRegistryQuery
						.checkValue(VCREDIST14_OPENMS_X64_KEY, "REG_DWORD", "Installed",
								REG_DWORD_1);
				LOGGER.debug("VC14 x64 Redist Value exists: "
						+ vcRedist2014_x64ValueExists);
				boolean vcRedist2014_x64BldValueEnough = WinRegistryQuery
						.checkValueGreater(VCREDIST14_OPENMS_X64_KEY, "REG_DWORD", "Bld",
								BLD_DWORD_VALUE, true);
				LOGGER.debug("VC14 x64 Redist Value large enough.");

				if (!(vcRedist2014_x64ValueExists && vcRedist2014_x64BldValueEnough)) {
					return getWarning();
				}
				*/
		}
		return new ArrayList<StartupMessage>();
	}

	@SuppressWarnings("unused")
	private List<StartupMessage> getWarning() {
		final String longMessage = String
				.format("Some of the requirements for the OpenMS KNIME Nodes are missing on your system. " +
						"Please download the requirements installer from <a href=\"%s\">here</a>.",
						OPENMS_REQUIREMENTS_URI);
		final String shortMessage = "Some of the OpenMS KNIME Nodes requirements might be missing. Double click for details.";

		StartupMessage message = new StartupMessage(longMessage, shortMessage,
				StartupMessage.WARNING, Activator.getInstance().getBundle());
		List<StartupMessage> messages = new ArrayList<StartupMessage>();
		messages.add(message);
		return messages;
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
