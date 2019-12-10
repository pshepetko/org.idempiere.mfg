/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, iDempiere contributors and Mutlimage SLOVAKIA
 */
package org.idempiere.component;

import java.math.BigDecimal;
import java.util.Collection;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_Forecast;
import org.compiere.model.I_M_ForecastLine;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Movement;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Requisition;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MForecastLine;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_M_Forecast;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPMRP;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;
import org.libero.tables.I_DD_Order;
import org.libero.tables.I_DD_OrderLine;
import org.libero.tables.I_PP_Order;
import org.libero.tables.I_PP_Order_BOMLine;
import org.osgi.service.event.Event;

/**
 *
 * @author hengsin (new Event ModelValidator regime)
 * @author Victor Perez, Teo Sarca,  
 * @contributor red1@red1.org (refactoring to new OSGi framework)
 *
 */
public class MFG_Validator extends AbstractEventHandler {
	private static CLogger log = CLogger.getCLogger(MFG_Validator.class);
	private String trxName = "";
	private PO po = null;
	@Override
	protected void initialize() {
		registerEvent(IEventTopics.AFTER_LOGIN);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, I_M_Movement.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, I_M_Product.Table_Name); 
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_C_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_C_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Requisition.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_RequisitionLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_DD_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_DD_OrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_PP_Order_BOMLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, I_M_ForecastLine.Table_Name); 
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, I_M_Forecast.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, I_M_ForecastLine.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, I_M_Movement.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, I_M_InOut.Table_Name);
		log.info("MFG MODEL VALIDATOR IS NOW INITIALIZED");
	}

	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		DocAction doc = null;
		boolean isDelete = false;
		boolean isReleased = false;
		boolean isVoided = false;
		boolean isChange = false;
		
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			LoginEventData eventData = getEventData(event);
			log.fine(" topic="+event.getTopic()+" AD_Client_ID="+eventData.getAD_Client_ID()
					+" AD_Org_ID="+eventData.getAD_Org_ID()+" AD_Role_ID="+eventData.getAD_Role_ID()
					+" AD_User_ID="+eventData.getAD_User_ID());
		}
		else 
		{
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			log.info(" topic="+event.getTopic()+" po="+po);
			isChange = (IEventTopics.PO_AFTER_NEW == type || (IEventTopics.PO_AFTER_CHANGE == type && MPPMRP.isChanged(po)));
			isDelete = (IEventTopics.PO_BEFORE_DELETE == type);
			isReleased = false;
			isVoided = false;
			
			if (po instanceof DocAction)
			{
				doc = (DocAction)po;
			}
				else if (po instanceof MOrderLine)
			{
				doc = ((MOrderLine)po).getParent();
			}
			
			if (doc != null)
			{
				String docStatus = doc.getDocStatus();
				isReleased = DocAction.STATUS_InProgress.equals(docStatus)
							|| DocAction.STATUS_Completed.equals(docStatus);
				isVoided = DocAction.STATUS_Voided.equals(docStatus);
			}
		
			// Can we change M_Product.C_UOM_ID ?
			if (po instanceof MProduct && IEventTopics.PO_BEFORE_CHANGE == type
					&& po.is_ValueChanged(MProduct.COLUMNNAME_C_UOM_ID)
					&& MPPMRP.hasProductRecords((MProduct)po))
			{
				throw new AdempiereException("@SaveUomError@");
			}
		 
			if (isDelete || isVoided || !po.isActive())
			{
				logEvent(event, po, type);//log.fine("MPPMRP.deleteMRP(po)");
				MPPMRP.deleteMRP(po);
			}
			else if (po instanceof MOrder)
			{
				MOrder order = (MOrder)po;
			// Create/Update a planning supply when isPurchase Order
			// or when you change DatePromised or DocStatus and is Purchase Order
				if (isChange && !order.isSOTrx())
				{
					logEvent(event, po, type);//log.fine("isChange && !order.isSOTrx() .. MPPMRP.C_Order(order)");
					MPPMRP.C_Order(order);
				}
			// Update MRP when you change the status order to complete or in process for a sales order
			// or you change DatePromised
				else if (type == IEventTopics.PO_AFTER_CHANGE && order.isSOTrx())
				{
					if (isReleased || MPPMRP.isChanged(order)) 
					{	
						logEvent(event, po, type);//log.fine("isReleased || MPPMRP.isChanged(order) .. MPPMRP.C_Order(order)");
						MPPMRP.C_Order(order);
					}
				}
			}
		// 
			else if (po instanceof MOrderLine && isChange)
			{
				MOrderLine ol = (MOrderLine)po;
				MOrder order = ol.getParent();
			// Create/Update a planning supply when isPurchase Order or you change relevant fields
				if (!order.isSOTrx())
				{
					logEvent(event, po, type);//log.fine("!order.isSOTrx() .. MPPMRP.C_OrderLine(ol)");
					MPPMRP.C_OrderLine(ol);
				}
			// Update MRP when Sales Order have document status in process or complete and 
			// you change relevant fields
				else if(order.isSOTrx() && isReleased)
				{
					logEvent(event, po, type);//log.fine("order.isSOTrx() && isReleased .. MPPMRP.C_OrderLine(ol)");
					MPPMRP.C_OrderLine(ol);
				}
			}
		//
			else if (po instanceof MRequisition && isChange)
			{
				MRequisition r = (MRequisition)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.M_Requisition(r)");
				MPPMRP.M_Requisition(r);
			}
		//
			else if (po instanceof MRequisitionLine && isChange)
			{
				MRequisitionLine rl = (MRequisitionLine)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.M_Requisition(rl)");
				MPPMRP.M_RequisitionLine(rl);
			}
		//
			else if (po instanceof X_M_Forecast && isChange)
			{
				X_M_Forecast fl = (X_M_Forecast)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.M_Forecast(fl)");
				MPPMRP.M_Forecast(fl);
			}
		//
			else if (po instanceof MForecastLine && isChange)
			{
				MForecastLine fl = (MForecastLine)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.M_ForecastLine(fl)");
				MPPMRP.M_ForecastLine(fl);
			}
		
			else if (po instanceof MDDOrder  && isChange)
			{
				MDDOrder order = (MDDOrder)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.DD_Order(order)");
				MPPMRP.DD_Order(order);
			}
		
		//
			else if (po instanceof MDDOrderLine && isChange)
			{
				MDDOrderLine ol = (MDDOrderLine)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.DD_OrderLine(ol)");
				MPPMRP.DD_OrderLine(ol);
			}
		//
			else if (po instanceof MPPOrder && isChange)
			{
				MPPOrder order = (MPPOrder)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.PP_Order(order)");
				MPPMRP.PP_Order(order);
			}
		//
			else if (po instanceof MPPOrderBOMLine && isChange)
			{
				MPPOrderBOMLine obl = (MPPOrderBOMLine)po;
				logEvent(event, po, type);//log.fine(" .. MPPMRP.PP_Order_BOMLine(obl)");
				MPPMRP.PP_Order_BOMLine(obl);
			}	
			
		//PO: TYPE_AFTER_NEW
			if (event.getTopic().equals(IEventTopics.PO_AFTER_NEW)) {
				po = getPO(event);
				log.info(" topic="+event.getTopic()+" po="+po); 
		
		//MProduct: TYPE_BEFORE_CHANGE
			} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_CHANGE)) { 
			 po = getPO(event);
			log.info(" topic="+event.getTopic()+" po="+po);
			if (po.get_TableName().equals(I_M_Product.Table_Name)) {
 				String msg = "TODO";
 				logEvent(event, po, type);//log.fine("EVENT MANAGER // Product: PO_BEFORE_CHANGE >> MFG TODO 1 = '"+msg+"'");
			}
		}
		if (po instanceof MInOut && type == IEventTopics.DOC_AFTER_COMPLETE)
			{
				logEvent(event, po, type);//
				MInOut inout = (MInOut)po;
				if(inout.isSOTrx())
				{
					for (MInOutLine outline : inout.getLines())
					{										
						updateMPPOrder(outline);				
					}
				}
			//Purchase Receipt
				else
				{	
					for (MInOutLine line : inout.getLines())
					{
						final String whereClause = "C_OrderLine_ID=? AND PP_Cost_Collector_ID IS NOT NULL";
						Collection<MOrderLine> olines = new Query(po.getCtx(), MOrderLine.Table_Name, whereClause, trxName)
													.setParameters(new Object[]{line.getC_OrderLine_ID()})
													.list();
						for (MOrderLine oline : olines)
						{
							if(oline.getQtyOrdered().compareTo(oline.getQtyDelivered()) >= 0)
							{	
								MPPCostCollector cc = new MPPCostCollector(po.getCtx(), oline.getPP_Cost_Collector_ID(), trxName);
								String docStatus = cc.completeIt();
								cc.setDocStatus(docStatus);
								cc.setDocAction(MPPCostCollector.DOCACTION_Close);
								cc.saveEx(trxName);
								return;
							}
						}	
					}
				}	
			}
		//
		// Update Distribution Order Line
			else if (po instanceof MMovement && type == IEventTopics.DOC_AFTER_COMPLETE)
			{
				logEvent(event, po, type);//
				MMovement move = (MMovement)po;
				for (MMovementLine line : move.getLines(false))
				{
					if(line.getDD_OrderLine_ID() > 0)
					{
						MDDOrderLine oline= new MDDOrderLine(line.getCtx(),line.getDD_OrderLine_ID(), po.get_TrxName());
						MLocator locator_to = MLocator.get(line.getCtx(), line.getM_LocatorTo_ID());
						MWarehouse warehouse =  MWarehouse.get(line.getCtx(), locator_to.getM_Warehouse_ID()); 
						if(warehouse.isInTransit())
						{
							oline.setQtyInTransit(oline.getQtyInTransit().add(line.getMovementQty()));
							oline.setConfirmedQty(Env.ZERO);
						}
						else
						{
							oline.setQtyInTransit(oline.getQtyInTransit().subtract(line.getMovementQty()));
							oline.setQtyDelivered(oline.getQtyDelivered().add(line.getMovementQty()));
						}   
						oline.saveEx(trxName);				   
					}
				}			
				if(move.getDD_Order_ID() > 0)
				{	
					MDDOrder order = new MDDOrder(move.getCtx(), move.getDD_Order_ID(), move.get_TrxName());
					order.setIsInTransit(isInTransit(order));
					order.reserveStock(order.getLines(true, null));
					order.saveEx(trxName);
					}	
				}
			}
	}
	
	/**
	 * Define if a Distribution Order is in transit 
	 * @param order
	 * @return true or false
	 */
	private boolean isInTransit(MDDOrder order)
	{
		for (MDDOrderLine line : order.getLines(true, null))
		{
			if(line.getQtyInTransit().signum() != 0)
			{
				return true;
			}
		}
		return false;
	}
	
	private void updateMPPOrder(MInOutLine outline)
	{
		MPPOrder order = null;
		BigDecimal qtyShipment = Env.ZERO;
		MInOut inout =  outline.getParent();
		String movementType = inout.getMovementType();
		int C_OrderLine_ID = 0;
		if(MInOut.MOVEMENTTYPE_CustomerShipment.equals(movementType))
		{
		   C_OrderLine_ID = outline.getC_OrderLine_ID();
		   qtyShipment = outline.getMovementQty();
		}
		else if (MInOut.MOVEMENTTYPE_CustomerReturns.equals(movementType)) 
		{
				MRMALine rmaline = new MRMALine(outline.getCtx(),outline.getM_RMALine_ID(), null); 
				MInOutLine line = (MInOutLine) rmaline.getM_InOutLine();
				C_OrderLine_ID = line.getC_OrderLine_ID();
				qtyShipment = outline.getMovementQty().negate();
		}
		
		final String whereClause = " C_OrderLine_ID = ? "
				+ " AND DocStatus IN  (?,?)"
				+ " AND EXISTS (SELECT 1 FROM  PP_Order_BOM "
				+ " WHERE PP_Order_BOM.PP_Order_ID=PP_Order.PP_Order_ID AND PP_Order_BOM.BOMType =? )"; 
	
		order = new Query(outline.getCtx(), I_PP_Order.Table_Name, whereClause, outline.get_TrxName())
			 .setParameters(new Object[]{C_OrderLine_ID,
				 					 MPPOrder.DOCSTATUS_InProgress,
				 					 MPPOrder.DOCSTATUS_Completed,
				 					 MPPOrderBOM.BOMTYPE_Make_To_Kit
				 					}).firstOnly();
		if (order == null)
		{	
			return;
		}
		
		if(MPPOrder.DOCSTATUS_InProgress.equals(order.getDocStatus()))
		{
			order.completeIt();
			order.setDocStatus(MPPOrder.ACTION_Complete);
			order.setDocAction(MPPOrder.DOCACTION_Close);
			order.saveEx(trxName);			
		}
		if (MPPOrder.DOCSTATUS_Completed.equals(order.getDocStatus()))
		{	
			String description = order.getDescription() !=  null ?  order.getDescription() : ""
				+ Msg.translate(inout.getCtx(), MInOut.COLUMNNAME_M_InOut_ID) 
				+ " : " 
				+ Msg.translate(inout.getCtx(), MInOut.COLUMNNAME_DocumentNo);
			order.setDescription(description);
			order.updateMakeToKit(qtyShipment);
			order.saveEx(trxName);
		}
		
		if(order.getQtyToDeliver().compareTo(Env.ZERO)==0)
		{
			order.closeIt();
			order.setDocStatus(MPPOrder.DOCACTION_Close);
			order.setDocAction(MPPOrder.DOCACTION_None);
			order.saveEx(trxName);
		}
		return;
	}
	//red1 factored log message handling
	private void logEvent (Event event, PO po, String msg) {
		log.info("LiberoMFG >> ModelValidator // "+event.getTopic()+" po="+po+" MESSAGE ="+msg);		
	}

	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}

}
