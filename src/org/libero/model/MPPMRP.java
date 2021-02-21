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
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/
package org.libero.model;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;		
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MDocType;
import org.compiere.model.MForecastLine;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_M_Forecast;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.I_PP_Product_Planning;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductPlanning;
import org.libero.exceptions.NoPlantForWarehouseException;
import org.libero.tables.X_PP_MRP;


/**
 * PP MRP
 *	
 * @author Victor Perez www.e-evolution.com     
 * @author Teo Sarca, www.arhipac.ro
 */
public class MPPMRP extends X_PP_MRP implements DocAction
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6831223361306903297L;
	
	private static CLogger s_log = CLogger.getCLogger(MPPMRP.class);
	public static int C_Order_ID=0;
	public static int C_OrderLine_ID=0; //arbitrary use from original Sales Order MRP during CalculateMaterialPlan
	
	public static int createMOMakeTo(MPPMRP mrp,MOrderLine ol, BigDecimal qty)
	{
		//Remark by Ferry, moveto after checking MPPProductBOM.BOMTYPE_CurrentActive
		//MPPOrder order = MPPOrder.forC_OrderLine_ID(ol.getCtx(), ol.get_ID(), ol.get_TrxName());
		
		//-->Ferry, checking BOM & Workflow
		final MProduct product = MProduct.get(ol.getCtx(), ol.getM_Product_ID());
		
		// if this product have no BOM defined, do not need to create manufacturing plan/order, added by <stevenyanzhi@gmail.com>
        if (!product.isBOM())
            return 0;
        //<stevenyanzhi@gmail.com>
        
		final String whereClause = MPPProductBOM.COLUMNNAME_BOMType+" IN (?,?)"
					   +" AND "+MPPProductBOM.COLUMNNAME_BOMUse+"=?"
					   +" AND "+MPPProductBOM.COLUMNNAME_Value+"=?";
		
		//Search standard BOM
		MPPProductBOM bom = new Query(ol.getCtx(), MPPProductBOM.Table_Name, whereClause,ol.get_TrxName())
					.setClient_ID()
					.setParameters(new Object[]{
							MPPProductBOM.BOMTYPE_Make_To_Order, 
							MPPProductBOM.BOMTYPE_Make_To_Kit, 
							MPPProductBOM.BOMUSE_Manufacturing,
							product.getValue()})
					.firstOnly();
		
		//Search workflow standard
		MWorkflow workflow = null;
		int workflow_id =  MWorkflow.getWorkflowSearchKey(product);
		if(workflow_id > 0)
			workflow = MWorkflow.get(ol.getCtx(), workflow_id);
		
		//Search Plant for this Warehouse - 
//red1 raised the getPlantForWareHouse as later stops setC_OrderLine in PP_Order (see http://red1.org/adempiere/viewtopic.php?f=45&t=1775&p=8471#p8471)
		int plant_id = MPPProductPlanning.getPlantForWarehouse(ol.getM_Warehouse_ID());
		
		MPPProductPlanning pp = null;
		//Search planning data if no exist BOM or Workflow Standard
		if(bom == null || workflow == null)
		{
			if(plant_id <= 0)
			{
				throw new NoPlantForWarehouseException(ol.getM_Warehouse_ID());
			}
			
			pp = MPPProductPlanning.find(ol.getCtx(), ol.getAD_Org_ID(), ol.getM_Warehouse_ID() , plant_id , ol.getM_Product_ID(), ol.get_TrxName()); 	
			if(pp == null)
				throw new AdempiereException("@NotFound@ @PP_Product_Planning_ID@");
		}
		//Validate BOM
		if(bom == null && pp != null)
		{
				bom = new MPPProductBOM(ol.getCtx(), pp.getPP_Product_BOM_ID(), ol.get_TrxName());
				if( bom != null
					&& !MPPProductBOM.BOMTYPE_Make_To_Order.equals(bom.getBOMType())
					&& !MPPProductBOM.BOMTYPE_Make_To_Kit.equals(bom.getBOMType()) )
				{
					/*FERRY 12.12.2015
					* If BOM Type is current active not need to create MO */
					if ( MPPProductBOM.BOMTYPE_CurrentActive.equals(bom.getBOMType()) )
						  return pp.getS_Resource_ID();//FERRY --end --red1-- returns Resource_ID to PP_MRP record.
					throw new AdempiereException("@NotFound@ @PP_ProductBOM_ID@");
				}
		}
		
		//Ferry, except MPPProductBOM.BOMTYPE_CurrentActive
		MPPOrder order = MPPOrder.forC_OrderLine_ID(ol.getCtx(), ol.get_ID(), ol.get_TrxName());
		
		if (workflow == null && pp != null) 
		{		
			//Validate the workflow based in planning data 						
			workflow = new MWorkflow( ol.getCtx() , pp.getAD_Workflow_ID(), ol.get_TrxName());
			
			if(workflow == null)
			{
				throw new AdempiereException("@NotFound@ @AD_Workflow_ID@");
			}
						
		} 
		//<--Ferry, checking BOM & Workflow
		
		if (order == null)
		{
			//-->Ferry, remark cause already checking at above
			/*final MProduct product = MProduct.get(ol.getCtx(), ol.getM_Product_ID());
			
			// if this product have no BOM defined, do not need to create manufacturing plan/order, added by <stevenyanzhi@gmail.com>
            if (!product.isBOM())
                return 0;
            //<stevenyanzhi@gmail.com>
            
			final String whereClause = MPPProductBOM.COLUMNNAME_BOMType+" IN (?,?)"
						   +" AND "+MPPProductBOM.COLUMNNAME_BOMUse+"=?"
						   +" AND "+MPPProductBOM.COLUMNNAME_Value+"=?";
			
			//Search standard BOM
			MPPProductBOM bom = new Query(ol.getCtx(), MPPProductBOM.Table_Name, whereClause,ol.get_TrxName())
						.setClient_ID()
						.setParameters(new Object[]{
								MPPProductBOM.BOMTYPE_Make_To_Order, 
								MPPProductBOM.BOMTYPE_Make_To_Kit, 
								MPPProductBOM.BOMUSE_Manufacturing,
								product.getValue()})
						.firstOnly();
			
			//Search workflow standard
			MWorkflow workflow = null;
			int workflow_id =  MWorkflow.getWorkflowSearchKey(product);
			if(workflow_id > 0)
				workflow = MWorkflow.get(ol.getCtx(), workflow_id);
			
			//Search Plant for this Warehouse - 
//red1 raised the getPlantForWareHouse as later stops setC_OrderLine in PP_Order (see http://red1.org/adempiere/viewtopic.php?f=45&t=1775&p=8471#p8471)
			int plant_id = MPPProductPlanning.getPlantForWarehouse(ol.getM_Warehouse_ID());
			
			MPPProductPlanning pp = null;
			//Search planning data if no exist BOM or Workflow Standard
			if(bom == null || workflow == null)
			{
				if(plant_id <= 0)
				{
					throw new NoPlantForWarehouseException(ol.getM_Warehouse_ID());
				}
				
				pp = MPPProductPlanning.find(ol.getCtx(), ol.getAD_Org_ID(), ol.getM_Warehouse_ID() , plant_id , ol.getM_Product_ID(), ol.get_TrxName()); 	
				if(pp == null)
					throw new AdempiereException("@NotFound@ @PP_Product_Planning_ID@");
			}
			
			//Validate BOM
			if(bom == null && pp != null)
			{
					bom = new MPPProductBOM(ol.getCtx(), pp.getPP_Product_BOM_ID(), ol.get_TrxName());
					if( bom != null
						&& !MPPProductBOM.BOMTYPE_Make_To_Order.equals(bom.getBOMType())
						&& !MPPProductBOM.BOMTYPE_Make_To_Kit.equals(bom.getBOMType()) )
					{
						//FERRY 12.12.2015
						//* If BOM Type is current active not need to create MO 
						if ( MPPProductBOM.BOMTYPE_CurrentActive.equals(bom.getBOMType()) )
							  return pp.getS_Resource_ID();//FERRY --end --red1-- returns Resource_ID to PP_MRP record.
						throw new AdempiereException("@NotFound@ @PP_ProductBOM_ID@");
					}
			}
			
			if (workflow == null && pp != null) 
			{		
				//Validate the workflow based in planning data 						
				workflow = new MWorkflow( ol.getCtx() , pp.getAD_Workflow_ID(), ol.get_TrxName());
				
				if(workflow == null)
				{
					throw new AdempiereException("@NotFound@ @AD_Workflow_ID@");
				}
				
				
			} 
			*/
			//<--Ferry
			
			if (plant_id > 0 && workflow != null)
			{
				String description = Msg.translate(ol.getCtx(),MRefList.getListName(ol.getCtx(), MPPOrderBOM.BOMTYPE_AD_Reference_ID, bom.getBOMType())) 
				+ " "
				+ Msg.translate(ol.getCtx(), MOrder.COLUMNNAME_C_Order_ID) 
				+ " : "
				+ ol.getParent().getDocumentNo();					
				// Create temporary data planning to create Manufacturing Order
				pp = new MPPProductPlanning(ol.getCtx(), 0 , ol.get_TrxName());
				pp.setAD_Org_ID(ol.getAD_Org_ID());
				pp.setM_Product_ID(product.getM_Product_ID());
				pp.setPlanner_ID(ol.getParent().getSalesRep_ID());
				pp.setPP_Product_BOM_ID(bom.getPP_Product_BOM_ID());
				pp.setAD_Workflow_ID(workflow.getAD_Workflow_ID());
				pp.setM_Warehouse_ID(ol.getM_Warehouse_ID());
				pp.setS_Resource_ID(plant_id);
				
				order = MPPMRP.createMO(mrp,pp, ol.getC_OrderLine_ID(),ol.getM_AttributeSetInstance_ID(), 
										qty, ol.getDateOrdered(), ol.getDatePromised(), description);
				
				description = "";
				if(ol.getDescription() != null)
					description = ol.getDescription();
				
				description = description + " " + Msg.translate(ol.getCtx(),MRefList.getListName(ol.getCtx(), MPPOrderBOM.BOMTYPE_AD_Reference_ID, bom.getBOMType())) 
							+ " "
							+ Msg.translate(ol.getCtx(), MPPOrder.COLUMNNAME_PP_Order_ID) 
							+ " : "
							+ order.getDocumentNo();
				
				ol.setDescription(description);
				ol.saveEx();
			}
		}
		else
		{    
			//-->Ferry
			boolean isNoEdit = false;
			isNoEdit = order.DOCSTATUS_Completed.equals(order.getDocStatus())
					|| order.DOCSTATUS_Closed.equals(order.getDocStatus());
			//<--Ferry
			
			if (!order.isProcessed() && !isNoEdit) //Ferry if (!order.isProcessed())
			{
				//if you change product in order line the Manufacturing order is void
				if(order.getM_Product_ID() != ol.getM_Product_ID())
				{
					order.setDescription("");
					order.setQtyEntered(Env.ZERO);
					order.setC_OrderLine_ID(0);
					order.voidIt();
					order.setDocStatus(MPPOrder.DOCSTATUS_Voided);
					order.setDocAction(MPPOrder.ACTION_None);
					order.save();
					ol.setDescription("");
					ol.saveEx();
					
				}
				if(order.getQtyEntered().compareTo(ol.getQtyEntered()) != 0)
				{	
					order.setQty(ol.getQtyEntered());
					order.saveEx();
				}	
				if(order.getDatePromised().compareTo(ol.getDatePromised()) != 0)
				{
					order.setDatePromised(ol.getDatePromised());
					order.saveEx();
				}
			}    
		}    
	return 0;
	}
	
	/**
	 * Create Manufacturing Order base on Planning Data
	 * @param pp Product Planning 
	 * @param C_OrderLine_ID Sales Order Line
	 * @param M_AttributeSetInstance_ID ASI
	 * @param qty Quantity 
	 * @param dateOrdered Data Ordered
	 * @param datePromised Data Promised
	 * @param description Order Description
	 * @return Manufacturing Order or null
	 */
	public static MPPOrder createMO(MPPMRP mrp,MPPProductPlanning pp,int C_OrderLine_ID,int M_AttributeSetInstance_ID , BigDecimal qty, 
									Timestamp dateOrdered,Timestamp datePromised,String description)
	{
	
		MPPProductBOM bom = pp.getPP_Product_BOM();
		MWorkflow wf = pp.getAD_Workflow();
		
		if (pp.getS_Resource_ID() > 0 && bom != null && wf != null)
		{
			RoutingService routingService = RoutingServiceFactory.get().getRoutingService(pp.getCtx());
			
			MPPOrder order = new MPPOrder(pp.getCtx(), 0 , pp.get_TrxName());
			order.setAD_Org_ID(pp.getAD_Org_ID());
			order.setDescription(description);
			order.setC_OrderLine_ID(C_OrderLine_ID);
			order.setS_Resource_ID(pp.getS_Resource_ID());
			order.setM_Warehouse_ID(pp.getM_Warehouse_ID());
			order.setM_Product_ID(pp.getM_Product_ID());
			order.setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
			order.setPP_Product_BOM_ID(pp.getPP_Product_BOM_ID());
			order.setAD_Workflow_ID(pp.getAD_Workflow_ID());
			order.setPlanner_ID(pp.getPlanner_ID());
			order.setLine(10);
			order.setDateOrdered(dateOrdered);                       
			order.setDatePromised(datePromised);
			
			//red1 passing MO to WFNode resource assign within to keep track
			int duration = routingService.calculateDuration(mrp, wf,MResource.get(pp.getCtx(), pp.getS_Resource_ID()), qty, datePromised).intValueExact();		//Ferry routingService.calculateDuration(mrp, wf,MResource.get(pp.getCtx(), pp.getS_Resource_ID()),qty).intValueExact(); 
			//
			Timestamp startTime = routingService.getStartAssignTime();

			order.setDateStartSchedule(startTime);//TimeUtil.addDays(datePromised, 0 - duration));
			order.setDateFinishSchedule(TimeUtil.addMinutess(startTime, duration)); //Ferry duration in minutes order.setDateFinishSchedule(TimeUtil.addDays(startTime, duration));
			order.setC_UOM_ID(pp.getM_Product().getC_UOM_ID());
			order.setQty(qty);
			order.setPriorityRule(MPPOrder.PRIORITYRULE_High);                                
			order.saveEx();  
			//Ferry prepare it manually order.setDocStatus(order.prepareIt());
			order.setDocAction(MPPOrder.ACTION_Complete);
			order.saveEx(pp.get_TrxName());
			
			//-->Ferry fill reff. PP_ORDER_ID
			if (!materialDemandOfMO(mrp))
				mrp.setPP_Order_ID(order.get_ID());
			
			mrp.setDocStatus(MPPMRP.DOCSTATUS_InProgress);
			//<--Ferry			
			order.saveEx(pp.get_TrxName());
			return order;				
		}    
		return null;
	}
	
	private static HashMap<String, String[]> s_sourceColumnNames = new HashMap<String, String[]>();
	static
	{
		s_sourceColumnNames.put(MOrder.Table_Name, new String[]{
				MOrder.COLUMNNAME_DatePromised,
				MOrder.COLUMNNAME_DocStatus,
		});
		s_sourceColumnNames.put(MOrderLine.Table_Name, new String[]{
				"AD_Org_ID",
				MOrderLine.COLUMNNAME_DateOrdered,
				MOrderLine.COLUMNNAME_DatePromised,
				MOrderLine.COLUMNNAME_C_BPartner_ID,
				MOrderLine.COLUMNNAME_M_Warehouse_ID,
				MOrderLine.COLUMNNAME_M_Product_ID,
				MOrderLine.COLUMNNAME_C_UOM_ID,
				MOrderLine.COLUMNNAME_QtyOrdered,
				MOrderLine.COLUMNNAME_QtyDelivered,
		});
		s_sourceColumnNames.put(MRequisition.Table_Name, new String[]{
				MRequisition.COLUMNNAME_DateRequired,
				MRequisition.COLUMNNAME_M_Warehouse_ID,
				//MRequisition.COLUMNNAME_DocStatus, // not needed
		});
		s_sourceColumnNames.put(MRequisitionLine.Table_Name, new String[]{
				"AD_Org_ID",
				MRequisitionLine.COLUMNNAME_M_Product_ID,
				MRequisitionLine.COLUMNNAME_Qty,
				MRequisitionLine.COLUMNNAME_C_OrderLine_ID, // QtyOrdered depends on that
		});
		s_sourceColumnNames.put(X_M_Forecast.Table_Name, new String[]{
		});
		s_sourceColumnNames.put(MForecastLine.Table_Name, new String[]{
				"AD_Org_ID",
				MForecastLine.COLUMNNAME_DatePromised,
				MForecastLine.COLUMNNAME_M_Warehouse_ID,
				MForecastLine.COLUMNNAME_M_Product_ID,
				MForecastLine.COLUMNNAME_Qty,
		});
		s_sourceColumnNames.put(MDDOrder.Table_Name, new String[]{
				MDDOrder.COLUMNNAME_DocStatus,
				MDDOrder.COLUMNNAME_C_BPartner_ID
		});
		s_sourceColumnNames.put(MDDOrderLine.Table_Name, new String[]{
				"AD_Org_ID",
				MDDOrderLine.COLUMNNAME_M_Product_ID,
				MDDOrderLine.COLUMNNAME_C_UOM_ID,
				MDDOrderLine.COLUMNNAME_DatePromised,
				MDDOrderLine.COLUMNNAME_QtyOrdered,
				MDDOrderLine.COLUMNNAME_QtyDelivered,
				MDDOrderLine.COLUMNNAME_ConfirmedQty,
				MDDOrderLine.COLUMNNAME_M_Locator_ID,
				MDDOrderLine.COLUMNNAME_M_LocatorTo_ID,
				MDDOrderLine.COLUMNNAME_ConfirmedQty,
		});
		s_sourceColumnNames.put(MPPOrder.Table_Name, new String[]{
				"AD_Org_ID",
				MPPOrder.COLUMNNAME_M_Product_ID,
				MPPOrder.COLUMNNAME_C_UOM_ID,
				MPPOrder.COLUMNNAME_DatePromised,
				MPPOrder.COLUMNNAME_QtyOrdered,
				MPPOrder.COLUMNNAME_QtyDelivered,
				MPPOrder.COLUMNNAME_PP_Product_BOM_ID,
				MPPOrder.COLUMNNAME_AD_Workflow_ID,
				MPPOrder.COLUMNNAME_DocStatus,
		});
		s_sourceColumnNames.put(MPPOrderBOMLine.Table_Name, new String[]{
				MPPOrderBOMLine.COLUMNNAME_M_Product_ID,
				MPPOrderBOMLine.COLUMNNAME_C_UOM_ID,
				MPPOrderBOMLine.COLUMNNAME_M_Warehouse_ID,
				MPPOrderBOMLine.COLUMNNAME_QtyEntered,
				MPPOrderBOMLine.COLUMNNAME_QtyDelivered,
		});
	}

	/**
	 * Check if a persistent object is changed, from MRP point of view
	 * @param po MRP relevant PO (e.g. MOrder, MOrderLine, MPPOrder etc)
	 * @return true if object changed
	 */
	public static boolean isChanged(PO po)
	{
		String[] columnNames = s_sourceColumnNames.get(po.get_TableName());
		if (columnNames == null || columnNames.length == 0)
		{
			return false;
		}
		if (po.is_new()
				|| po.is_ValueChanged("IsActive"))
		{
			return true;
		}
		for (String columnName : columnNames)
		{
			if (po.is_ValueChanged(columnName))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @return MRP Source tables
	 */
	public static Collection<String> getSourceTableNames()
	{
		return s_sourceColumnNames.keySet();
	}
	
	public static void deleteMRP(PO po)
	{
		String tableName = po.get_TableName();
		//-->Ferry 
		int no;
		String m_name;
		List<MResourceAssignment> resourceschedule_list;
		if ((po instanceof MPPOrder) || 
		    (po instanceof MOrder && ((MOrder)po).isSOTrx())
		    ) {
						
			//void all draft generated Requisition
			String whereClause = tableName+"_ID=? AND AD_Client_ID=? AND DocStatus=? AND M_Requisition_ID is not null";
			List<MPPMRP>mrpset = new Query(po.getCtx(), Table_Name, whereClause, po.get_TrxName())
					.setParameters(new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.DOCSTATUS_Drafted})
					.list();	
			MRequisition req;
			for (MPPMRP mrp:mrpset){
				req = new  MRequisition(po.getCtx(), mrp.getM_Requisition_ID(), po.get_TrxName());
				if (req !=null && req.getDocStatus().compareTo(MRequisition.DOCSTATUS_Drafted)==0 ) {
					req.setDocStatus(MRequisition.DOCSTATUS_Voided);
					req.voidIt();
				}
			}
			
			//Reset PP_OrderID or C_Order_ID for all MRP SO reff. to MO and clear resource assignment
			mrpset.clear();
			whereClause = tableName+"_ID=? AND AD_Client_ID=? AND OrderType = ?";
			mrpset = new Query(po.getCtx(), Table_Name, whereClause, po.get_TrxName())
					.setParameters(new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.ORDERTYPE_SalesOrder})
					.list();			
			if (mrpset.size() == 0) {
				//MO created manually
				whereClause = whereClause + " AND " + MPPMRP.COLUMNNAME_TypeMRP + "=?";
				mrpset = new Query(po.getCtx(), Table_Name, whereClause, po.get_TrxName())
						.setParameters(new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.ORDERTYPE_ManufacturingOrder, MPPMRP.TYPEMRP_Supply})
						.list();			
			}
				
			for (MPPMRP mrp:mrpset){
				if (po instanceof MPPOrder) {
					mrp.setPP_Order_ID(0);					
				} else if (po instanceof MOrder) {
					mrp.setC_Order_ID(0);
					mrp.setC_OrderLine_ID(0);
				}
				mrp.saveEx(po.get_TrxName());
				
				//Clear resource assignment
				m_name = "MRP:"+mrp.get_ID()+'%';
				resourceschedule_list = new Query(po.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_AD_Client_ID+"=? AND " + MResourceAssignment.COLUMNNAME_Name+" like ?", null)
						.setParameters(new Object[]{mrp.getAD_Client_ID(), m_name})
						.list();
				for (MResourceAssignment ra : resourceschedule_list) {
				  ra.deleteEx(true, po.get_TrxName());
				}
			}
			
		}
		//<--Ferry		
		
		//-->Ferry, remark cause need more detail condition of deleted MRP
		/*int no = DB.executeUpdateEx("DELETE FROM "+Table_Name+" WHERE "+tableName+"_ID=? AND AD_Client_ID=?",
				new Object[]{po.get_ID(), po.getAD_Client_ID()},
				po.get_TrxName());
		s_log.finest("Deleted "+tableName+" #"+no);*/
		//<--Ferry
		
		// Delete generated manufacturing order
		if (po instanceof MOrderLine)
		{
			MOrderLine ol = (MOrderLine)po;
			MPPOrder order = MPPOrder.forC_OrderLine_ID(ol.getCtx(), ol.get_ID(), ol.get_TrxName());
			if (order != null && !order.isProcessed())
			{
				order.deleteEx(true);
			}
		}		
		//-->Ferry, delete all MRP. Ex. MO Voided will deleted MRP of SO  
		no = DB.executeUpdateEx("DELETE FROM "+Table_Name+" WHERE "+tableName+"_ID=? AND AD_Client_ID=? AND ( DocStatus=? OR DocStatus=? )",
				new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.DOCSTATUS_Drafted,MPPMRP.DOCSTATUS_Voided},
				po.get_TrxName());
		s_log.finest("Deleted "+tableName+" #"+no);
		if (po instanceof MOrder) {
			MOrder void_order = (MOrder) po;
			if (void_order.isSOTrx() && void_order.getDocStatus().compareTo(MOrder.DOCSTATUS_Voided)==0) {
				no = DB.executeUpdateEx("DELETE FROM "+Table_Name+" WHERE "+tableName+"_ID=? AND AD_Client_ID=? AND OrderType = ?",
						new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.ORDERTYPE_SalesOrder},
						po.get_TrxName());
				s_log.finest("Deleted "+tableName+" #"+no);				
			}
		} else if (po instanceof MPPOrder) {
			//delete MRP with description ** Voided Ordered Quantity
			no = DB.executeUpdateEx("DELETE FROM "+Table_Name+" WHERE "+tableName+"_ID=? AND AD_Client_ID=? AND DocStatus=? AND Description like ?",
					new Object[]{po.get_ID(), po.getAD_Client_ID(),MPPMRP.DOCSTATUS_InProgress,"** Voided Ordered Quantity%"},
					po.get_TrxName());
			s_log.finest("Deleted "+tableName+" #"+no);			
		}
		//<--Ferry
		
	}
	
	private static Query getQuery(PO po, String typeMRP, String orderType)
	{
		ArrayList<Object> params = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		//
		whereClause.append("AD_Client_ID=?");
		params.add(po.getAD_Client_ID());
		//
		whereClause.append(" AND ").append(po.get_TableName()).append("_ID=?");
		params.add(po.get_ID());
		//
		if (typeMRP != null)
		{
			whereClause.append(" AND ").append(COLUMNNAME_TypeMRP).append("=?");
			params.add(typeMRP);
		}
		//
		if (orderType != null)
		{
			whereClause.append(" AND ").append(COLUMNNAME_OrderType).append("=?");
			params.add(orderType);
		}
		//
		// In case we query for PP_Order, we need to assure that no BOM Lines records are returned.
		// The TypeMRP=D/S filter is not enough since a BOM Line can produce a supply (e.g. co-product)
		if (po instanceof MPPOrder && TYPEMRP_Supply.equals(typeMRP))
		{
			whereClause.append(" AND ").append(COLUMNNAME_PP_Order_BOMLine_ID).append(" IS NULL");
		}
		//
		return new Query(po.getCtx(), Table_Name, whereClause.toString(), po.get_TrxName())
					.setParameters(params);
	}
	
	/**************************************************************************
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param PP_MRP_ID id
	 *	@param trxName Transaction Name
	 */
	public MPPMRP(Properties ctx, int PP_MRP_ID, String trxName)
	{
		super(ctx, PP_MRP_ID,trxName);
		if (PP_MRP_ID == 0)
		{
			setValue("MRP");
			setName("MRP");
			setDateSimulation(new Timestamp (System.currentTimeMillis()));
			//
			// The available flag should be handled by MRP engine. Initial it should be disabled.
			// see : [ 2593359 ] Calculate Material Plan error related to MRP-060 notice
			//		 https://sourceforge.net/tracker/?func=detail&atid=934929&aid=2593359&group_id=176962
			setIsAvailable(false);
			if (C_Order_ID>0){
				setC_Order_ID(C_Order_ID);
				setC_OrderLine_ID(C_OrderLine_ID);
				MPPOrder mo = new Query(Env.getCtx(),MPPOrder.Table_Name, MPPOrder.COLUMNNAME_C_OrderLine_ID+"=?",trxName)
				.setParameters(C_OrderLine_ID).first();
				if (mo!=null)
					setPP_Order_ID(mo.getPP_Order_ID());
			}
			
		}
	}	//	MPPMRP

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MPPMRP(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs , trxName);
	}                
	
	public void setPP_Order(MPPOrder o)
	{
		setPP_Order_ID(o.getPP_Order_ID());
		setOrderType(ORDERTYPE_ManufacturingOrder);
		//
		setName(o.getDocumentNo());
		setDescription(o.getDescription());
		setDatePromised(o.getDatePromised());
		setDateOrdered(o.getDateOrdered());
		setDateStartSchedule(o.getDateStartSchedule());
		setDateFinishSchedule(o.getDateFinishSchedule());
		setS_Resource_ID(o.getS_Resource_ID());
		//-->Ferry, if MO status complete means MRP in progress, MO close means MRP complete
		//setDocStatus(o.getDocStatus());
		if (o.getDocStatus().compareTo(MOrder.DOCSTATUS_Closed) == 0 ) {
			setDocStatus(MPPMRP.DOCSTATUS_Completed);
		} else if (o.getDocStatus().compareTo(MOrder.DOCSTATUS_Completed) == 0 ) {
			setDocStatus(MPPMRP.DOCSTATUS_InProgress);
		} else {
			setDocStatus(o.getDocStatus());
		}
		//<--Ferry	
	}
	
	public void setC_Order(MOrder o)
	{
		setC_Order_ID(o.get_ID());
		setC_BPartner_ID(o.getC_BPartner_ID());
		setDocStatus(o.getDocStatus());
		if (o.isSOTrx())
		{    
			setOrderType(MPPMRP.ORDERTYPE_SalesOrder);
			setTypeMRP(MPPMRP.TYPEMRP_Demand);
		}
		else
		{
			setOrderType(MPPMRP.ORDERTYPE_PurchaseOrder);
			setTypeMRP(MPPMRP.TYPEMRP_Supply);                                 
		}
	}
	
	public void setDD_Order(MDDOrder o)
	{
		setDD_Order_ID(o.get_ID());
		setC_BPartner_ID(o.getC_BPartner_ID());
		setDocStatus(o.getDocStatus());

	}
	public void setM_Requisition(MRequisition r)
	{
		setM_Requisition_ID(r.get_ID());
		setOrderType(MPPMRP.ORDERTYPE_MaterialRequisition);
		setTypeMRP(MPPMRP.TYPEMRP_Supply);
		//
		//setAD_Org_ID(r.getAD_Org_ID());
		setDateOrdered(r.getDateDoc());
		setDatePromised(r.getDateRequired());
		setDateStartSchedule(r.getDateDoc());
		setDateFinishSchedule(r.getDateRequired());
		setM_Warehouse_ID(r.getM_Warehouse_ID());
	}
	
	public void setM_Forecast(X_M_Forecast f)
	{
		setOrderType(MPPMRP.ORDERTYPE_Forecast);
		setTypeMRP(MPPMRP.TYPEMRP_Demand);                         
		setM_Forecast_ID(f.getM_Forecast_ID());
		setDescription(f.getDescription());
	}
	
	/**
	 * @return true if the document is released
	 */
	public boolean isReleased()
	{
		String docStatus = getDocStatus();
		if (docStatus == null)
			return false;
		return DOCSTATUS_InProgress.equals(docStatus)
				|| DOCSTATUS_Completed.equals(docStatus);
	}

	/**
	 * Create MRP record based in Forecast 
	 * @param MForecast Forecast
	 */
	public static void M_Forecast(X_M_Forecast f)
	{
		List<MPPMRP> list = getQuery(f, null, null).list();
		for (MPPMRP mrp : list)
		{
			mrp.setM_Forecast(f);
		}
	}

	/**
	 * Create MRP record based in Forecast Line 
	 * @param MForecastLine Forecast Line
	 */
	public static void M_ForecastLine(MForecastLine fl)
	{
		String trxName = fl.get_TrxName();
		Properties ctx = fl.getCtx();
		
		X_M_Forecast f = new X_M_Forecast(ctx, fl.getM_Forecast_ID(), trxName);
		MPPMRP mrp = getQuery(fl, null, null).firstOnly();
		if (mrp == null)
		{
			mrp = new MPPMRP(ctx, 0, trxName);     
			mrp.setM_ForecastLine_ID(fl.getM_ForecastLine_ID());
		}
		mrp.setM_Forecast(f);
		mrp.setName("MRP");
		mrp.setAD_Org_ID(fl.getAD_Org_ID());
		mrp.setDatePromised(fl.getDatePromised());
		mrp.setDateStartSchedule(fl.getDatePromised());
		mrp.setDateFinishSchedule(fl.getDatePromised());
		mrp.setDateOrdered(fl.getDatePromised());
		mrp.setM_Warehouse_ID(fl.getM_Warehouse_ID());
		mrp.setM_Product_ID(fl.getM_Product_ID());
		mrp.setQty(fl.getQty());  
		mrp.setDocStatus(DocAction.STATUS_InProgress);
		mrp.saveEx(trxName);
	}
	
	/**
	 * Create MRP record based in Order 
	 * @param MOrder
	 */
	public static void C_Order(MOrder o)
	{ 
		MDocType dt = MDocType.get(o.getCtx(), o.getC_DocTypeTarget_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		if(MDocType.DOCSUBTYPESO_StandardOrder.equals(DocSubTypeSO) || !o.isSOTrx())
		{		
			if((o.getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
					|| o.getDocStatus().equals(MOrder.DOCSTATUS_Completed))
					|| !o.isSOTrx())
			{
				for(MOrderLine line : o.getLines())
				{
					C_OrderLine(line);
				}
			}
			
			if (o.is_ValueChanged(MOrder.COLUMNNAME_DocStatus)
					|| o.is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_ID)
				)
			{
				List<MPPMRP> list = getQuery(o, null, null).list();
				for (MPPMRP mrp : list)
				{
					//-->Ferry , MRP of MO and Demand not need affected
					/*mrp.setC_Order(o);
					mrp.setDocStatus(DOCSTATUS_Drafted);// */
					mrp.setC_Order_ID(o.get_ID());
					mrp.setC_BPartner_ID(o.getC_BPartner_ID());
					if ((mrp.getDocStatus().compareTo(MPPMRP.DOCSTATUS_Completed) == 0 || mrp.getDocStatus().compareTo(MPPMRP.DOCSTATUS_Closed) == 0)
							&& (mrp.getQty().compareTo(Env.ZERO) == 0) 
							&& (mrp.getOrderType().compareTo(MPPMRP.ORDERTYPE_SalesOrder) !=0 )) {
						//no update doc status, if MRP complete or close but qty 0 and not order type sales
					} else if(o.getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
							|| o.getDocStatus().equals(MOrder.DOCSTATUS_Completed)) {
						mrp.setDocStatus(MPPMRP.DOCSTATUS_InProgress);
					}
					else	{ 	
						mrp.setDocStatus(DOCSTATUS_Drafted);	
					}				
					//<--Ferry
					mrp.saveEx(o.get_TrxName());
				}
			}
		}	
	}
	
	/**
	 * Create MRP record based in Order Line 
	 * @param MOrderLine
	 */
	public static void C_OrderLine(MOrderLine ol)
	{ 
		//-->Ferry when complete GI, return exception more than one rows 
		//MPPMRP mrp = getQuery(ol, null, null).firstOnly();
		boolean isReleased = false;
		MOrder ord = ((MOrder) ol.getParent());
		MPPMRP mrp;
		isReleased = MOrder.DOCSTATUS_InProgress.equals(ord.getDocStatus()) 
				  || MOrder.DOCSTATUS_Completed.equals(ord.getDocStatus());
		if (ord.isSOTrx() && isReleased) {
			mrp = getQuery(ol, MPPMRP.TYPEMRP_Demand, MPPMRP.ORDERTYPE_SalesOrder).firstOnly();
		}
		else {
			mrp = getQuery(ol, null, null).firstOnly();
		}
		//<--Ferry		
		if(mrp == null)
		{	
			mrp = new MPPMRP(ol.getCtx(), 0, ol.get_TrxName());   
		}
		mrp.setAD_Org_ID(ol.getAD_Org_ID());
		mrp.setC_Order(ol.getParent());
		mrp.setC_OrderLine_ID(ol.getC_OrderLine_ID());
		//-->Ferry, MRP releated MO MTO need SO ID
		//set Sales Order IDs to MRP
		C_Order_ID = ol.getC_Order_ID();
		C_OrderLine_ID = ol.getC_OrderLine_ID();		
		//<--Ferry		
		mrp.setDescription(ol.getDescription());
		mrp.setName("OrderLine");
		mrp.setDatePromised(ol.getDatePromised());
		mrp.setDateOrdered(ol.getDateOrdered());
		mrp.setM_Warehouse_ID(ol.getM_Warehouse_ID());
		mrp.setM_Product_ID(ol.getM_Product_ID());
		mrp.setQty(ol.getQtyOrdered().subtract(ol.getQtyDelivered()));
		mrp.setDocStatus(DOCSTATUS_InProgress); 
		mrp.saveEx(ol.get_TrxName());

		MOrder o = ol.getParent();
		MDocType dt = MDocType.get(o.getCtx(), o.getC_DocTypeTarget_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		
 	if ((MDocType.DOCSUBTYPESO_StandardOrder.equals(DocSubTypeSO)) &&
		  (DB.getSQLValue(ol.get_TrxName(), 
				"SELECT COUNT (PP_Product_BOM_ID) FROM PP_Product_BOM WHERE BOMType IN ('O','K')  AND BOMUse='M' AND M_Product_ID=?",ol.getM_Product_ID())>0))//PShepetko
		{
			int res = MPPMRP.createMOMakeTo(mrp,ol, ol.getQtyOrdered());
			mrp.setS_Resource_ID(res);
			mrp.saveEx(ol.get_TrxName());
		}	
		return;
	}

	//-->Ferry, update MRP SO or PO from MInOutLine
	public static void updateStatusMRPOrder(MInOutLine outline)
	{   		
		String docStatus;
		MPPMRP mrp = null;
		MOrderLine orderLine = new MOrderLine(outline.getCtx(),outline.getC_OrderLine_ID(), outline.get_TrxName());
		MOrder order = new MOrder(outline.getCtx(),orderLine.getC_Order_ID(), outline.get_TrxName());

		if (order != null && orderLine != null)	{
			if (orderLine.getQtyDelivered().compareTo(Env.ZERO) > 0 || 
					order.getDocStatus().compareTo(MOrder.DOCSTATUS_Closed) == 0 )	{
				
				if (order.getDocStatus().compareTo(MOrder.DOCSTATUS_Closed) ==0) {
					docStatus = MPPMRP.DOCSTATUS_Completed;
				} else if (orderLine.getQtyOrdered().compareTo(orderLine.getQtyDelivered()) <= 0 ) {
					docStatus = MPPMRP.DOCSTATUS_Completed;
				} else	{
					docStatus = MPPMRP.DOCSTATUS_InProgress;
				}

				//Update MRP of SO or PO to complete
				if (order.isSOTrx()) {
					mrp = MPPMRP.getQuery(orderLine, MPPMRP.TYPEMRP_Demand, MPPMRP.ORDERTYPE_SalesOrder).firstOnly();
				} else	{
					mrp = MPPMRP.getQuery(orderLine, MPPMRP.TYPEMRP_Supply, MPPMRP.ORDERTYPE_PurchaseOrder).firstOnly();
				}
				
				if (mrp == null) {
					s_log.finest("MRP SO/PO "+order.getDocumentNo()+" not found");
				} else {
					mrp.setDocStatus(docStatus);
					mrp.saveEx(outline.get_TrxName());
				}

			}
		}

	}
	//<--Ferry
	
	/**
	 * Create MRP record based in Manufacturing Order
	 * @param MPPOrder Manufacturing Order
	 */
	public static void PP_Order(MPPOrder o)
	{
		Properties ctx = o.getCtx();
		String trxName = o.get_TrxName();
		//
		// Supply
		MPPMRP mrpSupply = getQuery(o, TYPEMRP_Supply, ORDERTYPE_ManufacturingOrder).firstOnly();
		if(mrpSupply == null)
		{		                    
			mrpSupply = new MPPMRP(ctx, 0, trxName);                                                                                                                 
			mrpSupply.setAD_Org_ID(o.getAD_Org_ID());
			mrpSupply.setTypeMRP(MPPMRP.TYPEMRP_Supply);
		}
		mrpSupply.setPP_Order(o);
		mrpSupply.setM_Product_ID(o.getM_Product_ID());
		mrpSupply.setM_Warehouse_ID(o.getM_Warehouse_ID());
		mrpSupply.setQty(o.getQtyOrdered().subtract(o.getQtyDelivered()));
		//-->Ferry make sure C_ORDERLINE_ID is not PO
		if (C_Order_ID != 0) {
			MOrder order = new MOrder(ctx, C_Order_ID, trxName);
			if ( order == null || !order.isSOTrx()) C_Order_ID  = 0;
		}
		if (C_OrderLine_ID != 0) {
			MOrderLine ol = new MOrderLine(ctx, C_OrderLine_ID, trxName);
			if ( ol == null || !ol.getC_Order().isSOTrx()) C_OrderLine_ID  = 0;
		}
		//<--Ferry
		mrpSupply.setC_Order_ID(C_Order_ID);
		mrpSupply.setC_OrderLine_ID(C_OrderLine_ID);
		mrpSupply.saveEx(trxName);
		//
		// Demand
		List<MPPMRP> mrpDemandList = getQuery(o, TYPEMRP_Demand, ORDERTYPE_ManufacturingOrder).list();
		for (MPPMRP mrpDemand : mrpDemandList)
		{
			mrpDemand.setPP_Order(o);
			mrpDemand.saveEx(trxName);
		}
	}

	/**
	 * Create MRP record based in Manufacturing Order BOM Line
	 * @param MPPOrderBOMLine Order BOM Line
	 */
	public static void PP_Order_BOMLine(MPPOrderBOMLine obl)
	{        	   
		String trxName = obl.get_TrxName();
		Properties ctx = obl.getCtx();
		//
		String typeMRP = MPPMRP.TYPEMRP_Demand;
		BigDecimal qty = obl.getQtyRequired().subtract(obl.getQtyDelivered());
		if (obl.isCoProduct() || obl.isByProduct())
		{
			typeMRP = MPPMRP.TYPEMRP_Supply;
			qty = qty.negate();
		}
		//
		MPPMRP mrp = getQuery(obl, null, ORDERTYPE_ManufacturingOrder).firstOnly();
		if(mrp == null)
		{
			mrp = new MPPMRP(ctx, 0, trxName);                                                                           
			mrp.setPP_Order_BOMLine_ID(obl.getPP_Order_BOMLine_ID());
		}
		//-->Ferry make sure C_ORDERLINE_ID is not PO
		if (C_Order_ID != 0) {
			MOrder order = new MOrder(ctx, C_Order_ID, trxName);
			if ( order == null || !order.isSOTrx()) C_Order_ID  = 0;
		}
		if (C_OrderLine_ID != 0) {
			MOrderLine ol = new MOrderLine(ctx, C_OrderLine_ID, trxName);
			if ( ol == null || !ol.getC_Order().isSOTrx()) C_OrderLine_ID  = 0;
		}
		//<--Ferry		
		mrp.setC_Order_ID(C_Order_ID);
		mrp.setC_OrderLine_ID(C_OrderLine_ID);
		mrp.setAD_Org_ID(obl.getAD_Org_ID());
		mrp.setTypeMRP(typeMRP);
		mrp.setPP_Order(obl.getParent());
		mrp.setM_Warehouse_ID(obl.getM_Warehouse_ID());
		mrp.setM_Product_ID(obl.getM_Product_ID());
		mrp.setQty(qty);
		mrp.saveEx(trxName);
	}

	/**
	 * Create MRP record based in Distribution Order
	 * @param MDDOrder Distribution Order
	 */
	public static void DD_Order(MDDOrder o)
	{   
		if((MDDOrder.DOCSTATUS_InProgress.equals(o.getDocStatus())
		||  MDDOrder.DOCSTATUS_Completed.equals(o.getDocStatus())))
		{
			for(MDDOrderLine line : o.getLines())
			{
				DD_OrderLine(line);
			}
		}
		
		if (o.is_ValueChanged(MDDOrder.COLUMNNAME_DocStatus)
		||  o.is_ValueChanged(MDDOrder.COLUMNNAME_C_BPartner_ID))
		{
			List<MPPMRP> list = getQuery(o, null, null).list();
			for (MPPMRP mrp : list)
			{
				mrp.setDD_Order(o);
				mrp.saveEx(o.get_TrxName());
			}
		}
	} 	

	/**
	 * Create MRP record based in Distribution Order Line
	 * @param MDDOrderLine Distribution Order Line
	 */
	public static void DD_OrderLine(MDDOrderLine ol)
	{        	   
		String trxName = ol.get_TrxName();
		Properties m_ctx = ol.getCtx();
		//
		MPPMRP mrp = getQuery(ol, TYPEMRP_Demand, ORDERTYPE_DistributionOrder).firstOnly();
		MLocator source = MLocator.get( m_ctx , ol.getM_Locator_ID());
		MLocator target = MLocator.get( m_ctx , ol.getM_LocatorTo_ID());
		if(mrp != null)
		{	
			mrp.setAD_Org_ID(source.getAD_Org_ID());
			mrp.setName("DemandDistOrder"); 
			mrp.setDescription(ol.getDescription());                            
			mrp.setDatePromised(ol.getDatePromised());
			mrp.setDateOrdered(ol.getDateOrdered());
			mrp.setM_Warehouse_ID(source.getM_Warehouse_ID()); 
			mrp.setM_Product_ID(ol.getM_Product_ID());                           
			mrp.setQty(ol.getQtyOrdered().subtract(ol.getQtyDelivered()));
			mrp.setDocStatus(ol.getParent().getDocStatus());
			mrp.saveEx(trxName);
		}
		else
		{
			mrp = new MPPMRP(m_ctx , 0 ,trxName);                              
			mrp.setAD_Org_ID(source.getAD_Org_ID());
			mrp.setName("NewDemandDistOrder");
			mrp.setDescription(ol.getDescription());
			mrp.setDD_Order_ID(ol.getDD_Order_ID());
			mrp.setDD_OrderLine_ID(ol.getDD_OrderLine_ID());
			mrp.setDatePromised(ol.getDatePromised());
			mrp.setDateOrdered(ol.getDateOrdered());
			mrp.setM_Warehouse_ID(source.getM_Warehouse_ID());
			mrp.setM_Product_ID(ol.getM_Product_ID());
			mrp.setQty(ol.getQtyOrdered().subtract(ol.getQtyDelivered()));
			mrp.setDocStatus(ol.getParent().getDocStatus());
			mrp.setOrderType(MPPMRP.ORDERTYPE_DistributionOrder);
			mrp.setTypeMRP(MPPMRP.TYPEMRP_Demand);
			mrp.saveEx(trxName);

		}
		mrp = getQuery(ol, TYPEMRP_Supply, ORDERTYPE_DistributionOrder).firstOnly();
		if(mrp != null)
		{	
			mrp.setAD_Org_ID(target.getAD_Org_ID());
			mrp.setName("SupplyDistOrder"); 
			mrp.setDescription(ol.getDescription());                            
			mrp.setDatePromised(ol.getDatePromised());
			mrp.setDateOrdered(ol.getDateOrdered());
			mrp.setM_Product_ID(ol.getM_Product_ID());                           
			mrp.setM_Warehouse_ID(target.getM_Warehouse_ID()); 
			mrp.setQty(ol.getQtyOrdered().subtract(ol.getQtyDelivered()));
			mrp.setDocStatus(ol.getParent().getDocStatus());
			mrp.saveEx(trxName);
		}	
		else
		{	
			mrp = new MPPMRP( m_ctx , 0,trxName);
			mrp.setAD_Org_ID(target.getAD_Org_ID());
			mrp.setName("NewSupplyDistOrder");
			mrp.setDescription(ol.getDescription());
			mrp.setDD_Order_ID(ol.getDD_Order_ID());
			mrp.setDD_OrderLine_ID(ol.getDD_OrderLine_ID());
			mrp.setDatePromised(ol.getDatePromised());
			mrp.setDateOrdered(ol.getDateOrdered());
			mrp.setM_Product_ID(ol.getM_Product_ID());
			mrp.setM_Warehouse_ID(target.getM_Warehouse_ID());
			mrp.setQty(ol.getQtyOrdered().subtract(ol.getQtyDelivered()));
			mrp.setDocStatus(ol.getParent().getDocStatus());
			mrp.setOrderType(MPPMRP.ORDERTYPE_DistributionOrder);
			mrp.setTypeMRP(MPPMRP.TYPEMRP_Supply);
			mrp.saveEx(trxName);
			}                       
		return;
	}

	/**
	 * Create MRP record based in Requisition
	 * @param r
	 */
	public static void M_Requisition(MRequisition r)
	{
		List<MPPMRP> mrpList = getQuery(r, null, null).list();
		for (MPPMRP mrp : mrpList)
		{
			mrp.setM_Requisition(r);
			mrp.saveEx(r.get_TrxName());
		}
	}

	/**
	 * Create MRP record based in Requisition Line
	 * @param MRequisitionLine Requisition Line
	 */
	public static void M_RequisitionLine(MRequisitionLine rl)
	{
		MPPMRP mrp = getQuery(rl, null, null).firstOnly();
		MRequisition r = rl.getParent();
		if (mrp == null)
		{
			mrp = new MPPMRP(rl.getCtx(), 0, rl.get_TrxName());  
			mrp.setM_Requisition_ID(rl.getM_Requisition_ID());
			mrp.setM_RequisitionLine_ID(rl.getM_RequisitionLine_ID());
		}
		mrp.setM_Requisition(r);
		//
		mrp.setAD_Org_ID(rl.getAD_Org_ID());
		mrp.setName("MRP");
		mrp.setDescription(rl.getDescription());                                                        
		mrp.setM_Product_ID(rl.getM_Product_ID());
		// We create a MRP record only for Not Ordered Qty. The Order will generate a MRP record for Ordered Qty.
		mrp.setQty(rl.getQty().subtract(rl.getQtyOrdered()));
		// MRP record for a requisition will be ALWAYS Drafted because
		// a requisition generates just Planned Orders (which is a wish list)
		// and not Scheduled Receipts
		//-->Ferry
		//mrp.setDocStatus(DocAction.STATUS_Drafted); 
		if (r.getDocStatus().compareTo(MRequisition.DOCSTATUS_Closed) == 0)	{
			mrp.setDocStatus(MPPMRP.DOCSTATUS_Completed);
			mrp.setQty(Env.ZERO);
		}  else if (r.getDocStatus().compareTo(MRequisition.DOCSTATUS_Voided) == 0) {
			mrp.setDocStatus(MPPMRP.DOCSTATUS_Voided); 
			mrp.setQty(Env.ZERO);
		} else if (mrp.getQty().compareTo(Env.ZERO) == 0) {
			mrp.setDocStatus(MPPMRP.DOCSTATUS_Completed);
		} else if (rl.getQtyOrdered().compareTo(Env.ZERO) > 0) {
			mrp.setDocStatus(MPPMRP.DOCSTATUS_InProgress);
		} else {
			mrp.setDocStatus(MPPMRP.STATUS_Drafted);
		}
		//<--Ferry
		mrp.saveEx(rl.get_TrxName());
	}

	/**
	 * @param product
	 * @return true if there are MRP records for given product 
	 */
	public static boolean hasProductRecords(MProduct product)
	{
		final String whereClause = COLUMNNAME_M_Product_ID+"=?"
									+" AND "+COLUMNNAME_Qty+"<>0";
		return new Query(product.getCtx(), Table_Name, whereClause, product.get_TrxName())
			.setParameters(new Object[]{product.getM_Product_ID()})
			.match();
	}

	/**
	 * Get Qty Onhand
	 * @param AD_Client_ID
	 * @param M_Warehouse_ID
	 * @param M_Product_ID
	 * @return
	 */
	public static BigDecimal getQtyOnHand(Properties ctx, int M_Warehouse_ID ,int M_Product_ID,String trxName)
	{	
		final String sql = "SELECT COALESCE(bomQtyOnHand (M_Product_ID,?,0),0) FROM M_Product"
							+" WHERE AD_Client_ID=? AND M_Product_ID=?";
		return DB.getSQLValueBDEx(trxName, sql, new Object[]{M_Warehouse_ID,Env.getAD_Client_ID(ctx),M_Product_ID});
	}
	
    /**
     * Get Reserved Quantity for a Warehouse 
	 * @param ctx
	 * @param M_Warehouse_ID
	 * @param M_Product_ID
	 * @param To
	 * @param trxName
	 * @return BibDecimal
	 */
    public static BigDecimal getQtyReserved(Properties ctx, int M_Warehouse_ID ,int M_Product_ID, Timestamp To, String trxName)
	{
    	final String sql = "SELECT SUM(Qty) FROM PP_MRP WHERE "
    		+" TypeMRP=?"
    		+" AND DocStatus IN ('IP','CO')"
    		//+" AND OrderType IN ('SOO','MOP','DOO')"
    		+" AND AD_Client_ID=? AND M_Warehouse_ID =? AND M_Product_ID=?"
    		+" AND DatePromised <=?";
    	BigDecimal qty = DB.getSQLValueBDEx(trxName, sql, new Object[]{
    			MPPMRP.TYPEMRP_Demand,
    			Env.getAD_Client_ID(ctx),M_Warehouse_ID, M_Product_ID,
    			To,
    	}); 		
		//	SQL may return no rows or null
		if (qty == null)
			return Env.ZERO;
		
		return qty;
     }
    
    /**
     * Get Reserved Quantity for a Warehouse 
	 * @param ctx
	 * @param M_Warehouse_ID
	 * @param M_Product_ID
	 * @param trxName
	 * @return BibDecimal
	 */
    public static BigDecimal getQtyReserved(Properties ctx, int M_Warehouse_ID ,int M_Product_ID,String trxName)
	{
    	return getQtyReserved(ctx, M_Warehouse_ID, M_Product_ID, new Timestamp (System.currentTimeMillis()), trxName);
    }
    
    /**
     * Get Reserved Quantity for a Warehouse 
	 * @param ctx
	 * @param M_Warehouse_ID
	 * @param M_Product_ID
	 * @param To
	 * @param trxName
	 * @return
	 */
    public static BigDecimal getQtyOrdered(Properties ctx, int M_Warehouse_ID ,int M_Product_ID, Timestamp To, String trxName)
	{
    	final String sql = "SELECT SUM(Qty) FROM PP_MRP WHERE "
    				+" TypeMRP='S' AND DocStatus IN ('IP','CO')"	
    				//+" AND OrderType IN ('POO','MOP','DOO')"
    				+" AND AD_Client_ID=?"
    				+" AND DatePromised <=?"
    				+" AND M_Warehouse_ID =? AND M_Product_ID=?";
		BigDecimal qty = DB.getSQLValueBDEx(trxName, sql,
				new Object[]{Env.getAD_Client_ID(ctx), To , M_Warehouse_ID, M_Product_ID}); 		
		//	SQL may return no rows or null
		if (qty == null)
			return Env.ZERO;
		
		return qty;
     }
    
	 /**
	 * Set Order Reserved Quantity for a Warehouse 
	 * @param AD_Client_ID
	 * @param M_Warehouse_ID
	 * @param M_Product_ID
	 * @param trxName
	 * @return
	 */
   public static BigDecimal getQtyOrdered(Properties ctx, int M_Warehouse_ID ,int M_Product_ID,String trxName)
   {
		return getQtyOrdered(ctx, M_Warehouse_ID, M_Product_ID, new Timestamp (System.currentTimeMillis()), trxName);
   }
       
	/**
	 * Maximum Low Level Code
	 * @param ctx
	 * @param trxName
	 * @return maximum low level
	 */
	public static int getMaxLowLevel(Properties ctx, String trxName)
	{
		int AD_Client_ID = Env.getAD_Client_ID(ctx);
		//
		final String sql = "SELECT MAX("+MProduct.COLUMNNAME_LowLevel+") FROM M_Product"
							+" WHERE AD_Client_ID=? AND "+MProduct.COLUMNNAME_LowLevel+" IS NOT NULL";                      
		int LowLevel = DB.getSQLValueEx(trxName, sql, AD_Client_ID);
		return LowLevel + 1;
	}
	
	/**
	 * Duration to have this Qty available (i.e. Lead Time + Transfer Time)
	 * @param qty quantity
	 * @param pp product planning sheet 
	 * @return return duration [days]
	 */
	public static int getDurationDays(MPPMRP mrp, BigDecimal qty, I_PP_Product_Planning pp)
	{
		//-->Ferry remark
		/*Properties ctx = null;
		if (pp instanceof PO)
		{
			ctx = ((PO)pp).getCtx();
		}
		else
		{
			ctx = Env.getCtx();
		}
		
		MProduct product = MProduct.get(ctx, pp.getM_Product_ID());
		BigDecimal leadtime = pp.getDeliveryTime_Promised();
		if (leadtime.signum() != 0 || product.isPurchased())
		{
			;
		}
		else if (pp.getS_Resource_ID() > 0 && pp.getAD_Workflow_ID() > 0)
		{
			RoutingService routingService = RoutingServiceFactory.get().getRoutingService(ctx);
			leadtime = routingService.calculateDuration(mrp,pp.getAD_Workflow(), pp.getS_Resource(), qty);
			// TODO: convert to days
		}
		else
		{
			throw new AdempiereException("Cannot calculate leadtime for "+pp); // TODO: translate or create notice?
		}
		return leadtime.add(pp.getTransfertTime()).intValue();
		*/
		BigDecimal DayMinutes = BigDecimal.valueOf(24 * 60);
		BigDecimal duration = BigDecimal.valueOf(getDurationMinutes(mrp, qty, pp, null)).divide(DayMinutes, 0, RoundingMode.UP);
		
		return duration.intValue();
		//<--Ferry		
	}
	
    //-->Ferry
	/**
	 * Duration to have this Qty available (i.e. Lead Time + Transfer Time)
	 * @param qty quantity
	 * @param pp product planning sheet 
	 * @param Timestamp DemandDateStartSchedule
	 * @return return duration [Minutes]
	 */
	public static int getDurationMinutes(MPPMRP mrp, BigDecimal qty, I_PP_Product_Planning pp, Timestamp DemandDateStartSchedule)
	{
		BigDecimal DayMinutes = BigDecimal.valueOf(24*60);
		
		Properties ctx = null;
		if (pp instanceof PO)
		{
			ctx = ((PO)pp).getCtx();
		}
		else
		{
			ctx = Env.getCtx();
		}
		
		MProduct product = MProduct.get(ctx, pp.getM_Product_ID());
		BigDecimal leadtime = pp.getDeliveryTime_Promised().multiply(DayMinutes); //DeliveryTime unit is days
		if (leadtime.signum() != 0 || product.isPurchased())
		{
			;
		}
		else if (pp.getS_Resource_ID() > 0 && pp.getAD_Workflow_ID() > 0)
		{
			RoutingService routingService = RoutingServiceFactory.get().getRoutingService(ctx);
			leadtime = routingService.calculateDuration(mrp,pp.getAD_Workflow(), pp.getS_Resource(), qty, DemandDateStartSchedule);
		}
		else
		{
			throw new AdempiereException("Cannot calculate leadtime for "+pp); // TODO: translate or create notice?
		}
		return leadtime.add(pp.getTransfertTime().multiply(DayMinutes)).intValue(); //TransferTime unit is days
	}	
	
	public static void clearStatic() {
		C_Order_ID = 0;
		C_OrderLine_ID = 0;
	}
	
	/**
	 * Get net qty of forecast
	 * @param Ctx Context
	 * @param mrp
	 * @param AD_Client_ID
	 * @param trxName
	 * @return
	 */
	public static BigDecimal getNetQtyForecast(Properties ctx, MPPMRP mrp, int AD_Client_ID, String trxName) {
		String sql = "SELECT PP_MRP_ID"
				+" FROM PP_MRP"
				+" WHERE TypeMRP=?" //1
				+" AND AD_Client_ID=?" //2
				+" AND AD_Org_ID=? " //3
				+" AND M_Warehouse_ID=?" //4
				+" AND M_Product_ID=?" //5				
				+" AND DatePromised>=?" //6
				+" AND DatePromised<=?"  //7
				+" AND PP_MRP_ID<>?"  //8
				+" AND Qty <>0"
				+" ORDER BY DatePromised";
		
		BigDecimal Qty = mrp.getQty();
		
		//get end date of the month		
		Calendar calEnd = Calendar.getInstance(); 
		calEnd.setTimeInMillis(mrp.getDatePromised().getTime());
		calEnd.add(Calendar.MONTH, 1);  
		calEnd.set(Calendar.DAY_OF_MONTH, 1); 
		calEnd.set(Calendar.HOUR_OF_DAY, 0);
	    calEnd.set(Calendar.MINUTE, 0);
	    calEnd.set(Calendar.SECOND, 0);		
		calEnd.add(Calendar.SECOND, -1);  	
		Timestamp datePromisedEnd = new Timestamp(calEnd.getTimeInMillis());		
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setString(1, MPPMRP.TYPEMRP_Demand);
			pstmt.setInt(2, AD_Client_ID);
			pstmt.setInt(3, mrp.getAD_Org_ID());
			pstmt.setInt(4, mrp.getM_Warehouse_ID());
			pstmt.setInt(5, mrp.getM_Product_ID());
			pstmt.setTimestamp(6, mrp.getDatePromised());
			pstmt.setTimestamp(7, datePromisedEnd);
			pstmt.setInt(8, mrp.getPP_MRP_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				final int PP_MRP_ID = rs.getInt(MPPMRP.COLUMNNAME_PP_MRP_ID);		
				MPPMRP mrpDemand = new MPPMRP(ctx, PP_MRP_ID, trxName);
				
				if (mrpDemand.getOrderType().equals(MPPMRP.ORDERTYPE_Forecast) || Qty.compareTo(Env.ZERO) <= 0) {
					//if meet next forecast or net qry <= 0 then exit from loop
					break;
				}
				
				Qty = Qty.subtract(mrpDemand.getQty());				
						
			}
			Qty = Qty.max(Env.ZERO);
		} // try
		catch (SQLException ex)
		{
			throw new DBException(ex);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		return Qty;
	}
	//Return true is MRP is material demand of MO
	public static boolean materialDemandOfMO(MPPMRP mrp) {
	  return (mrp.getTypeMRP().equals(TYPEMRP_Demand) && mrp.getOrderType().equals(ORDERTYPE_ManufacturingOrder));
	}
	//<--Ferry
	
	public static String getDocumentNo(int PP_MRP_ID)
	{
		return DB.getSQLValueStringEx(null, "SELECT documentNo(PP_MRP_ID) AS DocumentNo FROM PP_MRP WHERE PP_MRP_ID = ?", PP_MRP_ID);
	}

	public String toString()
	{
		String description = getDescription();
		return getClass().getSimpleName()+"["
			+", TypeMRP="+getTypeMRP()
			+", DocStatus="+getDocStatus()
			+", Qty="+getQty()
			+", DatePromised="+getDatePromised()
			+", Schedule="+getDateStartSchedule()+"/"+getDateFinishSchedule()
			+", IsAvailable="+isAvailable()
			+(!Util.isEmpty(description, true) ? ", Description="+description : "")
			+", ID="+get_ID()
			+"]";
	}

	@Override
	public boolean processIt(String action) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unlockIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean invalidateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String prepareIt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean approveIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rejectIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String completeIt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean voidIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean closeIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reverseCorrectIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reverseAccrualIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reActivateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentNo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File createPDF() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDoc_User_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getC_Currency_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BigDecimal getApprovalAmt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocAction() {
		// TODO Auto-generated method stub
		return null;
	}

}	//	MPPMRP
