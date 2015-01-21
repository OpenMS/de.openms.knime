de.openms.knime
===============

Nodes etc. related to OpenMS


Instruction to test OpenMS nightly build in KNIME:

First, the nightly OpenMS nodes have to be installed in KNIME using the Knime nightly build update site:

(Menu) 'Help' -> 
'Install New Software ...' ->
Paste 'nightly - http://tech.knime.org/update/community-contributions/trunk/' in the text field next to work with

Among the listed node categories, the nightly OpenMS nodes should be located in the group 'KNIME Community Contributions (nightly build) - Bioinformatics & NGS' as 'OpenMS'.
Select its checkbox, click 'Next' and follow the Installation instructions of the installation wizard.

After installation of the OpenMS nodes and restarting KNIME, you can then create new workflows using the OpenMS nodes or use existing workflows.

In existing workflows with older OpenMS nodes two things can mainly happen:
- If the workflow is executed, it might fail due to mismatching OpenMS versions in the used configuration and the node. A second execution in this case often replaces the failed node/configuration with a matching up-to-date pair.
- If multiple OpenMS installations exist in your KNIME version, older OpenMS node Versions might execute.

When in doubt whether an OpenMS node in a used workflow is of the correct version, open its configuration dialog, check 'Show advanced parameters' (if applicable) and look at the version number. Nodes added to a workflow after installing the nightly build should automatically have the correct version number.
