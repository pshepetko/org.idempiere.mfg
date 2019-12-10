/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 */
package org.libero.bom.drop;

import org.zkoss.zul.AbstractTreeModel;

public class SupportRadioTreeModel extends AbstractTreeModel<ISupportRadioNode>  {

	/**
	 * 
	 */ 	
	private static final long serialVersionUID = -4260907076488563930L;
	
	public SupportRadioTreeModel(ISupportRadioNode root) {
		super(root);

	}

	@Override
	public boolean isLeaf(ISupportRadioNode node) {
		return node.isLeaf();
	}

	@Override
	public ISupportRadioNode getChild(ISupportRadioNode parent, int index) {
		return parent.getChild(index);
	}

	@Override
	public int getChildCount(ISupportRadioNode parent) {
		return parent.getChildCount();
	}

}
