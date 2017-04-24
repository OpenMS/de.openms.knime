package de.openms.knime.importers.mzml;

import java.util.ArrayList;

import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;

public final class MzMLHelper {
	public static ListCell numberArrayToListCell(Number[] n) {
		ArrayList<DoubleCell> cells = new ArrayList<DoubleCell>();
		for (int i = 0; i < n.length; i++) {
			cells.add(new DoubleCell(n[i].doubleValue()));
		}
		return CollectionCellFactory.createListCell(cells);
	}
}
