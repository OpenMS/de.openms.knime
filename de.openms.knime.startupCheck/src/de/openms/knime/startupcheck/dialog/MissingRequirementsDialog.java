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
package de.openms.knime.startupcheck.dialog;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import de.openms.knime.startupcheck.Activator;
import de.openms.knime.startupcheck.StartupCheck;

/**
 * @author aiche
 */
public class MissingRequirementsDialog extends Dialog {

	private Button btnShowThisWarningAgain;

	private static URI OPENMS_REQUIREMENTS_URI;

	static {
		try {
			OPENMS_REQUIREMENTS_URI = new URI("http://www.openms.de/");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructs the {@link MissingRequirementsDialog} dialog.
	 * 
	 * @param parentShell
	 */
	public MissingRequirementsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gl_container = new GridLayout(2, false);
		gl_container.marginWidth = 15;
		gl_container.marginHeight = 15;
		container.setLayout(gl_container);

		Label lblWarning = new Label(container, SWT.NONE);
		lblWarning.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
				1, 1));
		lblWarning.setImage(Display.getCurrent().getSystemImage(
				SWT.ICON_WARNING));

		Link linkToRequirementsInstaller = new Link(container, SWT.NONE);
		GridData gd_link = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1,
				1);
		gd_link.widthHint = 379;
		linkToRequirementsInstaller.setLayoutData(gd_link);
		linkToRequirementsInstaller
				.setText("Some of the requirements for the OpenMS KNIME Nodes are missing on your system. Please download the requirements installer from our website <a>here.</a>");
		linkToRequirementsInstaller.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.text.equals("here.")) { // the text in the link above
					try {
						Desktop.getDesktop().browse(OPENMS_REQUIREMENTS_URI);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		// fake label to fill in the missing grid element below the warning icon
		new Label(container, SWT.NONE);

		btnShowThisWarningAgain = new Button(container, SWT.CHECK);
		btnShowThisWarningAgain.setSelection(Activator.getInstance()
				.getPreferenceStore()
				.getBoolean(StartupCheck.PREFERENCE_SHOW_AGAIN));
		btnShowThisWarningAgain.setText("Show this warning again.");
		btnShowThisWarningAgain.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Activator
						.getInstance()
						.getPreferenceStore()
						.setValue(StartupCheck.PREFERENCE_SHOW_AGAIN,
								btnShowThisWarningAgain.getSelection());
			}
		});

		return container;
	}
}
