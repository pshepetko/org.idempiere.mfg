package org.libero.process.eam;

import org.compiere.model.MAsset;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import java.sql.Timestamp;
import java.util.logging.Level;


/**
 *	Process AddMeter4eAM
 *	
 *  @author Peter Shepetko
 *  @version $Id: AddMeter4eAM.java,v 1.0 2017/12/20 pshepetko Exp $
 *  
 * 
 */
public class AddMeter4eAM extends SvrProcess
{
 	private int p_AD_Client_ID= Env.getAD_Client_ID(Env.getCtx());
 	private int p_AD_User_ID= Env.getAD_User_ID(Env.getCtx());
	private int p_A_Asset_ID=0;
	private Timestamp p_DateValue = null;
	private int p_UnitsCycles=0;

	
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
			else if (name.equals("A_Asset_ID"))
			{
				p_A_Asset_ID = para.getParameterAsInt();
			}
			else if (name.equals("DateValue"))
			{
				p_DateValue = para.getParameterAsTimestamp();
			}
			else if (name.equals("UnitsCycles"))
			{
				p_UnitsCycles = para.getParameterAsInt();
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
		if (!getPMRuleStr(p_A_Asset_ID, "Asset_Prev_Maintenance_Rule").equals("M"))  return "Asset don't have a Meter rule!";
		String _result=null;
		MAsset asset = new MAsset(getCtx(),p_A_Asset_ID, get_TrxName());
		asset.setUseUnits(p_UnitsCycles);
		asset.saveEx();
//		_result= asset.getName()+" Last Date="+asset.getLastMaintenanceDate()+"|Last Unit="+asset.getLastMaintenanceUnit();
        return _result;
	}
	
	public String getPMRuleStr(int  Asset_ID, String Field) 	{
		return DB.getSQLValueString  (get_TrxName(), "SELECT "+Field+" FROM A_Asset_Prev_Maintenance WHERE isActive='Y' AND A_Asset_ID="+Asset_ID);
	}
			
}	