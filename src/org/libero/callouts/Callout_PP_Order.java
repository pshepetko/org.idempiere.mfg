package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab; 

public class Callout_PP_Order extends CalloutOrder implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("QtyEntered")) {
			qty(ctx, WindowNo, mTab, mField,value);
			return qtyBatch(ctx, WindowNo, mTab, mField,value);
		}
		if (mField.getColumnName().equals("M_Product_ID"))
			return product(ctx, WindowNo, mTab, mField,value);
		return null;
	}

}
