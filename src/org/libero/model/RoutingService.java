/**
 * 
 */
package org.libero.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_AD_WF_Node;
import org.compiere.model.I_AD_Workflow;
import org.compiere.model.I_S_Resource;
import org.compiere.model.MResourceAssignment;
import org.libero.tables.I_PP_Cost_Collector;
import org.libero.tables.I_PP_Order_Node;
/**
 * Rounting(Workflow Service)
 * @author Teo Sarca, www.arhipac.ro
 */
public interface RoutingService
{
	public BigDecimal estimateWorkingTime(I_AD_WF_Node node);
	
	/**
	 * Estimate Activity Working Time for given qty.
	 * Please not that SetupTime or any other times are not considered.
	 * @param node activity
	 * @param qty qty required
	 * @return working time (using Workflow DurationUnit UOM)
	 */
	public BigDecimal estimateWorkingTime(I_PP_Order_Node node, BigDecimal qty);
	
	public BigDecimal estimateWorkingTime(I_PP_Cost_Collector cc);

	/**
	 * Calculate node duration for 1 item, AD_Workflow.DurationUnit UOM will be used
	 * @param node operation
	 * @return node duration for 1 item (AD_Workflow.DurationUnit UOM)
	 */
	//Ferry below code not used public BigDecimal calculateDuration(I_AD_WF_Node node);
	
	/**
	 * Calculate workflow duration for given qty
	 * @param node operation
	 * @return node duration for 1 item (AD_Workflow.DurationUnit UOM)
	 */
	//Ferry not used public BigDecimal calculateDuration(MPPMRP mrp, I_AD_Workflow wf, I_S_Resource plant, BigDecimal qty);

	//-->Ferry
	/**
	 * Calculate workflow duration for given qty
	 * @param node operation
	 * @param DemandDateStartSchedule
	 * @return node duration for 1 item (AD_Workflow.DurationUnit UOM)
	 */
	public BigDecimal calculateDuration(MPPMRP mrp, I_AD_Workflow wf, I_S_Resource plant, BigDecimal qty, Timestamp DemandDateStartSchedule);
	/**
	 * Display plant/resource/machine defined in MfgWF at their duration on the InfoSchedule View
	 * Each new assignment of same resource will be placed subsequent times.
	 * No checking of ResourceType Available Slots i.e. assumed open 24/7 TODO
	 * Thereafter they have to be managed manually or programmatically (future version as MPS)
	 * @param mrp
	 * @param ctx
	 * @param durationRealMinutes
	 * @param node
	 * @param startDateTime
	 * @param finishDateTime
	 * @return
	 */
	 public MResourceAssignment createResourceAssign(MPPMRP mrp,
			final Properties ctx, BigDecimal durationRealMinutes, I_AD_WF_Node node, Timestamp startDateTime, Timestamp finishDateTime);	
	 public long calculateMillisFor(MPPOrderNode node, long commonBase);
	 public long calculateMillisFor(I_AD_WF_Node node, long commonBase, BigDecimal qty);	
	 //<--Ferry
	
	/**
	 * Calculate activity duration based on reported data from Cost Collector.
	 * @param cc cost collector
	 * @return activity duration (using Workflow DurationUnit UOM)
	 */
	//Ferry below code not used public BigDecimal calculateDuration(I_PP_Cost_Collector cc);
	
	/**
	 * Return cost collector base value in resource UOM (e.g. duration)
	 * @param S_Resource_ID resource
	 * @param cc cost collector
	 * @return value (e.g. duration)
	 */
	public BigDecimal getResourceBaseValue(int S_Resource_ID, I_PP_Cost_Collector cc);

	/**
	 * Return node base value in resource UOM (e.g. duration)
	 * @param S_Resource_ID resource
	 * @param node
	 * @return value (e.g. duration)
	 */
	public BigDecimal getResourceBaseValue(int S_Resource_ID, I_AD_WF_Node node);
	
	public Timestamp getStartAssignTime();
}
