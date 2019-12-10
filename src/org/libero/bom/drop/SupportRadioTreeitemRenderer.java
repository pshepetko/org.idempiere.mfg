package org.libero.bom.drop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.zkoss.util.Utils;
import org.zkoss.zk.Version;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Space;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 * render tree item implement {@link ISupportRadioNode}, it will create unique radio group for group radio in a node
 * 
 * @author hieplq
 *
 */
public class SupportRadioTreeitemRenderer implements TreeitemRenderer<ISupportRadioNode>, EventListener<Event> {
	public static final String PROPERTIE_NAME_RADIO_GROUP = "gp_name";
	public static final String DATA_ITEM = "REF_DATA_MODEL";
	public static final String TREE_ITEM = "REF_TREE_ITEM";
	private Boolean needFixIndent = null;
	
	private Map<String, String> mGroupID = new HashMap<String, String>();
	private EventListener<Event> listenerSelection;
	private IRendererListener rendererListener;
	/**
	 * when false only root is expand, other all child will expand
	 */
	public boolean isOpen = false;
	
	public void setCheckedListener (EventListener<Event> listenerSelection){
		this.listenerSelection = listenerSelection;
	}
	
	public void setRendererListener (IRendererListener rendererListener){
		this.rendererListener = rendererListener;
	}
	
	protected void fixIndent (ISupportRadioNode data, Treecell cell, boolean firstLevel){
		if (needFixIndent == null){
			int [] currentVersion = Utils.parseVersion(Version.RELEASE);
			int [] correctVersion = Utils.parseVersion("8.0.0");
			// from 8.0.0, indent for leaf node is more better
			needFixIndent = Utils.compareVersion(currentVersion, correctVersion) < 0;
		}
		
		if (!firstLevel && data.isLeaf() && needFixIndent){
			cell.appendChild(new Space());
			cell.appendChild(new Space());
		}
	}
	
	@Override
	public void render(Treeitem item, ISupportRadioNode data, int index) throws Exception {
		// initialization tree containerized component for a node
		Treerow row = new Treerow();
		Treecell cell = new Treecell();
		cell.setSpan(2);
		item.appendChild(row);
		row.appendChild(cell);
		item.setAttribute(DATA_ITEM, data);
		
		// create check-box or radio
		Checkbox selectionCtr = null;
		if (data.isRadio()){
			Radio radioCtr = new Radio();

			Component groupContainer = null;
			
			// get control for place Radiogroup
			if (item.getParentItem() != null){
				groupContainer = item.getParentItem().getTreerow().getFirstChild();
			}else{
				// top radio in level 1, put Groupradio to root because no where suitable in tree control
				// this groupRadio isn't remove when tree is removed, but it's small, acceptable leak.
				groupContainer = item.getTree().getParent();
			}
			
			// use groupContainer.hashCode() as prefix of groupName to ensure distinct same group name but at difference node
			// see example at ISupportRadioNode.getGroupName
			String uniqueGroupName = groupContainer.hashCode() + data.getGroupName();
			String groupId = mGroupID.get(uniqueGroupName);
			if (groupId == null){
				// use UUID to sure it's unique in spaceID
				UUID groupUUID = UUID.randomUUID();
				groupId = groupUUID.toString();
				mGroupID.put(uniqueGroupName, groupId);
				data.setIsChecked(true);
			}
			
			// create radioGroup
			Component radioGroup = groupContainer.getFellowIfAny(groupId);
			if (radioGroup == null){
				radioGroup = new Radiogroup();
				radioGroup.setId(groupId);
				groupContainer.appendChild(radioGroup);
			}
			
			// set group for radio
			radioCtr.setRadiogroup((Radiogroup)radioGroup);
			
			selectionCtr = radioCtr;
		}else{
			selectionCtr = new Checkbox();
		}
		
		if (rendererListener != null) //red1 renderer listener put here from above so that it already know about the radio group status
			rendererListener.render(item, row, data, index);

		// save reference of data and treeItem to recall when handle event
		selectionCtr.setAttribute(DATA_ITEM, data);
		selectionCtr.setAttribute(TREE_ITEM, item);
		
		selectionCtr.setLabel(data.getLabel());
		
		fixIndent (data, cell, item.getParentItem() == null);
		
		cell.appendChild(selectionCtr);
		
		selectionCtr.setDisabled(data.isDisable());
		
		selectionCtr.setChecked(data.isChecked());
		
		// forward handle event to out side
		selectionCtr.addEventListener(Events.ON_CHECK, this);
		
		item.setOpen(isOpen);
	}

	/**
	 * forward check event out for implement logic anywhere
	 */
	@Override
	public void onEvent(Event event) throws Exception {
		defaultHandleEvent (event);
		if (listenerSelection != null)
			listenerSelection.onEvent(event);
	}
	
	/**
	 * default handle do update model status.
	 * for radio, because don't has event when a radio is unselected so must travel all node to manual unselected
	 * @param event
	 * @throws Exception
	 */
	public void defaultHandleEvent (Event event) throws Exception{
		Object targetObj = event.getTarget();
		if (!(targetObj instanceof Checkbox)){
			return;
		}
		Checkbox chkBox = (Checkbox)targetObj;
		
		ISupportRadioNode dataItem = (ISupportRadioNode)chkBox.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);
	
		dataItem.setIsChecked(chkBox.isChecked());
		
		Treeitem curentTreeItem = (Treeitem)chkBox.getAttribute(SupportRadioTreeitemRenderer.TREE_ITEM);
		
		if (rendererListener != null){
			rendererListener.onchecked(curentTreeItem, dataItem, true);
		}
		
		if (targetObj instanceof Radio){
			
			// find all radio in all next sibling tree has same group to unselected it
			Treeitem nextSiblingTreeItem = (Treeitem)curentTreeItem.getNextSibling();
			while (nextSiblingTreeItem != null){
				ISupportRadioNode dataNodeNext = (ISupportRadioNode)nextSiblingTreeItem.getAttribute(DATA_ITEM);
				if (dataNodeNext.getGroupName().equals(dataItem.getGroupName())){
					dataNodeNext.setIsChecked(false);
					
					if (rendererListener != null){
						rendererListener.onchecked(nextSiblingTreeItem, dataNodeNext, false);
					}
				}
				
				nextSiblingTreeItem = (Treeitem)nextSiblingTreeItem.getNextSibling();
			}
			
			// find all radio in all prev sibling tree has same group to unselected it
			Treeitem prevSiblingTreeItem = (Treeitem)curentTreeItem.getPreviousSibling();
			while (prevSiblingTreeItem != null){
				ISupportRadioNode dataNodePrev = (ISupportRadioNode)prevSiblingTreeItem.getAttribute(DATA_ITEM);
				if (dataNodePrev.getGroupName().equals(dataItem.getGroupName())){
					dataNodePrev.setIsChecked(false);
					
					if (rendererListener != null){
						rendererListener.onchecked(prevSiblingTreeItem, dataNodePrev, false);
					}
				}
				
				prevSiblingTreeItem = (Treeitem)prevSiblingTreeItem.getPreviousSibling();
			}
		}
	}
}
