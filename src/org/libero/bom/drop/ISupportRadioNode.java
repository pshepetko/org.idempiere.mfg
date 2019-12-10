package org.libero.bom.drop;

/**
 * Implement this interface to provide model of tree with support radio
 * almost function of {@link SupportRadioTreeModel} will forward to this class function
 * BOM Drop is reason for grow this interface. understand GUI function of BOM Drop will help to understand this interface
 * 
 * should implement detect all radio is validate (two radio in same group of same node can't also has {@link #isChecked()} = true)
 * this check should implement as utility and call after load all child of a node
 * 
 * it should implement to order all radio item in same group lie together.
 * @author hieplq
 *
 */
public interface ISupportRadioNode {
	/**
	 * Detect this node is leaf node or not.
	 * can detect by number of child node
	 * 
	 * @return
	 */
	public boolean isLeaf ();
	
	/**
	 * Get child at index, so implement of this interface should cache child in a list
	 * @param index
	 * @return
	 */
	public ISupportRadioNode getChild(int index);
	
	/**
	 * number of child node of this node
	 * @return
	 */
	public int getChildCount();
	
	/**
	 * detect this node is a radio node other it's check-box.
	 * @return
	 */
	public boolean isRadio();
	
	/**
	 * all radio in same group, has same group name.
	 * unique group name has scope in a node, at other node it can duplicate.
	 * when group name in other node has same value, {@link SupportRadioTreeitemRenderer} treat as difference group
	 * 
	 * example:
	 * -- node 1
	 *    -- radio 1 in group 1
	 *    -- radio 2 in group 1
	 *    -- radio 3 in group 2
	 *    -- radio 4 in group 2
	 *    -- radio 5 in group 2
	 *    -- radio 6 in group 2
	 * -- node 2
	 *    -- radio 7 in group 1
	 *    -- radio 8 in group 1
	 *    -- radio 9 in group 3
	 *    -- radio 10 in group 3
	 *    
	 * then {@link SupportRadioTreeitemRenderer} will place radio 1, 2 in same group and radio 9,10 in other group.
	 * it also place radio 3,4 in other group with radio 4,6
	 * 
	 * when data for check-box return empty string "" don't return null
	 * @return
	 */
	public String getGroupName();
	
	/**
	 * Node label
	 * @return
	 */
	public String getLabel ();
	
	/**
	 * detect this item is selected
	 * @return
	 */
	public boolean isChecked();
	
	/**
	 * detect this item is can't selection
	 * @return
	 */
	public boolean isDisable();
	
	public void setIsChecked (boolean isChecked);
	
	public void setIsDisable (boolean isDisable);
}