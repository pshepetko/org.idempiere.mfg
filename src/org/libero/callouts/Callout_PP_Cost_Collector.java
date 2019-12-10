package org.libero.callouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab; 

public class Callout_PP_Cost_Collector extends CalloutCostCollector implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("PP_Order_ID"))  
			return order(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("PP_Order_Node_ID"))
			return node(ctx, WindowNo, mTab, mField,value);
		if (mField.getColumnName().equals("MovementQty"))
			return duration(ctx, WindowNo, mTab, mField,value);
		return null;
	}

}
