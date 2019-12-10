package org.libero.bom.drop;

import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

/**
 * interface for inject to render item
 * @author hieplq
 *
 */
public interface IRendererListener{
	/**
	 * after {@link ProductBOMRendererListener#render(Treeitem, Treerow, ISupportRadioNode, int)} make row and add a cell for radio, checkbox button.
	 * it call this listener to do far UI
	 * you can set something as your component into attribute of item, and reference at handle of onclick event
	 * @param item
	 * @param row
	 * @param data
	 * @param index
	 */
	public void render(Treeitem item, Treerow row, ISupportRadioNode data, int index);
	
	public void onchecked (Treeitem item, ISupportRadioNode data, boolean isChecked);
}
