package org.libero.process.eam;

import org.compiere.model.I_M_ForecastLine;
import org.compiere.model.MAsset;
import org.compiere.model.MForecastLine;
import org.compiere.model.MRequest;
import org.compiere.model.MStatus;
import org.compiere.model.PO;
import org.compiere.model.POResultSet;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.eevolution.model.MPPProductPlanning;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderWorkflow;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;


/**
 *	Process CreateMOFromForecastLine4eAM
 *	
 *  @author Peter Shepetko
 *  @version $Id: CreateMOFromForecastLine4eAM.java,v 1.0 2017/12/20 pshepetko Exp $
 *  
 * 
 */
public class CreateMOFromForecastLine4eAM extends SvrProcess
{
 	private int p_AD_Client_ID= Env.getAD_Client_ID(Env.getCtx());
 	private int p_AD_User_ID= Env.getAD_User_ID(Env.getCtx());
	private int p_A_Asset_ID=0;
	private int p_M_Forecast_ID=0;
	private int p_PlanningHorizon=0;
	private Boolean p_DeleteOld=false;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		for (ProcessInfoParameter para:getParameter())
		{
			String name = para.getParameterName();
			
			if (para.getParameter() == null)
				;
			else if (name.equals("PlanningHorizon"))
			{
				p_PlanningHorizon = para.getParameterAsInt();
			}	
			else if (name.equals("A_Asset_ID"))
			{
				p_A_Asset_ID = para.getParameterAsInt();
			}
			else if (name.equals("M_Forecast_ID"))
			{
				p_M_Forecast_ID = para.getParameterAsInt();
			}
			else if (name.equals("DeleteOld"))
			{    
				p_DeleteOld = para.getParameterAsBoolean();        
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}
	
	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		String _result="[v1.07] "; 
		int count_ok=0;
		int count_error=0;
		Timestamp DateFinishSchedule=null;
		Timestamp DateStartSchedule=null;
 		Trx  trx_fcline = Trx.get(Trx.createTrxName("fcLine"), true);

 		MAsset asset = new MAsset(getCtx(),p_A_Asset_ID, get_TrxName());
 		int BPUser_ID=0;
 		//delete MO
 		if (p_DeleteOld)
 		{
 		List<Object> params = new ArrayList<Object>();
		params.add(asset.getAD_Client_ID());
		params.add(asset.getAD_Org_ID());
		params.add(asset.getM_Product_ID());
		deletePO(MRequest.Table_Name, " Record_ID IN (SELECT PP_Order_ID FROM PP_Order "
				+ " WHERE DocStatus IN ('DR','IP') AND AD_Client_ID=? AND AD_Org_ID=? AND M_Product_ID=?)", params);
		deletePO(MPPOrder.Table_Name, " DocStatus IN ('DR','IP') AND AD_Client_ID=? AND AD_Org_ID=? AND M_Product_ID=?", params);
 		}
 		
try
{			
//		String whereClause = "EXISTS (SELECT T_Selection_ID FROM T_Selection WHERE  T_Selection.AD_PInstance_ID=? " +
//				"AND T_Selection.T_Selection_ID=DD_OrderLine.DD_OrderLine_ID)";		
	String whereClause = " M_Product_ID="+asset.getM_Product_ID();
	
	Collection <MForecastLine> fclines = new Query(getCtx(), I_M_ForecastLine.Table_Name, whereClause,trx_fcline.getTrxName())
										.setOrderBy("M_ForecastLine_ID")
										.setClient_ID()
//										.setParameters(new Object[]{getAD_PInstance_ID()	})
										.list();
		
		for (MForecastLine fcline : fclines)
		{
			int PP_ID= 50102;//fcline.get_ValueAsInt("PP_Product_Planning_ID");//DB.getSQLValue (get_TrxName(), "SELECT pp_product_planning_id FROM M_ForecastLine WHERE isActive='Y' AND M_ForecastLine_ID="+ fcline.getM_ForecastLine_ID());
if (PP_ID>0)
{	
	MPPProductPlanning pp = new MPPProductPlanning (Env.getCtx(),PP_ID, trx_fcline.getTrxName());

//		 	int res_id = DB.getSQLValue( trx_fcline.getTrxName(), "SELECT pp.S_Resource_ID FROM PP_Product_Planning pp WHERE pp.AD_Client_ID="+fcline.getAD_Client_ID()+" AND  pp.AD_Org_ID="+fcline.getAD_Org_ID()+" AND pp.M_Warehouse_ID="+fcline.getM_Warehouse_ID()+" AND pp.M_Product_ID=?", fcline.getM_Product_ID());
//		 	int bom_id = DB.getSQLValue( trx_fcline.getTrxName(), "SELECT pp.PP_Product_BOM_ID FROM PP_Product_Planning pp WHERE pp.AD_Client_ID="+fcline.getAD_Client_ID()+" AND  pp.AD_Org_ID="+fcline.getAD_Org_ID()+" AND pp.M_Warehouse_ID="+fcline.getM_Warehouse_ID()+" AND pp.M_Product_ID=?", fcline.getM_Product_ID());
//		 	int wf_id = DB.getSQLValue( trx_fcline.getTrxName(), "SELECT pp.AD_Workflow_ID FROM PP_Product_Planning pp WHERE pp.AD_Client_ID="+fcline.getAD_Client_ID()+" AND  pp.AD_Org_ID="+fcline.getAD_Org_ID()+" AND pp.M_Warehouse_ID="+fcline.getM_Warehouse_ID()+" AND pp.M_Product_ID=?", fcline.getM_Product_ID());
//		 	int planner_id = DB.getSQLValue( trx_fcline.getTrxName(), "SELECT pp.Planner_ID FROM PP_Product_Planning pp WHERE pp.AD_Client_ID="+fcline.getAD_Client_ID()+" AND  pp.AD_Org_ID="+fcline.getAD_Org_ID()+" AND pp.M_Warehouse_ID="+fcline.getM_Warehouse_ID()+" AND pp.M_Product_ID=?", fcline.getM_Product_ID());

			MPPOrder morder = new MPPOrder(Env.getCtx(),0,  trx_fcline.getTrxName());
			morder.addDescription("[created from Forecast])"); 
			morder.setAD_Org_ID(fcline.getAD_Org_ID());
			morder.setLine(10);
			morder.setC_DocTypeTarget_ID(50010);  
			morder.setC_DocType_ID(50010); 
			morder.setS_Resource_ID(pp.getS_Resource_ID());
			morder.setM_Warehouse_ID(fcline.getM_Warehouse_ID());
			morder.setM_Product_ID(fcline.getM_Product_ID());
			morder.setM_AttributeSetInstance_ID(0);
 			morder.setPP_Product_BOM_ID(pp.getPP_Product_BOM_ID());
 			morder.setAD_Workflow_ID(pp.getAD_Workflow_ID());
			morder.setPlanner_ID(pp.getPlanner_ID());
			morder.setDateOrdered(fcline.getDatePromised());                       
			morder.setDatePromised(fcline.getDatePromised());
			morder.setDateStartSchedule(fcline.getDatePromised());
			morder.setDateFinishSchedule(fcline.getDatePromised());
			morder.setQty(fcline.getQty());
//			morder.setC_UOM_ID(p_orderline.getC_UOM_ID());
			morder.setYield(Env.ZERO);
			morder.setScheduleType("D");
			morder.setPriorityRule(MPPOrder.PRIORITYRULE_Medium);
//			morder.setDocAction(MPPOrder.DOCACTION_Complete);
			morder.saveEx();
			
			//doc processed to InProcess
			if (morder.processIt("IP"))
			{
				morder.saveEx();
				count_ok++;
			} else {
				count_error++;		
			}
			
			trx_fcline.commit();
			//--< update Node
/*			MPPOrder new_morder = new MPPOrder(Env.getCtx(), morder.getPP_Order_ID(),  trx_fcline.getTrxName());
			MPPOrderWorkflow new_owf = new_morder.getMPPOrderWorkflow();
			MPPOrderNode new_pon =(MPPOrderNode) new_owf.getPP_Order_Node();	
			 if (new_pon.getS_Resource().getS_ResourceType().getC_UOM_ID()==101)// add hours
				 DateStartSchedule=TimeUtil.addMinutess(morder.getDatePromised(),(
							new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getHours()*60)+
							new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getMinutes());
//			new_pon.setDateStartSchedule(DateStartSchedule);

			
			 if (new_pon.getS_Resource().getS_ResourceType().getC_UOM_ID()==103) // add minutes
				DateFinishSchedule=TimeUtil.addMinutess(morder.getDatePromised(),(new_pon.getSetupTimeRequired()+new_pon.getDurationRequired()+
						(new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getHours()*60)+
						new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getMinutes()));
			 
			else if (new_pon.getS_Resource().getS_ResourceType().getC_UOM_ID()==101)// add hours
				DateFinishSchedule=TimeUtil.addMinutess(morder.getDatePromised(),((new_pon.getSetupTimeRequired()+new_pon.getDurationRequired()+
						new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getHours())*60)+
						new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getMinutes());
			 
			else if (new_pon.getS_Resource().getS_ResourceType().getC_UOM_ID()==102) // add days
				DateFinishSchedule=TimeUtil.addDays(morder.getDatePromised(),(new_pon.getSetupTimeRequired()+new_pon.getDurationRequired()));
//			new_pon.setDateFinishSchedule(DateFinishSchedule);
			new_pon.setDescription(morder.getDatePromised()+"|"+DateFinishSchedule+"|"+new_pon.getWorkingTime()+"|"+ +
					new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getHours()+"|"+
					new_pon.getS_Resource().getS_ResourceType().getTimeSlotStart().getMinutes());
			
			DateStartSchedule=morder.getDateStartSchedule();
			new_pon.setDateStartSchedule(DateStartSchedule);
			new_pon.setDateFinishSchedule(DateFinishSchedule);
			new_pon.saveEx();
			
			morder.setDateFinishSchedule(DateFinishSchedule);
			morder.saveEx();
			
			if (new_pon.getC_BPartner_ID()>0) 
			{
				BPUser_ID = DB.getSQLValue( trx_fcline.getTrxName(), "SELECT MAX(AD_User_ID) FROM AD_User WHERE C_BPartner_ID="+new_pon.getC_BPartner_ID());
			}
			else
				BPUser_ID = new_morder.getPlanner_ID();
			
			//doc processed to InProcess
			if (new_morder.processIt("IP"))
			{
				new_morder.saveEx();
				_result+="|+"+new_morder.getDocumentNo();
			} else {
				_result+="|-"+new_morder.getDocumentNo();		
			}
*/
/*		
			//---- request create for Calendar based MO
			if ( new_morder.getPP_Order_ID() >0) {
				MRequest request = new MRequest(Env.getCtx(), 0, get_TrxName());
				MStatus st = MStatus.getDefault(getCtx(), 1000005);//hard for maintenance
				
				request.setR_RequestType_ID(1000005);
				DateFormat df = new SimpleDateFormat("HH:mm dd/MM/yyyy");
				
				request.setSummary(new_morder.getM_Product().getName()+" <konserwacja>"+ System.lineSeparator()
//						+"Wykonawca:"+new_pon.getC_BPartner().getName()+ System.lineSeparator()+
//						"Czas:"+df.format(DateStartSchedule)+"-"+df.format(DateFinishSchedule)
						);	
				//added new columl HTML
				request.set_ValueOfColumn("HTML","Maintenance:<b>"+new_morder.getM_Product().getName()+ "</b><br>"
//						+"Wykonawca:<b>"+new_pon.getC_BPartner().getName()+ System.lineSeparator()+ "</b><br>"+
//						"Czas:<b>"+df.format(DateStartSchedule)+"-"+df.format(DateFinishSchedule)+"</b>"
						);
				
				request.setR_Status_ID(); //�������� ����������

				request.setSalesRep_ID(Env.getAD_User_ID(Env.getCtx()));//hard for user
 				request.setAD_Role_ID(Env.getAD_Role_ID(Env.getCtx()));

 				request.setStartDate(new_morder.getDatePromised());
 				request.setM_Product_ID(new_morder.getM_Product_ID());
				request.setM_ProductSpent_ID(fcline.getM_Product_ID());
 				 
//				request.setC_BPartner_ID(mr.getC_BPartner_ID());
				request.setAD_Table_ID(new_morder.get_Table_ID()); //PP_Order
				request.setRecord_ID(new_morder.get_ID() );
				request.setConfidentialType("C");
				request.setConfidentialTypeEntry("C");
				request.setPriority("3");
				request.setPriorityUser("3");
				request.setDueType("7");//Scheduled
				request.setStartTime(DateStartSchedule);
				request.setEndTime(DateFinishSchedule);
				request.setDateStartPlan(DateStartSchedule);
				request.setDateCompletePlan(DateFinishSchedule);
				request.saveEx();
				_result+="|R+"+request.getDocumentNo();
		}
*/		
			//--------------------------
			
			//--> update Node 
			trx_fcline.commit();
			
			//_result+="|+"+morder.getDocumentNo();
		}
		
		}//pp_ID
		fclines.clear();
}
finally
{
	if (trx_fcline != null) trx_fcline.close();	 
}
		
        return _result+" Created:"+count_ok+" [Errors:"+count_error+"]";
	}
	
	private void deletePO(String tableName, String whereClause, List<Object> params) throws SQLException
	{
		// TODO: refactor this method and move it to org.compiere.model.Query class
		POResultSet<PO> rsd = new Query(getCtx(), tableName, whereClause, get_TrxName())
		.setParameters(params)
		.scroll();
		try
		{
			while(rsd.hasNext())
			{
				rsd.next().deleteEx(true);
				commitEx();
			}
		}
		finally
		{
			rsd.close();
		}
	}

}	