/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *****************************************************************************/

package org.libero.model.reasoner;

import java.math.BigDecimal;

import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MStorageReservation;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderWorkflow;
import org.libero.model.wrapper.BOMLineWrapper;

/**
 * @author Gunther Hoppe, tranSIT GmbH Ilmenau/Germany
 * @version 1.0, October 14th 2005
 */
public class StorageReasoner {

	public StorageReasoner() {
	}

	public MPPOrderWorkflow getPPOrderWorkflow(MPPOrder order) {
		
		int[] ids = PO.getAllIDs("PP_Order_Workflow", "PP_Order_ID = "+order.get_ID(), null);
		if(ids.length != 1) {
			
			return null;
		}
		
		return new MPPOrderWorkflow(Env.getCtx(), ids[0], null);
	}
	
	public boolean equalAttributeInstanceValue(MAttributeInstance ai1, MAttributeInstance ai2) {
		
		if(ai1.getM_Attribute_ID() != ai2.getM_Attribute_ID()) {
			
			return false;
		}
		
		boolean equal = true;


		MAttribute a = new MAttribute(Env.getCtx(), ai1.getM_Attribute_ID(), null);
		if(MAttribute.ATTRIBUTEVALUETYPE_Number.equals(a.getAttributeValueType())) {
			
			if(ai1.getValue() == null) {
				
				equal = ai2.getValueNumber() == null;
			}
			else {
				
				equal = ai1.getValueNumber().compareTo(ai2.getValueNumber()) == 0;
			}
		}
		else if(MAttribute.ATTRIBUTEVALUETYPE_StringMax40.equals(a.getAttributeValueType())) {
			
			if(ai1.getValue() == null) {
				
				equal = ai2.getValue() == null;
			}
			else {
				
				equal = ai1.getValue().equals(ai2.getValue());
			}
		}
		else if(MAttribute.ATTRIBUTEVALUETYPE_List.equals(a.getAttributeValueType())) {

			equal = ai1.getM_AttributeValue_ID() == ai2.getM_AttributeValue_ID();
		}

		return equal;
	}
	
	public int[] getAttributeIDs(MAttributeSetInstance asi) {
		
		MAttributeSet as = new MAttributeSet(Env.getCtx(), asi.getM_AttributeSet_ID(), null);
		return getPOIDs(MAttribute.Table_Name, "M_Attribute_ID IN (SELECT M_Attribute_ID FROM M_AttributeUse WHERE M_AttributeSet_ID = "+as.get_ID()+")", null);
	}
	
	public BigDecimal getSumQtyAvailable(MProduct p, MAttributeSetInstance asi) {
		
		int[] ids = getPOIDs(MLocator.Table_Name, null, null);

		MStorageOnHand storage = null;
		 
		BigDecimal sumQtyAvailable = BigDecimal.ZERO;
		
	 
		for(int i = 0; i < ids.length; i++) {
			
			storage = MStorageOnHand.get(Env.getCtx(), ids[i], p.get_ID(), asi.get_ID(), null);
			if(storage == null) {
				
				continue;
			}
			 
			BigDecimal available = (MStorageReservation.getQtyAvailable(p.get_ID(), storage.getM_Warehouse_ID(), asi.get_ID(), null));		
			sumQtyAvailable = sumQtyAvailable.add(available);
		}
	
		return sumQtyAvailable;
	}
	
	public BigDecimal getSumQtyRequired(BOMLineWrapper line) {
	
		MProduct p = new MProduct(Env.getCtx(), line.getM_Product_ID(), null);
		MAttributeSetInstance asi = new MAttributeSetInstance(Env.getCtx(), line.getM_AttributeSetInstance_ID(), null);
		
		return getSumQtyAvailable(p, asi).subtract(line.getQtyBOM()).negate();
	}
	
	public BigDecimal getAvailableQtyLocator(int prodID, String trxName) {	
		BigDecimal qtyOnHand = getQtyOnHand(prodID, trxName);
		BigDecimal qtyReserved = getQtyReserved(prodID, trxName);		
		return qtyOnHand.subtract(qtyReserved);
	}

	public BigDecimal getQtyOnHand(int prodID, String trxName) {
		BigDecimal qtyOnHand = BigDecimal.ZERO;
		MStorageOnHand[] storages = MStorageOnHand.getOfProduct(Env.getCtx(), prodID, trxName);		
		for(MStorageOnHand storage:storages) {	
			if(storage != null) { 	
				qtyOnHand = qtyOnHand.add(storage.getQtyOnHand());
  			}
		}
		return qtyOnHand;
	}
	
	public BigDecimal getQtyReserved(int prodID, String trxName) {
		BigDecimal qtyReserved = BigDecimal.ZERO;		
		MStorageReservation[] reserves = MStorageReservation.getOfProduct(Env.getCtx(), prodID, trxName);
		for (MStorageReservation reserve:reserves){
				qtyReserved = qtyReserved.add(reserve.getQty());
			}
		return qtyReserved;
	}
	
	public boolean isQtyAvailable(BOMLineWrapper line) {
		
		MProduct p = new MProduct(Env.getCtx(), line.getM_Product_ID(), null);
		MAttributeSetInstance asi = new MAttributeSetInstance(Env.getCtx(), line.getM_AttributeSetInstance_ID(), null);
		
		return isQtyAvailable(p, asi);
	}

	public boolean isQtyAvailable(MProduct p, MAttributeSetInstance asi) {
			
		int[] ids = getPOIDs(MLocator.Table_Name, null, null);

		MStorageOnHand storage = null;
		
		BigDecimal sumQtyOnHand = BigDecimal.ZERO;
		BigDecimal sumQtyReserved = BigDecimal.ZERO;
		
		int count = 0;
		for(int i = 0; i < ids.length; i++) {
			
			storage = MStorageOnHand.get(Env.getCtx(), ids[i], p.get_ID(), asi.get_ID(), null);
			if(storage == null) {
				
				continue;
			}
			BigDecimal available = (MStorageReservation.getQtyAvailable(p.get_ID(), storage.getM_Warehouse_ID(), asi.get_ID(),  null));
			BigDecimal reserved = (storage.getQtyOnHand().subtract(available));		
			sumQtyOnHand = sumQtyOnHand.add(storage.getQtyOnHand());
			sumQtyReserved = sumQtyReserved.add(reserved);
		}

		double available = sumQtyOnHand.subtract(sumQtyReserved).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		if(count == 0 || (available <= 0.00d)) {
			
			return false;
		}
		
		return true;
	}
	
	public int[] getPOIDs(String locator, String where, String trx) {
		
		String client = "AD_Client_ID = "+Env.getAD_Client_ID(Env.getCtx());
		String w = null;
		if(where == null || where.length() == 0) {
			
			w = client;
		}
		else {
			
			w = where+" AND "+client;
		}
		return PO.getAllIDs(locator, w, trx);
	}
}
