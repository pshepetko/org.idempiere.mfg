/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, iDempiere contributors
*/
package org.libero.infowindow;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.compiere.model.MStorageOnHand;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.libero.model.MPPMRP;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;

/**
 * @author red1
 * Converted from the original org.libero.form.OrderReceiptIssue /WOrderReceiptIssue form)
 * As a second process in the MRP InfoWindow.
 * Will take the MRP base record and traverse via associated PP_Order to get to the PPOrderBOMLines
 * association is from M_Product_ID
 *
 */

public class OrderReceiptIssue extends SvrProcess {

	private String p_DeliveryRule;
	private boolean p_BackFlushGroup;
	private Timestamp p_MovementDate;
	private boolean firsttime = true;
	private int PP_Order_ID;
	private Timestamp minGuaranteeDate;
	private Timestamp movementDate;
	private BigDecimal qtyToDeliver;
	private BigDecimal qtyScrapComponent;

	@Override
	protected void prepare() { 
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("DeliveryRule"))
			{    
				p_DeliveryRule = para[i].getParameterAsString();
			}   
			else if (name.equals("BackFlushGroup"))
			{    
				p_BackFlushGroup = para[i].getParameterAsBoolean();
			}  
			else if (name.equals("MovementDate"))
			{    
				p_MovementDate = para[i].getParameterAsTimestamp();
			}
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE  T_Selection.AD_PInstance_ID=? " +
				"AND T_Selection.T_Selection_ID=PP_MRP.PP_MRP_ID)";		
		List<MPPMRP>mrpset = new Query(Env.getCtx(), MPPMRP.Table_Name, whereClause, get_TrxName())
		.setParameters(new Object[]{getAD_PInstance_ID()})
		.list();
		
		//MRP record as selected in InfoWindow
		//obtain PP_Order as base. Relate each MRP to OrderBOMLine
		for (MPPMRP mrp:mrpset){
			if (firsttime){
				//get PPOrder
				PP_Order_ID = mrp.getPP_Order_ID();
				firsttime = false;
			}
			//get associated OrderBOMLine
			MPPOrderBOMLine bomline = new Query(Env.getCtx(),MPPOrderBOMLine.Table_Name,
					MPPOrderBOMLine.COLUMNNAME_PP_Order_ID+"=?",get_TrxName())
			.setParameters(mrp.getPP_Order_ID()).first();
			if (p_DeliveryRule.equals("BackFlush") || p_DeliveryRule.equals("OnlyIssue"))
				createIssue(bomline);
			if (p_DeliveryRule.equals("BackFlush") || p_DeliveryRule.equals("OnlyReceipt")) 
			{
				createReceipt(bomline);
			}
		}
		return null;
	}

	private void createReceipt(MPPOrderBOMLine bomline) {
		MPPOrder mo = new MPPOrder(Env.getCtx(),bomline.getPP_Order_ID(), bomline.get_TrxName());
	/*
	 *  Embargo until InfoWindow can accept row level data input
	 * 	MPPOrder.createReceipt(mo,
	 
				bomline.getMovementDate(),
				getDeliveredQty(),
				bomline.getToDeliverQty(), 
				bomline.getScrap(),
				getRejectQty(),
				bomline.getM_Locator_ID(),
				bomline.getM_AttributeSetInstance_ID()
		);*/
		
	}

	private void createIssue(MPPOrderBOMLine bomline) { 
		MPPOrder mo = new MPPOrder(Env.getCtx(),bomline.getPP_Order_ID(), bomline.get_TrxName());
		int M_Product_ID = bomline.getM_Product_ID();
		int PP_Order_BOMLine_ID = bomline.getPP_Order_BOMLine_ID();
		int M_AttributeSetInstance_ID = bomline.getM_AttributeSetInstance_ID();
		MStorageOnHand[] storages = MPPOrder.getStorages(Env.getCtx(),
				M_Product_ID, mo.getM_Warehouse_ID(),
				M_AttributeSetInstance_ID, minGuaranteeDate,
				bomline.get_TrxName());

		MPPOrder.createIssue(mo, PP_Order_BOMLine_ID, movementDate,
				qtyToDeliver, qtyScrapComponent, Env.ZERO, storages,
				false);
	}

	
}
