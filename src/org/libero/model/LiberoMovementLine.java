package org.libero.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query; 
import org.eevolution.model.MDDOrderLine;

public class LiberoMovementLine extends MMovementLine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LiberoMovementLine(MMovement parent) {
		super(parent); 
	}
	public LiberoMovementLine(Properties ctx, int record_ID, String trxName) {
		super(ctx, record_ID, trxName);
	}
	/** 
	 *      Set Distribution Order Line. 
	 *      Does not set Quantity! 
	 *      @param oLine order line 
	 *      @param M_Locator_ID locator 
	 *      @param Qty used only to find suitable locator 
	 */ 
	public void setOrderLine (MDDOrderLine oLine, BigDecimal Qty, boolean isReceipt) 
	{ 
		setDD_OrderLine_ID(oLine.getDD_OrderLine_ID()); 
		setLine(oLine.getLine()); 
		//setC_UOM_ID(oLine.getC_UOM_ID()); 
		MProduct product = oLine.getProduct(); 
		if (product == null) 
		{ 
			set_ValueNoCheck(COLUMNNAME_M_Product_ID, null); 
			set_ValueNoCheck(COLUMNNAME_M_AttributeSetInstance_ID, null); 
			set_ValueNoCheck(COLUMNNAME_M_AttributeSetInstanceTo_ID, null); 
			set_ValueNoCheck(COLUMNNAME_M_Locator_ID, null); 
			set_ValueNoCheck(COLUMNNAME_M_LocatorTo_ID, null); 
		} 
		else 
		{ 
			setM_Product_ID(oLine.getM_Product_ID()); 
			setM_AttributeSetInstance_ID(oLine.getM_AttributeSetInstance_ID()); 
			setM_AttributeSetInstanceTo_ID(oLine.getM_AttributeSetInstanceTo_ID()); 
			// 
			if (product.isItem()) 
			{ 
				MWarehouse w = MWarehouse.get(getCtx(), oLine.getParent().getM_Warehouse_ID());
				MLocator locator_inTransit = MLocator.getDefault(w);
				if(locator_inTransit == null)
				{
					throw new AdempiereException("Do not exist Locator for the  Warehouse in transit");
				}
				
				if (isReceipt)
				{
					setM_Locator_ID(locator_inTransit.getM_Locator_ID()); 
					setM_LocatorTo_ID(oLine.getM_LocatorTo_ID()); 
				}
				else 
				{
					setM_Locator_ID(oLine.getM_Locator_ID()); 
					setM_LocatorTo_ID(locator_inTransit.getM_Locator_ID()); 
				}
			} 
			else 
			{	
				set_ValueNoCheck(COLUMNNAME_M_Locator_ID, null); 
				set_ValueNoCheck(COLUMNNAME_M_LocatorTo_ID, null); 
			}	
		} 
	
		setDescription(oLine.getDescription()); 
		this.setMovementQty(Qty);
	}       //      setOrderLine 
	
	/** 
	 *  Get Movement lines Of Distribution Order Line 
	 *  @param ctx context 
	 *  @param DD_OrderLine_ID line 
	 *  @param where optional addition where clause 
	 *  @param trxName transaction 
	 *      @return array of receipt lines 
	 */ 
	public static LiberoMovementLine[] getOfOrderLine (Properties ctx, 
			int DD_OrderLine_ID, String where, String trxName) 
	{
		String whereClause = COLUMNNAME_DD_OrderLine_ID+"=?"; 
		if (where != null && where.length() > 0) 
			whereClause += " AND (" + where + ")";
		//
		List<MMovementLine> list = new Query(ctx, Table_Name, whereClause, trxName)
										.setParameters(DD_OrderLine_ID)
										.list();
		return list.toArray(new LiberoMovementLine[list.size()]);
	}       //      getOfOrderLine 
}
