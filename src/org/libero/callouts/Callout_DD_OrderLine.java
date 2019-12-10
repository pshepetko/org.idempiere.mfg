package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab; 

public class Callout_DD_OrderLine extends CalloutDistributionOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("QtyEntered"))
			return qty(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("M_Product_ID"))
			return setLocatorTo(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("ConfirmedQty"))
			return qtyConfirmed(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("C_UOM_ID"))
			return qty(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("M_AtttributeSetInstanceTo_ID"))
			return qtyConfirmed(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("M_AtttributeSetInstance_ID"))
			return qty(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("QtyOrdered"))
			return qty(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("AD_Org_ID")) // for DD_Order table
			return bPartner(ctx, WindowNo, mTab, mField,value);
		return null;
	}

}
