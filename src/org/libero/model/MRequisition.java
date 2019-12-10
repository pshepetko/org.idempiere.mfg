/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, Sysnova Bangladesh and other contributors 
*/
package org.libero.model;
 
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MBPartner; 
import org.compiere.model.MRequisitionLine; 
import org.compiere.model.Query;
import org.compiere.model.X_C_BP_Group;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.model.MStorageReservation;

/**
 * Requisition Model class for MRP.
	check MStorageReservation.getQtyAvailable and subtract from for final Required Qty.
 * @author red1
 *
 */
public class MRequisition extends org.compiere.model.MRequisition{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8844916612112952581L;
	private int C_BPartner_ID;
	private int M_Product_ID;
	private BigDecimal QtyPlanned; 
	private int M_RequisitionLine_ID;
	
	public MRequisition(Properties ctx, int M_Requisition_ID, String trxName) {
		super(ctx, M_Requisition_ID, trxName);
		 
	}
	
	private void setPriceList(int C_BPartner_ID){
		this.C_BPartner_ID = C_BPartner_ID;
		// Get PriceList from BPartner/Group - teo_sarca, FR [ 2829476 ]
		final String sql = "SELECT COALESCE(bp."+MBPartner.COLUMNNAME_PO_PriceList_ID
				+",bpg."+X_C_BP_Group.COLUMNNAME_PO_PriceList_ID+")"
				+" FROM C_BPartner bp"
				+" INNER JOIN C_BP_Group bpg ON (bpg.C_BP_Group_ID=bp.C_BP_Group_ID)"
				+" WHERE bp.C_BPartner_ID=?";
		int M_PriceList_ID = DB.getSQLValueEx(get_TrxName(), sql, C_BPartner_ID);
		if (M_PriceList_ID > 0)
			setM_PriceList_ID(M_PriceList_ID);		
	}
	
	public void create(int PP_MRP_ID, BigDecimal QtyPlanned, int M_Product_ID, int C_BPartner, int AD_Org_ID, int AD_User_ID, Timestamp DateRequired, String description, int M_Warehouse_ID, int C_DocType_ID){
		this.QtyPlanned = QtyPlanned;
		this.M_Product_ID = M_Product_ID;
 		BigDecimal available = MStorageReservation.getQtyAvailable(M_Warehouse_ID,M_Product_ID, 0, get_TrxName());
		if (QtyPlanned.compareTo(available)<1)
			return;
		QtyPlanned = QtyPlanned.subtract(available);
		setPriceList(C_BPartner);
		setAD_Org_ID(AD_Org_ID);
		setAD_User_ID(AD_User_ID);
		setDateRequired(DateRequired);
		setDescription(description);
		setM_Warehouse_ID(M_Warehouse_ID);
		setC_DocType_ID(C_DocType_ID);
		saveEx(get_TrxName());
		createLine();
	}

	private void createLine(){
		MRequisitionLine reqline = new  MRequisitionLine(this);
		reqline.setLine(10);
		reqline.setAD_Org_ID(getAD_Org_ID());
		reqline.setC_BPartner_ID(C_BPartner_ID);
		reqline.setM_Product_ID(M_Product_ID);
		reqline.setPrice();
		reqline.setPriceActual(Env.ZERO);
		reqline.setQty(QtyPlanned);
		reqline.saveEx(get_TrxName());
		M_RequisitionLine_ID = reqline.get_ID();
	}
	
	/**
	 * was original code to do after createLine(), but not understood its usage.
	 */
	private void setCorrectDates(){
		// Set Correct Dates for Plan
		final String whereClause = MPPMRP.COLUMNNAME_M_Requisition_ID+"=?";
		List<MPPMRP> mrpList = new Query(getCtx(), MPPMRP.Table_Name, whereClause, get_TrxName())
									.setParameters(new Object[]{getM_Requisition_ID()})
									.list();
		for (MPPMRP mrp : mrpList)
		{
			mrp.setDatePromised(getDateRequired());  
			mrp.setM_RequisitionLine_ID(M_RequisitionLine_ID);
			mrp.saveEx(get_TrxName());
		} 
	}
}
