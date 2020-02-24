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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.startup.StartupMessage;
import org.knime.workbench.ui.startup.StartupMessageProvider;

import de.openms.knime.startupcheck.registryaccess.WinRegistryQuery;

/**
 * @author aiche
 * 
 */
public class OpenMSStartupMessageProvider implements StartupMessageProvider {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OpenMSStartupMessageProvider.class);
	
	private static final String OPENMS_REQUIREMENTS_URI = "https://abibuilder.informatik.uni-tuebingen.de/archive/openms/OpenMSInstaller/PrerequisitesInstaller/OpenMS-2.5-prerequisites-installer.exe";

	private static final String REG_DWORD_1 = "0x1";
	private static final int BLD_DWORD_VALUE = 0x6ddf; // since VS2015 the registry key is the same. We need to check the min. build number now.
	
	private static ArrayList<String> pwizkeys = new ArrayList<String>(
			Arrays.asList(
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Products\\1af2a8da7e60d0b429d7e6453b3d0182",
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Products\\67D6ECF5CD5FBA732B8B22BAC8DE1B4D",
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Products\\1926E8D15D0BCE53481466615F760A7F"));
	private static ArrayList<String> pwizkeysnew = new ArrayList<String>(
			Arrays.asList(
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Dependencies\\{ca67548a-5ebe-413a-b50c-4b9ceb6d66c6}",
				"HKEY_LOCAL_MACHINE\\SOFTWARE\\Classes\\Installer\\Dependencies\\{050d4fc8-5d48-4b8f-8972-47c82c46020f}"
				));
	private static final String VCREDIST14_OPENMS_X64_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\x64";
	private static final String NET35_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v3.5";
	private static final String NET4_FULL_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full";
	private static final String NET4_CLIENT_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Client";

	@Override
	public List<StartupMessage> getMessages() {
		try {
			if (isWindows()) {

				boolean dotNet4ValueClientExists = WinRegistryQuery.checkValue(
						NET4_CLIENT_KEY, "REG_DWORD", "Install", REG_DWORD_1);
				LOGGER.debug(".NET4 Client Value exists: "
						+ dotNet4ValueClientExists);

				boolean dotNet4ValueFullExists = WinRegistryQuery.checkValue(
						NET4_FULL_KEY, "REG_DWORD", "Install", REG_DWORD_1);
				LOGGER.debug(".NET4 Full Value exists: "
						+ dotNet4ValueFullExists);

				boolean dotNet35ValueExists = WinRegistryQuery.checkValue(
						NET35_KEY, "REG_DWORD", "Install", REG_DWORD_1);
				LOGGER.debug(".NET3.5 1031 Value exists: "
						+ dotNet35ValueExists);

				if (!(dotNet35ValueExists
						&& dotNet4ValueClientExists && dotNet4ValueFullExists)) {
					return getWarning();
				}

				if (is64BitSystem()) {
					boolean pwizok = true;
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
	
	private List<StartupMessage> getPwizWarning() {
		final String longMessage = String
				.format("OpenMS FileConverter for RAW formats depends on ProteoWizard. Not all dependencies found." +
						"Try our prerequisites installer <a href=\"%s\">here</a> first. If it does not help, " +
						"consider installing ALL redistributables from <a href=\"https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads\">here</a>.",
						OPENMS_REQUIREMENTS_URI);
		final String shortMessage = "Some of the OpenMS FileConverter requirements might be missing. Double click for details.";

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
}
