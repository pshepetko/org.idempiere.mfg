/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 */

package org.libero.bom.drop;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

public class ProductBOMTreeNode implements ISupportRadioNode {
	protected static CLogger log = CLogger.getCLogger(ProductBOMTreeNode.class);
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	MProduct product;
	MPPProductBOMLine productBOMLine;
	List<ProductBOMTreeNode> bomChilds;
	boolean invidateState = false;
	boolean isChecked = false;
	private ComparatorBOMTreeNode comparatorBOMTreeNode = new ComparatorBOMTreeNode();
	private BigDecimal unitQty = BigDecimal.ZERO;
	private BigDecimal totQty = BigDecimal.ZERO; 
	private BigDecimal qtyAvailable;
	private BigDecimal priceStdAmt=Env.ZERO;
	private BigDecimal priceTotalAmt=Env.ZERO;
	public static int PriceListVersion = 0;
	public ProductBOMTreeNode(MProduct product, BigDecimal qty){
		this.product = product;
		this.unitQty = qty;
	}
	
	public ProductBOMTreeNode(MPPProductBOMLine productBOMLine, BigDecimal parrentQty){
		this.productBOMLine = productBOMLine;
		this.totQty = parrentQty.multiply(productBOMLine.getQty());
		this.unitQty = productBOMLine.getQty();
	}
	
	protected void initChilds (){
		if (bomChilds != null){
			return;//loaded
		}
		bomChilds = new ArrayList<ProductBOMTreeNode>();
		MPPProductBOMLine [] bomLines = null;
		
		if (product != null){
			MPPProductBOM bom = new Query(Env.getCtx(), MPPProductBOM.Table_Name, MPPProductBOM.COLUMNNAME_M_Product_ID+"=?",null)
			.setParameters(product.get_ID())
			.first();
			bomLines = bom.getLines();	
		}else if (productBOMLine != null){
			MPPProductBOM bom = new Query(Env.getCtx(),MPPProductBOM.Table_Name,MPPProductBOM.COLUMNNAME_M_Product_ID+"=?",null)
			.setParameters(productBOMLine.getM_Product_ID())
			.first();
			if (bom!=null)	
				bomLines = bom.getLines();
				else {
					//handle Attribute Set as radio group if exist.
				};
		}else{
			invidateState = true;
			return;
		}
		if (bomLines!=null){
			for (MPPProductBOMLine bomLine : bomLines){
				bomChilds.add(new ProductBOMTreeNode(bomLine, this.totQty.compareTo(Env.ZERO)>0? this.totQty:this.unitQty));
			}
		}
		
		// short let radio same group lie together
		Collections.sort(bomChilds, comparatorBOMTreeNode);
			
	}
	
	@Override
	public boolean isLeaf() {
		initChilds();		
		return bomChilds.size() == 0;
	}

	@Override
	public ISupportRadioNode getChild(int index) {
		initChilds();
		return bomChilds.get(index);
	}

	@Override
	public int getChildCount() {
		initChilds();
		return bomChilds.size();
	}

	@Override
	public boolean isRadio() {  
		return (!MPPProductBOMLine.COMPONENTTYPE_Option.equals(getComponentType())
				&& !MPPProductBOMLine.COMPONENTTYPE_Component.equals(getComponentType())
				&& !MPPProductBOMLine.COMPONENTTYPE_Variant.equals(getComponentType()));
	}

	@Override
	public String getGroupName() {
		if (isRadio())
			return getComponentType();
		// importance because inside render don't check null
		return "";
	}

	@Override
	public boolean isChecked() {
		if (MPPProductBOMLine.COMPONENTTYPE_Component.equals(getComponentType()) || MPPProductBOMLine.COMPONENTTYPE_Variant.equals(getComponentType()))
			return true;
		
		return isChecked;
	}
	/**
	 * Only Variant and Component is disabled to edit checkbox
	 */
	@Override
	public boolean isDisable() {
		return (MPPProductBOMLine.COMPONENTTYPE_Variant.equals(getComponentType()) 
				|| MPPProductBOMLine.COMPONENTTYPE_Component.equals(getComponentType()));
	}

	@Override
	public void setIsChecked(boolean isChecked) {
		if (MPPProductBOMLine.COMPONENTTYPE_Component.equals(getComponentType()) || MPPProductBOMLine.COMPONENTTYPE_Variant.equals(getComponentType()))
			return;//non effect for standard part, always checked
		
		this.isChecked = isChecked;
	}

	@Override
	public void setIsDisable(boolean isDisable) {
		return;//not effect never must manual set it

	}
	
	protected String getComponentType (MPPProductBOMLine bomLine){
		if (bomLine != null && bomLine.getComponentType() != null) 
			return bomLine.getComponentType();
		
		return MPPProductBOMLine.COMPONENTTYPE_Component;
	}
	
	protected String getComponentType (){		// 
		return getComponentType (productBOMLine);
	}
	
	class ComparatorBOMTreeNode implements Comparator<ProductBOMTreeNode>{

		@Override
		public int compare(ProductBOMTreeNode bom1, ProductBOMTreeNode bom2) {
			if (getComponentType(bom1.productBOMLine).equals(getComponentType(bom2.productBOMLine))){
				return 0;
			}
			
			String t1 = String.valueOf(bom1.productBOMLine.getLine()+100000);
            String t2 = String.valueOf(bom2.productBOMLine.getLine()+100000);
            return t1.compareTo(t2);
		}
	}

	/**
	 * Label =+ QtyOnHand (Available) -- red1
	 */
	@Override
	public String getLabel() {
		String label = "";
		if (productBOMLine != null) {
			label = productBOMLine.getProduct().getName();
			qtyAvailable = getQtyOnHand(productBOMLine.getProduct());
		}			
		else if (product != null){
			label = product.getName();
			qtyAvailable = getQtyOnHand(product);
		}
 		return label;
	}
	
	public BigDecimal getQtyAvailable(){
		return qtyAvailable;
	}
	
	 private BigDecimal getQtyOnHand(MProduct prod) {
			BigDecimal qtyOnHand = BigDecimal.ZERO;
			MStorageOnHand[] storages = MStorageOnHand.getOfProduct(Env.getCtx(), prod.getM_Product_ID(), prod.get_TrxName());		
			for(MStorageOnHand storage:storages) {	
				if(storage != null) { 	
					qtyOnHand = qtyOnHand.add(storage.getQtyOnHand());
				}
			}
			return qtyOnHand;
		}

	public int getProductID (){
		if (product == null && productBOMLine == null)
			throw new IllegalStateException ("no product info in this node");
		
		MProduct productNode = product != null ? product:productBOMLine.getProduct();
		return productNode.get_ID();
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
	public BigDecimal getQty() {
		return unitQty;
	}
	public BigDecimal getTotQty(){
		return totQty;
	}
	public void setTotQty(BigDecimal totQty){
		this.totQty=totQty;
	}
	
	public void setQty(BigDecimal qty) {
		if (!this.unitQty.equals(qty)){
			BigDecimal oldValue = this.unitQty;
			this.unitQty = qty;
			this.propertyChangeSupport.firePropertyChange("qty", oldValue, this.unitQty);
		}		
	}

	/**
	 * 
	 * @return
	 */
	public BigDecimal getRowPrice(){
		MProductPrice price = new Query(Env.getCtx(),MProductPrice.Table_Name,MProductPrice.COLUMNNAME_M_Product_ID+"=? AND "
				+MProductPrice.COLUMNNAME_M_PriceList_Version_ID+"=?",null)
		.setParameters(productBOMLine.getM_Product_ID(), PriceListVersion)
		.first();
		if (price==null) {
			this.priceStdAmt = Env.ZERO;
			return Env.ZERO;
		}			
		BigDecimal priceStd = price.getPriceStd();
		this.priceStdAmt = priceStd;
		return priceStd;
	}
	
	public BigDecimal calculateRowTotalPrice(BigDecimal qty){ 
		priceTotalAmt = priceStdAmt.multiply(qty);
		return priceTotalAmt;
	}
	
	public BigDecimal getPriceStdAmt(){
		return priceStdAmt;
	}
	
	public BigDecimal getTotalPrice(){
		return priceTotalAmt;
	}
}
