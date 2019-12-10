package org.libero.process.eam;

import org.compiere.model.MAsset;
import org.compiere.model.MForecast;
import org.compiere.model.MForecastLine;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.model.MPPProductPlanning;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;


/**
 *	Process CreateForecastLine4eAM
 *	
 *  @author Peter Shepetko
 *  @version $Id: CreateForecastLine4eAM.java,v 1.0 2017/12/20 pshepetko Exp $
 *  
 * 
 */
public class CreateForecastLine4eAM extends SvrProcess
{
 	private int p_AD_Client_ID= Env.getAD_Client_ID(Env.getCtx());
 	private int p_AD_User_ID= Env.getAD_User_ID(Env.getCtx());
	private int p_A_Asset_ID=0;
	private int p_M_Forecast_ID=0;
	private Timestamp p_DateValue = null;
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
		if (p_DateValue==null)  p_DateValue=new Timestamp (System.currentTimeMillis()); 
		
		String _result=""; 
		String pmrule=null;
		Timestamp MaintenanceDate = null;
		Timestamp FinishMaintenanceDate = null;
		Timestamp NextMaintenanceDate = null;
		Timestamp LastMaintenanceDate = null;
		int LastMaintenanceUnit=0;
		int rate = 0;
		int rate_planning_end = 0;
		int rate_current = 0;
		int useunits=0;
		int counter1=1;
		int counter2=1;
		int unitscycles=0;
		int unit_current=0;
		
		MAsset asset = new MAsset(getCtx(),p_A_Asset_ID, get_TrxName());
		
		if (p_DeleteOld)
		{
		DB.executeUpdateEx("DELETE FROM pp_mrp WHERE  m_forecastline_ID IN "+
				 " (SELECT m_forecastline_ID FROM m_forecastline WHERE m_product_ID="+asset.getM_Product_ID()+" AND  AD_Org_ID=" +asset.getAD_Org_ID()+");"+
				 " DELETE FROM m_forecastline WHERE m_product_ID="+asset.getM_Product_ID()+" AND AD_Org_ID="+asset.getAD_Org_ID(), get_TrxName());
		commitEx();
		}
		
		
		String sql_so = "SELECT a_asset_id, Asset_Prev_Maintenance_Rule,  " + 
				" nextmaintenencedate, pp_product_planning_id, rate, unitscycles,  " + 
				" description, c_uom_id, validfrom, validto " + 
				" FROM A_Asset_Prev_Maintenance "
				+ " WHERE isActive='Y' AND A_Asset_ID="+p_A_Asset_ID;
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql_so, null);
				rs = pstmt.executeQuery ();
				while(rs.next ())
				{
					//------------
//					pmrule = getPMRuleStr(p_A_Asset_ID, "Asset_Prev_Maintenance_Rule");
					
					//if (rs.getInt(4)>0)
					MPPProductPlanning pp =new MPPProductPlanning(getCtx(),rs.getInt(4), get_TrxName());			
					
					pmrule=rs.getString("Asset_Prev_Maintenance_Rule");
					if (pmrule.equals("D"))
					{
						FinishMaintenanceDate =TimeUtil.addDays(asset.getLastMaintenanceDate(), p_PlanningHorizon);
						MaintenanceDate =TimeUtil.addDays(asset.getLastMaintenanceDate(), rs.getInt("UnitsCycles"));//getPMRuleInt(p_A_Asset_ID, "UnitsCycles"));
					    while(MaintenanceDate.compareTo(FinishMaintenanceDate)<=0) {
							MForecastLine fcl =new MForecastLine(getCtx(),0, get_TrxName());
						fcl.setAD_Org_ID(pp.getAD_Org_ID());
							fcl.setM_Forecast_ID(p_M_Forecast_ID);
							fcl.setM_Product_ID(asset.getM_Product_ID());
							fcl.setM_Warehouse_ID(pp.getM_Warehouse_ID());
							fcl.setQty(Env.ONE);
							fcl.setC_Period_ID(getPeriod_ID(MaintenanceDate));
							fcl.setDatePromised(MaintenanceDate);
						//	fcl.set_ValueOfColumn("Description", "D");
//							fcl.set_ValueOfColumn("pp_product_planning_id",  rs.getInt(4));
							fcl.saveEx();   
							
							MaintenanceDate =TimeUtil.addDays(MaintenanceDate,rs.getInt("UnitsCycles"));//getPMRuleInt(p_A_Asset_ID, "UnitsCycles"));	
					      }
						_result+=" Created Forecat Lines for "+asset.getName()+" based Date Rules!";	
					}
					
					if (pmrule.equals("M"))
					{
						useunits=asset.getUseUnits();
						rate=rs.getInt("Rate");//getPMRuleInt(p_A_Asset_ID, "Rate");
						unitscycles=rs.getInt("UnitsCycles");//getPMRuleInt(p_A_Asset_ID, "UnitsCycles");
						LastMaintenanceUnit=asset.getLastMaintenanceUnit();
						LastMaintenanceDate=asset.getLastMaintenanceDate();
						rate_planning_end=LastMaintenanceUnit+(p_PlanningHorizon*rate);
						rate_current=LastMaintenanceUnit+rate;
					    while(rate_current<=rate_planning_end) {
					    	unit_current=rate*counter2;

					    	if (unit_current>=unitscycles)
					    	{
			 					MForecastLine fcl =new MForecastLine(getCtx(),0, get_TrxName());
			 					fcl.setAD_Org_ID(pp.getAD_Org_ID());
			 					fcl.setM_Forecast_ID(p_M_Forecast_ID);
								fcl.setM_Product_ID(asset.getM_Product_ID());
								fcl.setM_Warehouse_ID(pp.getM_Warehouse_ID());
								fcl.setQty(Env.ONE);
								fcl.setC_Period_ID(getPeriod_ID(TimeUtil.addDays(LastMaintenanceDate,counter1)));
								fcl.setDatePromised(TimeUtil.addDays(LastMaintenanceDate,counter1));
							//	fcl.set_ValueOfColumn("Description",  "Planning unit="+rate_current);
							//	fcl.set_ValueOfColumn("PP_Product_Planning_ID",  rs.getInt(4));
								fcl.saveEx();   			    	
			 					unit_current=0;counter2=0;
					    	}
								
							rate_current=rate_current+rate;	
					    	counter1++;counter2++;
					      }
						_result+=" Created Forecat Lines for "+asset.getName()+" based Meter Rules!";	
						rate_current=0;	counter1=0;counter2=0;
					}
					
					if (pmrule.equals("L"))
					{
						NextMaintenanceDate =	rs.getTimestamp("NextMaintenenceDate");//getPMRuleTS( p_A_Asset_ID, "NextMaintenenceDate");
						if (NextMaintenanceDate!=null)
						{
						MForecastLine fcl =new MForecastLine(getCtx(),0, get_TrxName());
						fcl.setAD_Org_ID(pp.getAD_Org_ID());
						fcl.setM_Forecast_ID(p_M_Forecast_ID);
						fcl.setM_Product_ID(asset.getM_Product_ID());
						fcl.setM_Warehouse_ID(pp.getM_Warehouse_ID());
						fcl.setQty(Env.ONE);
						fcl.setC_Period_ID(getPeriod_ID(NextMaintenanceDate));
						fcl.setDatePromised(NextMaintenanceDate);
					//	fcl.set_ValueOfColumn("Description", "L");
					//	fcl.set_ValueOfColumn("pp_product_planning_id",  rs.getInt(4));
						fcl.saveEx();
						_result+=" Created Forecat Lines for "+asset.getName()+" based List Dates!";	
						}
					}
					
					
					//-----------
				}
			}
			catch (Exception e)
			{
				log.log (Level.SEVERE, sql_so, e);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; 	pstmt = null;
			}
		
		
        return _result;
	}
	
	public String getPMRuleStr(int  Asset_ID, String Field) 	{
		return DB.getSQLValueString  (get_TrxName(), "SELECT "+Field+" FROM A_Asset_Prev_Maintenance WHERE isActive='Y' AND A_Asset_ID="+Asset_ID);
	}
	
	public int getPMRuleInt(int  Asset_ID, String Field) 	{
		return DB.getSQLValue (get_TrxName(), "SELECT "+Field+" FROM A_Asset_Prev_Maintenance WHERE isActive='Y' AND A_Asset_ID="+Asset_ID);
	}	
	public Timestamp getPMRuleTS(int  Asset_ID, String Field) 	{
		return DB.getSQLValueTS (get_TrxName(), "SELECT "+Field+" FROM A_Asset_Prev_Maintenance WHERE isActive='Y' AND A_Asset_ID="+Asset_ID);
	}	
	
	public int getPeriod_ID(Timestamp date) 	{
		return DB.getSQLValue (get_TrxName(), "SELECT C_Period_ID FROM C_Period WHERE AD_Client_ID="+p_AD_Client_ID+" AND CAST('"+date+"' AS date) BETWEEN CAST(StartDate AS date) and CAST(EndDate AS date);");
	}
}	