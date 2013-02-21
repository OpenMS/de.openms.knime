/**
 * --------------------------------------------------------------------------
 *                   OpenMS -- Open-Source Mass Spectrometry
 * --------------------------------------------------------------------------
 * Copyright The OpenMS Team -- Eberhard Karls University Tuebingen,
 * ETH Zurich, and Freie Universitaet Berlin 2002-2012.
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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import de.openms.knime.startupcheck.dialog.MissingRequirementsDialog;
import de.openms.knime.startupcheck.registryaccess.WinRegistryQuery;

/**
 * The earlyStartup() will be executed on startup of Eclipse to check if all
 * prerequistes are installed on windows.
 * 
 * @author aiche
 */
public class StartupCheck implements IStartup {

	public static final String PREFERENCE_SHOW_AGAIN = "show_warning_again";
	private static final String REG_DWORD_1 = "0x1";
	private static final String VCREDIST_X64_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\x64";
	private static final String VCREDIST_X86_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\x86";
	private static final String NET35_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v3.5";
	private static final String NET4_FULL_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full";
	private static final String NET4_CLIENT_KEY = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Client";
	// debug logger
	private final static Logger LOGGER;

	static {
		BasicConfigurator.configure();
		LOGGER = Logger.getLogger(StartupCheck.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
		// make sure we have the correct default for our properties
		Activator.getInstance().getPreferenceStore()
				.setDefault(PREFERENCE_SHOW_AGAIN, true);

		try {
			if (isWindows()) {
				boolean dotNet4ValueClientExists = WinRegistryQuery.checkDWord(
						NET4_CLIENT_KEY, "Install", REG_DWORD_1);
				LOGGER.debug(".NET4 Client Value exists: "
						+ dotNet4ValueClientExists);

				boolean dotNet4ValueFullExists = WinRegistryQuery.checkDWord(
						NET4_FULL_KEY, "Install", REG_DWORD_1);
				LOGGER.debug(".NET4 Full Value exists: "
						+ dotNet4ValueFullExists);

				boolean dotNet35ValueExists = WinRegistryQuery.checkDWord(
						NET35_KEY, "Install", REG_DWORD_1);
				LOGGER.debug(".NET3.5 1031 Value exists: "
						+ dotNet35ValueExists);

				boolean vcRedist2010_x86ValueExists = WinRegistryQuery
						.checkDWord(VCREDIST_X86_KEY, "Installed", REG_DWORD_1);
				LOGGER.debug("VC10 x86 Redist Value exists: "
						+ vcRedist2010_x86ValueExists);

				if (!(vcRedist2010_x86ValueExists && dotNet35ValueExists
						&& dotNet4ValueClientExists && dotNet4ValueFullExists)) {
					showWarningDialog();
				}

				if (is64BitSystem()) {
					boolean vcRedist2010_x64ValueExists = WinRegistryQuery
							.checkDWord(VCREDIST_X64_KEY, "Installed",
									REG_DWORD_1);
					LOGGER.debug("VC10 x64 Redist Value exists: "
							+ vcRedist2010_x64ValueExists);

					if (!vcRedist2010_x64ValueExists) {
						showWarningDialog();
					}
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	private void showWarningDialog() {
		if (Activator.getInstance().getPreferenceStore()
				.getBoolean(PREFERENCE_SHOW_AGAIN)) {

			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					MissingRequirementsDialog mrDialog = new MissingRequirementsDialog(
							PlatformUI.getWorkbench().getDisplay()
									.getActiveShell());
					mrDialog.create();
					mrDialog.open();
				}
			});
		}
	}

	private boolean is64BitSystem() {
		return "64".equals(System.getProperty("sun.arch.data.model"));
	}

	private boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}
}