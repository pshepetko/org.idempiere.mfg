package org.libero.bom.drop;
  
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
 
import org.adempiere.webui.component.NumberBox;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.compiere.model.MProduct;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.model.MPPProductBOMLine;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

public class ProductBOMRendererListener implements IRendererListener, PropertyChangeListener, ValueChangeListener {
	protected static CLogger log = CLogger.getCLogger(ProductBOMRendererListener.class);
	public static final String QTY_COMPONENT = "qty_component";
	public static final String TOTAL_QTY = "total_qty";
	public static final String TOTAL_PRICE = "total_price";
	public static final String Tree_ITEM = "tree_item";
	// need reference to process PropertyChangeListener. model need method to back reference input control
	protected Tree tree; 
	private static BigDecimal GrandTotal = Env.ZERO; 
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this); 
	public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
	@Override
	public void render(Treeitem item, Treerow row, ISupportRadioNode data, int index) {
		if (data != null && data instanceof ProductBOMTreeNode){
			BigDecimal buffer = Env.ZERO;
			ProductBOMTreeNode productBOMTreeNode = (ProductBOMTreeNode)data; 
			Treecell available = new Treecell();
			row.appendChild(available);
			Treecell inputcell = new Treecell();
			row.appendChild(inputcell);
			Treecell pricecell = new Treecell();
			row.appendChild(pricecell);
			Treecell totcell = new Treecell();
			row.appendChild(totcell);
			Treecell totalpricecell = new Treecell();
			row.appendChild(totalpricecell);
			
			if (item.getTree().getTreecols() != null && item.getTree().getTreecols().getChildren().size() < row.getChildren().size()){
				item.getTree().getTreecols().appendChild(new Treecol()); 
				item.getTree().getTreecols().appendChild(new Treecol(Msg.translate(Env.getCtx(), "QtyAvailable")));
				item.getTree().getTreecols().appendChild(new Treecol(Msg.translate(Env.getCtx(), "Qty")));
				item.getTree().getTreecols().appendChild(new Treecol(Msg.translate(Env.getCtx(), "Price")));
				item.getTree().getTreecols().appendChild(new Treecol(Msg.translate(Env.getCtx(), "TotalQty")));
				item.getTree().getTreecols().appendChild(new Treecol(Msg.translate(Env.getCtx(), "TotalPrice")));
			}
			
			boolean editQty = false;
			editQty = MPPProductBOMLine.COMPONENTTYPE_Variant.equals(productBOMTreeNode.getComponentType());
			
 			productBOMTreeNode.getLabel(); //to trigger qty available; 
			Label availableQty = new Label();			
			WNumberEditor inputQty = new WNumberEditor();
			NumberBox price = new NumberBox(false);
			NumberBox totQty = new NumberBox(false);
			NumberBox totPrice = new NumberBox(false);
			price.setEnabled(false);
			totQty.setEnabled(false);
			totPrice.setEnabled(false);
			price.getDecimalbox().setScale(2);
			totPrice.getDecimalbox().setStyle("text-align:right"); 
 			inputQty.setReadWrite(editQty); 
 			totQty.getDecimalbox().setScale(2);
 			totPrice.getDecimalbox().setScale(2);
 			
			available.appendChild(availableQty);
			inputcell.appendChild(inputQty.getComponent());
			pricecell.appendChild(price);
			totcell.appendChild(totQty);
			totalpricecell.appendChild(totPrice);
			
			if (productBOMTreeNode.productBOMLine != null){
				availableQty.setValue(productBOMTreeNode.getQtyAvailable().toString());
				inputQty.setValue(productBOMTreeNode.getQty());
				price.setValue(productBOMTreeNode.getRowPrice());
				totQty.setValue(productBOMTreeNode.getTotQty());
				totPrice.setValue(productBOMTreeNode.calculateRowTotalPrice(productBOMTreeNode.getTotQty()));

				item.setAttribute(TOTAL_QTY, totQty);
				totQty.setAttribute(Tree_ITEM, item);
				item.setAttribute(TOTAL_PRICE, totPrice);
				totPrice.setAttribute(Tree_ITEM, item);
				
				//refresh again with unchecked as zeroes. check IsParentChecked too.  				 
				if (!isParentChecked(item)){
					totQty.setValue(Env.ZERO);
					totPrice.setValue(Env.ZERO);
				} 
				if (productBOMTreeNode.getChildCount()>0){
					//if this is a parent then set to Zero for later roll up calculation
					totPrice.setValue(Env.ZERO);
				}
				//Grand Total setting updating display when changed from last loop.
				GrandTotal = GrandTotal.add((BigDecimal) totPrice.getValue());
				this.propertyChangeSupport.firePropertyChange("GrandTotal", buffer, GrandTotal);
				buffer = GrandTotal;
			}
			else
				log.warning(data.toString());
			
			item.setAttribute(QTY_COMPONENT, inputQty);
			inputQty.getComponent().setAttribute(Tree_ITEM, item);
			productBOMTreeNode.addPropertyChangeListener(this);
			inputQty.addValueChangeListener(this); 
		}		
	}
	
	/**
	 * This is only run at end of routine to roll up from the bottom descendants to sum for each parent node total prices
	 * 
	 */
	private void rollUpParentNodeTotalPricing(Treeitem treeItem) {
 		//Starting from the top 
		ProductBOMTreeNode rootNode = (ProductBOMTreeNode) tree.getModel().getRoot();
  		if (rootNode.getChildCount()>0){
 			BigDecimal grandtotalprice = rollupRoutine(rootNode.bomChilds);
			if (grandtotalprice.compareTo(GrandTotal)==0)
				log.info("Grand Total CORRECT = Sum of Parent Nodes");
			else
				log.info("Grand Total ERROR != Sum sub parent nodes");
 		}
	}
	/**
	 * RollUp done from bottom
	 * @param parent
	 */
	private BigDecimal rollupRoutine(List<ProductBOMTreeNode> bomchildren){
		BigDecimal nodeTotalPrice = Env.ZERO;
		//this is the top level going downwards
		for (ProductBOMTreeNode node:bomchildren){
			//going downwards
 			if (node.getChildCount()>0) {
				BigDecimal totalPrice = rollupRoutine(node.bomChilds);	
				int [] pathToNode = tree.getModel().getPath(node);
				Treeitem treeItem = tree.renderItemByPath(pathToNode);
				NumberBox itemTotalPrice = (NumberBox)treeItem.getAttribute(TOTAL_PRICE);
				itemTotalPrice.setValue(totalPrice);
				itemTotalPrice.getDecimalbox().setStyle("font-size:16px;color:gray;text-align:right;font-weight: bold");
				if (treeItem.getLevel()>0){
					Integer fontsize = treeItem.getRoot().getChildren().size()-treeItem.getLevel()+13; 
					itemTotalPrice.getDecimalbox().setStyle("font-size:"+fontsize.toString()+"px;color:gray;text-align:right;font-weight: bold");
				}
				nodeTotalPrice = nodeTotalPrice.add((BigDecimal)itemTotalPrice.getValue());	
			}				
			else {
				//just sum these bomChildren and set to its parent TotalPrice		
				int [] pathToNode = tree.getModel().getPath(node);
				Treeitem bomitem = tree.renderItemByPath(pathToNode);
				NumberBox itemTotalPrice = (NumberBox)bomitem.getAttribute(TOTAL_PRICE);
				nodeTotalPrice = nodeTotalPrice.add(itemTotalPrice.getValue());				
			}
		}
		return nodeTotalPrice;
	}
	
	/**
	 * 
	 * @param thisItem
	 * @return
	 */
	private static boolean isParentChecked (Treeitem thisItem){
		if (thisItem == null)
			return true;		
		Treeitem parentItem = thisItem; //start from present item
		// isn't root
		while (parentItem != null){
			ProductBOMTreeNode dataItem = (ProductBOMTreeNode)parentItem.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);
			if (!dataItem.isChecked() || dataItem.getTotQty().compareTo(Env.ZERO)==0){
				return false;
			}

			thisItem = parentItem;
			parentItem = thisItem.getParentItem();
		}
		return true;
	}

	@Override
	public void onchecked(Treeitem item, ISupportRadioNode data,
			boolean isChecked) { 
		//if checked, cascade all children totals to unit qtys X parent BOMQty
		// multiply with parentQty
		BigDecimal totQty = Env.ZERO;
		BigDecimal totPrice = Env.ZERO;
		if (isParentChecked(item)){  
			totQty = ((ProductBOMTreeNode) data).getQty(); //set initial unit qty
			//getParent TotQty
			Treeitem parent = item.getParentItem();
			if (parent!=null){
				NumberBox totalQtyComponent = (NumberBox)parent.getAttribute(TOTAL_QTY);
				totQty = totQty.multiply((BigDecimal)totalQtyComponent.getValue());
			}
			totPrice = ((ProductBOMTreeNode) data).calculateRowTotalPrice(totQty);

			//Checking will add total price to grand total. Un-check will subtract..
			 if( item.isEmpty())
				 GrandTotal = GrandTotal.add(totPrice);
		}  else if (item.isEmpty()){
			NumberBox totalQtyComponent = (NumberBox)item.getAttribute(TOTAL_QTY); 
			GrandTotal = GrandTotal.subtract(((ProductBOMTreeNode) data).calculateRowTotalPrice(totalQtyComponent.getValue()));
		}
		int [] pathToNode = tree.getModel().getPath(data);
		Treeitem treeItem = tree.renderItemByPath(pathToNode);
		NumberBox totalQtyComponent = (NumberBox)item.getAttribute(TOTAL_QTY); 
 		totalQtyComponent.setValue(totQty);
 		NumberBox totPriceComponent = (NumberBox)treeItem.getAttribute(TOTAL_PRICE);
		//then propertyChange is fired to check change and update display
		BigDecimal oldvalue = totPriceComponent.getValue();
		this.propertyChangeSupport.firePropertyChange("GrandTotal", totPrice, oldvalue);
		totPriceComponent.setValue(totPrice);
		//and cascading happens if there is change and children, zero qty must be processed to cascade correctly
		if (!treeItem.isEmpty()){ //assuming from price change  -- cannot consider: totPrice.compareTo(oldvalue)!=0 && 
			cascadeChildren(treeItem);//after cascading execute the rollUp with the last tree item			
		}
		rollUpParentNodeTotalPricing(treeItem);
	}
	/**
	 * 
	 * @param treeItem
	 * @param parentQty
	 */
	private void cascadeChildren(Treeitem treeItem) {
 		//NOTE: no need to get Parent BOMQty as this is done work prior. 
		// Just cascade to handle children values. 
		Treechildren tch = treeItem.getTreechildren();
		if (tch!=null){
			Collection<Treeitem> children = tch.getItems();
			for (Treeitem child:children){ 
				ProductBOMTreeNode treeNode = (ProductBOMTreeNode)child.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);
				int [] pathToNode = tree.getModel().getPath(treeNode);
				child = tree.renderItemByPath(pathToNode);
				//getting ParentNode to get its TotalQty
				Treeitem parentTreeItem = child.getParentItem();
				ProductBOMTreeNode parentNode = (ProductBOMTreeNode)parentTreeItem.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);					
				//calculate new Total Qty
				BigDecimal totQty = Env.ZERO; //if unchecked TotQty is ZERO
				
				if (isParentChecked(child)){ // 
					totQty = treeNode.getQty().multiply(parentNode.getTotQty());				
				} 
				NumberBox totQtyComponent = (NumberBox)child.getAttribute(TOTAL_QTY);
				totQtyComponent.setValue(totQty);
			
				NumberBox totPriceComponent = (NumberBox)child.getAttribute(TOTAL_PRICE);
				//then propertyChange is fired to check change and update display
				BigDecimal oldPrice = totPriceComponent.getValue();
				BigDecimal totPrice = totQty.multiply(treeNode.getPriceStdAmt());
				GrandTotal = GrandTotal.subtract((BigDecimal) oldPrice);
				GrandTotal = GrandTotal.add(totPrice);
				this.propertyChangeSupport.firePropertyChange("GrandTotal", totPrice, oldPrice);
				totPriceComponent.setValue(totPrice); 
			}
		}  
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {  
		// handle event when qty in model node change. update value display at input control
		ProductBOMTreeNode nodeChange = (ProductBOMTreeNode) evt.getSource();
		int [] pathToNode = tree.getModel().getPath(nodeChange);
		Treeitem treeItem = tree.renderItemByPath(pathToNode);
		WNumberEditor editor = (WNumberEditor)treeItem.getAttribute(QTY_COMPONENT);
		BigDecimal newQty = (BigDecimal)evt.getNewValue();
		editor.setValue(newQty);
		
		//recalculate TotQty and TotPrice
		Treeitem parent = treeItem.getParentItem();
		BigDecimal newPrice = nodeChange.getPriceStdAmt();
		BigDecimal parentTotQty = Env.ONE;
		if (isParentChecked(treeItem) && parent!=null){
			ProductBOMTreeNode parentNode = (ProductBOMTreeNode)parent.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);			
			parentTotQty = ((ProductBOMTreeNode) parentNode).getTotQty();
		}
		BigDecimal totQty = newQty.multiply(parentTotQty);
		newPrice = newPrice.multiply(totQty);			
		NumberBox totQtyComponent = (NumberBox)treeItem.getAttribute(TOTAL_QTY);	
		totQtyComponent.setValue(totQty);
		NumberBox totPriceComponent = (NumberBox)treeItem.getAttribute(TOTAL_PRICE);
		
		GrandTotal = GrandTotal.subtract((BigDecimal) totPriceComponent.getValue());
		GrandTotal = GrandTotal.add(newPrice);
		this.propertyChangeSupport.firePropertyChange("GrandTotal",(BigDecimal) totPriceComponent.getValue(),newPrice);

		totPriceComponent.setValue(newPrice);
		 
		nodeChange.setTotQty(totQty);
		//cascading to children to follow the change BOMQty
		//and cascading happens here IF there is change, but the zero test later saves time.
		if ((!treeItem.isEmpty())) //assuming from price change
			cascadeChildren(treeItem); //newvalue is parentqty in cascadeChildren
	}

	@Override
	public void valueChange(ValueChangeEvent evt) {
		// handle event when user update qty value. process update value of children node
		Treeitem treeItem = (Treeitem) ((WNumberEditor)evt.getSource()).getComponent().getAttribute(Tree_ITEM);
		ProductBOMTreeNode nodeModel = (ProductBOMTreeNode)treeItem.getAttribute(SupportRadioTreeitemRenderer.DATA_ITEM);
		nodeModel.setQty((BigDecimal)evt.getNewValue());
	}
 
	public void setTree (Tree tree){
		this.tree = tree;
	}
	
	public static String getGrandTotal(){		
		return GrandTotal.setScale(2).toString();
	}
	public static void setGrandTotal(BigDecimal a){
		GrandTotal=a;
	}
	 
}
