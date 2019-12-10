/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, iDempiere contributors and Mutlimage SLOVAKIA
 */
package org.libero.model.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_WF_Node;
import org.compiere.model.I_AD_Workflow;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_S_Resource;
import org.compiere.model.MProcess;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.MResourceUnAvailable;
import org.compiere.model.MUOM;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Workflow;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFProcess;
import org.compiere.wf.MWorkflow;
import org.libero.bom.drop.ISupportRadioNode;
import org.libero.exceptions.CRPException;
import org.libero.model.MPPMRP;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderNode;
import org.libero.model.MPPOrderWorkflow;
import org.libero.model.RoutingService;
import org.libero.model.RoutingServiceFactory;
import org.libero.model.reasoner.CRPReasoner;
import org.libero.process.CRP;
import org.libero.tables.I_PP_Cost_Collector;
import org.libero.tables.I_PP_Order_Node;

/**
 * Default Routing Service Implementation
 * @author Teo Sarca
 */
public class DefaultRoutingServiceImpl implements RoutingService
{
	private final CLogger log = CLogger.getCLogger(getClass());
	
	private Timestamp startAssignTime;
	
	public BigDecimal estimateWorkingTime(I_AD_WF_Node node)
	{
		final double duration;
		if (node.getUnitsCycles().signum() == 0)
		{
			duration = node.getDuration();
		}
		else
		{
			duration = node.getDuration() / node.getUnitsCycles().doubleValue();
		}
		return BigDecimal.valueOf(duration);
	}
	public BigDecimal estimateWorkingTime(I_PP_Order_Node node, BigDecimal qty)
	{
		double unitDuration = node.getDuration();
		double cycles = calculateCycles(node.getUnitsCycles(), qty);
		BigDecimal duration = BigDecimal.valueOf(unitDuration * cycles);
		return duration;
	}
	//-->Ferry
	public BigDecimal estimateWorkingTime(I_AD_WF_Node node, BigDecimal qty)
	{
		double unitDuration = node.getDuration();
		double cycles = calculateCycles(node.getUnitsCycles().intValue(), qty);
		BigDecimal duration = BigDecimal.valueOf(unitDuration * cycles);
		return duration;
	}	
	//<--Ferry
	
	public BigDecimal estimateWorkingTime(I_PP_Cost_Collector cc)
	{
		final String trxName = (cc instanceof PO ? ((PO)cc).get_TrxName() : null);
		final BigDecimal qty = cc.getMovementQty();
		MPPOrderNode node = MPPOrderNode.get(Env.getCtx(), cc.getPP_Order_Node_ID(), trxName);
		return estimateWorkingTime(node, qty);
	}

	
	/**
	 * Calculate how many cycles are needed for given qty and units per cycle
	 * @param unitsCycle
	 * @param qty
	 * @return number of cycles
	 */
	protected int calculateCycles(int unitsCycle, BigDecimal qty)
	{
		BigDecimal cycles = qty;
		BigDecimal unitsCycleBD = BigDecimal.valueOf(unitsCycle);
		if (unitsCycleBD.signum() > 0)
		{
			cycles = qty.divide(unitsCycleBD, 0, RoundingMode.UP);
		}
		return cycles.intValue();
	}
	
	/**
	 * Calculate node duration in DurationUnit UOM (see AD_Workflow.DurationUnit)
	 * @param node
	 * @param setupTime setup time (workflow duration unit)
	 * @param durationTotal (workflow duration unit)
	 * @reeturn duration
	 */
	protected BigDecimal calculateDuration(I_AD_WF_Node node, I_PP_Cost_Collector cc)
	{
		if (node == null)
		{
			node = cc.getPP_Order_Node().getAD_WF_Node();
		}
		//-->
		if (node == null) {
			throw new AdempiereException("calculateDuration not supported using Node null!!!");
		}
		//<--
		final I_AD_Workflow workflow = node.getAD_Workflow();
		final double batchSize = workflow.getQtyBatchSize().doubleValue();
		double setupTime; //Ferry final double setupTime;
		//-->Ferry
		double totalDuration;
		BigDecimal batchS = Env.ONE;
		double queuingTime = 0;
		double waitingTime = 0;
		double movingTime = 0;
		if ( node != null) {
			queuingTime = node.getQueuingTime();
			waitingTime += node.getWaitingTime();
			movingTime += node.getMovingTime();
		}
		//<--Ferry
		
		final double duration; 
		if (cc != null) //Ferry all duration for Cost Collector
		{
			setupTime = cc.getSetupTimeReal().doubleValue();
			duration = cc.getDurationReal().doubleValue();
			batchS = cc.getPP_Order().getQtyBatchs(); //Ferry
			//Ferry setupTime duration is real on cost collector
			if (batchSize > 1) 
				totalDuration = (( queuingTime + waitingTime + movingTime ) * batchS.doubleValue()) + setupTime + duration;
			else
				totalDuration = (( queuingTime + waitingTime + movingTime )) + setupTime + duration;	
		}
		else //Ferry duration for 1 unit final
		{
			setupTime = node.getSetupTime();
			// Estimate total duration for 1 unit of final product as duration / units cycles
			duration = estimateWorkingTime(node).doubleValue(); 
			//-->Ferry, not supported because can not count total duration based on object node
			//throw new AdempiereException("calculateDuration not supported using Node !!!");
			if (batchSize > 1) 
				totalDuration = ((setupTime + queuingTime + waitingTime + movingTime) / batchSize) + duration;			
			else
				totalDuration = (setupTime + queuingTime + waitingTime + movingTime) + duration;
			//<--Ferry
		}
		
		/*double totalDuration;
		if(batchSize > 0)
			totalDuration = ((setupTime / batchSize) + 
							 (queuingTime / batchSize) + 
							 (waitingTime / batchSize) + 
							 (movingTime / batchSize) +  
							 duration); //Ferry totalDuration = ((setupTime / batchSize) + duration);
		else
			totalDuration = setupTime  + queuingTime + waitingTime + movingTime + duration; *///Ferry totalDuration = setupTime  + duration;
		
		return BigDecimal.valueOf(totalDuration);
	}
	//Ferry below code not used
	/*
	public BigDecimal calculateDuration(I_AD_WF_Node node)
	{
		return calculateDuration(node, null);
	}
	public BigDecimal calculateDuration(I_PP_Cost_Collector cc)
	{
		return calculateDuration(getAD_WF_Node(cc), cc);
	}*/

	/** 
	 * red1 - to set ResourceAssignment same time and pass values Start/End TimeStamp
	 *  also each node has its own Resource! and later product can also have WF!
	 *  Duration Unit must synch with Resource base time unit (min/hour/day/week).
	 *  Result in minutes(including plant day off)
	 */
	//Ferry below code not used
	/*public BigDecimal calculateDuration(MPPMRP mrp, I_AD_Workflow wf, I_S_Resource plant, BigDecimal qty)
	{
		if (plant == null)
			return Env.ZERO;
		final Properties ctx = mrp.getCtx();
		//final MResourceType S_ResourceType = MResourceType.get(ctx, plant.getS_ResourceType_ID());  	

		Double nodeTotal = 0.0;
		double durationTotal = 0.0; 
		BigDecimal duration = Env.ZERO;
		MWFNode[] nodes = ((MWorkflow)wf).getNodes(false, Env.getAD_Client_ID(ctx));
		//going thru each node that has own resource to accumulate its duration and trigger its Activity and Schedule booking
		for (I_AD_WF_Node node : nodes)
		{
			//get the Node's Resource, then calculate the duration of its use
			MResourceType S_ResourceType = MResourceType.get(ctx, plant.getS_ResourceType_ID());
			BigDecimal AvailableDayTime  = BigDecimal.valueOf(S_ResourceType.getTimeSlotHours());
			int AvailableDays = S_ResourceType.getAvailableDaysWeek();
			double durationBaseSec = getDurationBaseSec(wf.getDurationUnit());// 1 hour = 3600 seconds

			// Qty independent times:
			nodeTotal += node.getQueuingTime();
			nodeTotal += node.getSetupTime();
			nodeTotal += node.getWaitingTime();
			nodeTotal += node.getMovingTime();
			
			// Get OverlapUnits - number of units that must be completed before they are moved the next activity 
			double overlapUnits = qty.doubleValue();
			if (node.getOverlapUnits() > 0 && node.getOverlapUnits() < overlapUnits)
			{
				overlapUnits = node.getOverlapUnits();
			}
			double durationBeforeOverlap = node.getDuration() * overlapUnits;
			durationTotal = nodeTotal;
			durationTotal += durationBeforeOverlap;
			
			//red1 do Activity thread
			createWFActivity(mrp, wf);
			
			BigDecimal requiredTime = BigDecimal.valueOf(durationTotal * durationBaseSec / 60);// removed / 60 to retain minutes for adding later.
			// TODO: implement here, Victor's suggestion - https://sourceforge.net/forum/message.php?msg_id=5179460
			//  " : cannot find the above thread.

			// Weekly Factor  	-- red1 made adjustments according to my understanding that all in minutes base
			BigDecimal DayTime = BigDecimal.valueOf(24); // red1 done this as a factor instead of a divisor
			AvailableDayTime = AvailableDayTime.divide(DayTime);// red1 done this as a factor instead of a divisor
			BigDecimal WeeklyFactor = BigDecimal.valueOf(7).divide(BigDecimal.valueOf(AvailableDays), 8, RoundingMode.UP);
			duration = (requiredTime.multiply(WeeklyFactor)).divide(AvailableDayTime, 0, RoundingMode.UP); //
			
			MResourceAssignment ra = createResourceAssign(mrp, ctx, duration,
					node);
			
			//set start end times to MRP
			mrp.setDateStartSchedule(startAssignTime);
			mrp.setDateFinishSchedule(ra.getAssignDateTo());
			mrp.saveEx(mrp.get_TrxName());
			//red1 -- end
		} 
		return duration;
	}*/
	/**
	 * Ferry+param Timestamp DemandDateStartSchedule
	 * red1 - to set ResourceAssignment same time and pass values Start/End TimeStamp
	 *  also each node has its own Resource! and later product can also have WF!
	 *  Duration Unit must synch with Resource base time unit (min/hour/day/week).
	 *  Result in minutes(including plant day off)
	 */
	public BigDecimal calculateDuration(MPPMRP mrp, I_AD_Workflow wf, I_S_Resource plant, BigDecimal qty, Timestamp DemandDateStartSchedule)
	{
		if (plant == null)
			return Env.ZERO;		
		
		final Properties ctx = mrp.getCtx();
		//final MResourceType S_ResourceType = MResourceType.get(ctx, plant.getS_ResourceType_ID());  	

		final CRPReasoner reasoner = new CRPReasoner();
		
		BigDecimal duration = Env.ZERO;
		//-->Ferry, nodes must be processed descending
		//MWFNode[] nodes = ((MWorkflow)wf).getNodes(false, Env.getAD_Client_ID(ctx));
		MWFNode[] nodes = ((MWorkflow)wf).getNodes(true, Env.getAD_Client_ID(ctx));
		Timestamp counter = DemandDateStartSchedule; //DemandDateStartSchedule from MRP Info or Calculate Material Plan
		final CRP crp = new CRP();
		//Reverse ordering
		ArrayList<MWFNode> list = new ArrayList<MWFNode>();
		for (int n = (nodes.length - 1); n >= 0; n--) 
			list.add(nodes[n]);
		list.toArray(nodes);
			
		final ArrayList<Integer> visitedNodes = new ArrayList<Integer>();
		//<--
		//going thru each node that has own resource to accumulate its duration and trigger its Activity and Schedule booking
		for (I_AD_WF_Node node : nodes)
		{
			int nodeId = node.getAD_WF_Node_ID();
			if (visitedNodes.contains(nodeId))
			{
				throw new CRPException("Cyclic transition found " + node.getName());
			}
			
			visitedNodes.add(nodeId);
			log.info("PP_Order Node:" + node.getName() != null ? node.getName() : ""  + " Description:" + node.getDescription() != null ? node.getDescription() : "");

			MResource resource = MResource.get(ctx, node.getS_Resource_ID());
			// Skip this node if there is no resource
			if(resource == null)
			{						
				continue;
			}			
			
			if(!reasoner.isAvailable(resource))
			{
				throw new CRPException("@ResourceNotInSlotDay@").setS_Resource(resource);
			}
			
			long nodeMillis = calculateMillisFor(node, getDurationBaseSec(wf.getDurationUnit()), qty);
			Timestamp dateStart = crp.scheduleBackward(counter, nodeMillis ,resource, Env.getAD_Client_ID(ctx));
			

			//red1 do Activity thread
			createWFActivity(mrp, wf);
			
			BigDecimal durationRealThisNode = BigDecimal.valueOf(nodeMillis / 1000 / 60); //in minutes		
			BigDecimal durationThisNodeBG =  BigDecimal.valueOf((counter.getTime() - dateStart.getTime()) / 1000 / 60);//in minutes 
			
			//MResourceAssignment ra = createResourceAssign(mrp, ctx, duration,node);
			MResourceAssignment ra = createResourceAssign(mrp, ctx, durationThisNodeBG, node, dateStart, durationRealThisNode);
			duration = duration.add(durationThisNodeBG);
			//<--
			
			//set start end times to MRP
			mrp.setDateStartSchedule(dateStart);
			mrp.setDateFinishSchedule(DemandDateStartSchedule);		//Ferry mrp.setDateFinishSchedule(ra.getAssignDateTo());
			mrp.saveEx(mrp.get_TrxName());
			//red1 -- end
			
			counter = dateStart;
		} 
		return duration;
	}
	/**
	 * Calculate how many millis take to complete given qty on given node(operation).
	 * @param node operation
	 * @param commonBase multiplier to convert duration to seconds 
	 * @return duration in millis
	 */
	//private 
	public long calculateMillisFor(MPPOrderNode node, long commonBase)
	{
		final BigDecimal qty = node.getQtyToDeliver();
		// Total duration of workflow node (seconds) ...
		// ... its static single parts ...
		long totalDuration =
				+ node.getQueuingTime() 
				+ node.getSetupTime() 		//Ferry + node.getSetupTimeRequired() // Use the present required setup time to notice later changes  
				+ node.getMovingTime() 
				+ node.getWaitingTime()
		;
		// ... and its qty dependend working time ... (Use the present required duration time to notice later changes)
		final BigDecimal workingTime = estimateWorkingTime(node, qty);
		//-->Ferry considerinq QtyBatchSize
		final BigDecimal qtyBatchSize = node.getPP_Order_Workflow().getQtyBatchSize();
		if (qtyBatchSize.compareTo(Env.ONE) == 1 ) {
			final BigDecimal qtyBatchs = qty.divide(qtyBatchSize , 0, BigDecimal.ROUND_UP); 
			totalDuration = totalDuration * qtyBatchs.longValue();
		}
		//<--
		totalDuration += workingTime.doubleValue();
		
		// Returns the total duration of a node in milliseconds.
		return (long)(totalDuration * commonBase * 1000);
	}
	
	/**
	 * Calculate how many millis take to complete given qty on given node(operation).
	 * @param node operation
	 * @param commonBase multiplier to convert duration to seconds 
	 * @param qty
	 * @return duration in millis
	 */
	//private 
	public long calculateMillisFor(I_AD_WF_Node node, long commonBase, BigDecimal qty)
	{
		// Total duration of workflow node (seconds) ...
		// ... its static single parts ...
		long totalDuration =
				+ node.getQueuingTime() 
				+ node.getSetupTime() // Use the present required setup time to notice later changes  
				+ node.getMovingTime() 
				+ node.getWaitingTime()
		;
		// ... and its qty dependend working time ... (Use the present required duration time to notice later changes)
		final BigDecimal workingTime = estimateWorkingTime(node, qty);
		//-->Ferry considerinq QtyBatchSize
		final BigDecimal qtyBatchSize = node.getWorkflow().getQtyBatchSize();
		if (qtyBatchSize.compareTo(Env.ONE) == 1 ) {
			final BigDecimal qtyBatchs = qty.divide(qtyBatchSize , 0, BigDecimal.ROUND_UP); 
			totalDuration = totalDuration * qtyBatchs.longValue();
		}
		//<--		
		totalDuration += workingTime.doubleValue();
		
		// Returns the total duration of a node in milliseconds.
		return (long)(totalDuration * commonBase * 1000);
	}	
	//<--
	/**
	 * Display plant/resource/machine defined in MfgWF at their duration on the InfoSchedule View
	 * Each new assignment of same resource will be placed subsequent times.
	 * No checking of ResourceType Available Slots i.e. assumed open 24/7 TODO
	 * Thereafter they have to be managed manually or programmatically (future version as MPS)
	 * @param mrp
	 * @param ctx
	 * @param duration
	 * @param node
	 * @return
	 */
	private MResourceAssignment createResourceAssign(MPPMRP mrp,
			final Properties ctx, BigDecimal duration, I_AD_WF_Node node) {
		//red1 -- set ResourceAssignment.. first get lastAssignTime
		MResourceAssignment resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=?", null)
		.setParameters(node.getS_Resource_ID()) 
		.first();
		MResourceAssignment ra;				
		java.util.Date date= new java.util.Date();
		startAssignTime = new Timestamp(date.getTime());
		if (resourceschedule!=null){
			if (resourceschedule.getName().equals("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo())) {
				ra = resourceschedule;
			} else if (resourceschedule.getName().equals("MRP:"+mrp.get_ID()+" MO:"+mrp.getPP_Order().getDocumentNo())) {
				ra = resourceschedule;
			} else {	
				startAssignTime = resourceschedule.getAssignDateTo();
				ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
			}			
		}
		else {
			ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
		}
		ra.setAD_Org_ID(mrp.getAD_Org_ID()); 
		ra.setName("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo());
		ra.setAssignDateFrom(startAssignTime);
		ra.setAssignDateTo(TimeUtil.addMinutess(startAssignTime, duration.intValueExact())); 
		//TODO red1 in future to check ResourceType.isDayAvailable and move balance after.
		ra.setS_Resource_ID(node.getS_Resource_ID());
		ra.setDescription(mrp.getC_OrderLine().getM_Product().getName()+" "+mrp.getC_OrderLine().getQtyOrdered());
		ra.saveEx(mrp.get_TrxName());
		return ra;
	}
	//-->Ferry
	/**
	 * Display plant/resource/machine defined in MfgWF at their duration on the InfoSchedule View
	 * Each new assignment of same resource will be placed subsequent times.
	 * No checking of ResourceType Available Slots i.e. assumed open 24/7 TODO
	 * Thereafter they have to be managed manually or programmatically (future version as MPS)
	 * @param mrp
	 * @param ctx
	 * @param duration
	 * @param node
	 * @param durationRealMinutes
	 * @return
	 */
	 private MResourceAssignment createResourceAssign(MPPMRP mrp,
			final Properties ctx, BigDecimal duration, I_AD_WF_Node node, Timestamp startDateTime, BigDecimal durationRealMinutes) {
		//red1 -- set ResourceAssignment.. first get lastAssignTime
		//-->Ferry
		/*MResourceAssignment resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=?", null)
		.setParameters(node.getS_Resource_ID()) 
		.first();*/
		String m_name;
		m_name = "MRP:"+mrp.get_ID();
		if (mrp.getC_Order().getDocumentNo() != null) {
			m_name = m_name.concat(" Order:"+mrp.getC_Order().getDocumentNo())  ;
		} else if (mrp.getPP_Order().getDocumentNo() != null) {
			m_name = m_name.concat(" MO:"+mrp.getPP_Order().getDocumentNo())  ;			
		}
		MResourceAssignment resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=? AND " + MResourceAssignment.COLUMNNAME_Name+"=?", null)
		.setParameters(new Object[]{node.getS_Resource_ID(), m_name})
		.first();
		//<--
		MResourceAssignment ra;				
		//-->Ferry
		//java.util.Date date= new java.util.Date();
		//startAssignTime = new Timestamp(date.getTime());
		startAssignTime = new Timestamp(startDateTime.getTime());
		//<--Ferry
		if (resourceschedule!=null){
			if (resourceschedule.getName().equals(m_name)) {		// Ferry (resourceschedule.getName().equals("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo())){
				ra = resourceschedule;
			}					
			else{	
				ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
			}			
		}
		else {
			ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
		}
		ra.setAD_Org_ID(mrp.getAD_Org_ID()); 
		ra.setName(m_name);			//Ferry ra.setName("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo());
		ra.setAssignDateFrom(startAssignTime);
		ra.setAssignDateTo(TimeUtil.addMinutess(startAssignTime, duration.intValueExact())); 
		//TODO red1 in future to check ResourceType.isDayAvailable and move balance after.
		ra.setS_Resource_ID(node.getS_Resource_ID());
		//-->Ferry
		//ra.setDescription(mrp.getC_OrderLine().getM_Product().getName()+" "+mrp.getC_OrderLine().getQtyOrdered());
		if (mrp.getC_OrderLine().getM_Product() != null && mrp.getC_OrderLine().getQtyOrdered().compareTo(Env.ZERO) != 0) {
			ra.setDescription(mrp.getC_OrderLine().getM_Product().getName()+" "+mrp.getC_OrderLine().getQtyOrdered());
		}
		else {
			ra.setDescription(mrp.getM_Product().getName()+" "+mrp.getQty());
		}
		
		//UOM resource_type
		String baseUOMResource = node.getS_Resource().getS_ResourceType().getC_UOM().getUOMSymbol().trim();
		double durationBaseSec = getDurationBaseSec(baseUOMResource);// 1 hour = 3600 seconds
		if (durationBaseSec == 0) {
			throw new AdempiereException("@NotSupported@ @C_UOM_ID@ - "+baseUOMResource);
		}
		BigDecimal durationRealUOM =   durationRealMinutes.multiply(BigDecimal.valueOf(60)).divide(BigDecimal.valueOf(durationBaseSec), 8, RoundingMode.UP); //Ferry
		ra.setQty(durationRealUOM);
		//<--
		ra.saveEx(mrp.get_TrxName());
		return ra;
	}	
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
			 final Properties ctx, BigDecimal durationRealMinutes, I_AD_WF_Node node, Timestamp startDateTime, Timestamp finishDateTime) {
		 Timestamp startAssignTime;
		 //red1 -- set ResourceAssignment.. first get lastAssignTime
		 //-->Ferry
		 /*MResourceAssignment resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=?", null)
			.setParameters(node.getS_Resource_ID()) 
			.first();*/
		 String m_name;
		 String m_prefix;
		 m_name = "MRP:"+mrp.get_ID();
		 m_prefix = m_name;
		 if (mrp.getPP_Order().getDocumentNo() != null && mrp.getC_Order().getDocumentNo() != null) {
			 m_name = m_name.concat(" MO:"+mrp.getPP_Order().getDocumentNo()+" Order:"+mrp.getC_Order().getDocumentNo())  ;
		 } else if (mrp.getC_Order().getDocumentNo() != null) {
			 m_name = m_name.concat(" Order:"+mrp.getC_Order().getDocumentNo())  ;
		 } else if (mrp.getPP_Order().getDocumentNo() != null) {
			 m_name = m_name.concat(" MO:"+mrp.getPP_Order().getDocumentNo())  ;			
		 }
		 MResourceAssignment resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=? AND " + MResourceAssignment.COLUMNNAME_Name+"=?", null)
				 .setParameters(new Object[]{node.getS_Resource_ID(), m_name})
				 .first();
		 if (resourceschedule == null) {
			 resourceschedule = new Query(Env.getCtx(), MResourceAssignment.Table_Name, MResourceAssignment.COLUMNNAME_S_Resource_ID+"=? AND " + MResourceAssignment.COLUMNNAME_Name+" like ?", null)
					 .setParameters(new Object[]{node.getS_Resource_ID(), m_prefix+'%'})
					 .first();
		 }
		 //<--
		 MResourceAssignment ra;				
		 //-->Ferry
		 //java.util.Date date= new java.util.Date();
		 //startAssignTime = new Timestamp(date.getTime());
		 startAssignTime = new Timestamp(startDateTime.getTime());
		 //<--Ferry
		 if (resourceschedule!=null){
			 if (resourceschedule.getName().startsWith(m_prefix)) {		// Ferry resourceschedule.getName().equals(m_name)		(resourceschedule.getName().equals("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo())){
				 ra = resourceschedule;
			 }					
			 else{	
				 ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
			 }			
		 }
		 else {
			 ra = new MResourceAssignment(ctx, 0, mrp.get_TrxName());
		 }
		 ra.setAD_Org_ID(mrp.getAD_Org_ID()); 
		 ra.setName(m_name);			//Ferry ra.setName("MRP:"+mrp.get_ID()+" Order:"+mrp.getC_Order().getDocumentNo());
		 ra.setAssignDateFrom(startAssignTime);
		 ra.setAssignDateTo(finishDateTime);			//ra.setAssignDateTo(TimeUtil.addMinutess(startAssignTime, duration.intValueExact())); 
		 //TODO red1 in future to check ResourceType.isDayAvailable and move balance after.
		 ra.setS_Resource_ID(node.getS_Resource_ID());
		 //-->Ferry
		 //ra.setDescription(mrp.getC_OrderLine().getM_Product().getName()+" "+mrp.getC_OrderLine().getQtyOrdered());
		 if (mrp.getC_OrderLine().getM_Product() != null && mrp.getC_OrderLine().getQtyOrdered().compareTo(Env.ZERO) != 0) {
			 ra.setDescription(mrp.getC_OrderLine().getM_Product().getName()+" "+mrp.getC_OrderLine().getQtyOrdered());
		 }
		 else {
			 ra.setDescription(mrp.getM_Product().getName()+" "+mrp.getQty());
		 }

		 //UOM resource_type
		 String baseUOMResource = node.getS_Resource().getS_ResourceType().getC_UOM().getUOMSymbol().trim();
		 double durationBaseSec = getDurationBaseSec(baseUOMResource);// 1 hour = 3600 seconds
		 if (durationBaseSec == 0) {
			 throw new AdempiereException("@NotSupported@ @C_UOM_ID@ - "+baseUOMResource);
		 }
		 BigDecimal durationRealUOM =   durationRealMinutes.multiply(BigDecimal.valueOf(60)).divide(BigDecimal.valueOf(durationBaseSec), 8, RoundingMode.UP); //FERRY
		 ra.setQty(durationRealUOM);
		 //<--Ferry
		 ra.saveEx(mrp.get_TrxName());
		 return ra;
	 }		 
	//<--
	/**
	 * Only first level nodes WFActivity created as main. Processing them should trigger the rest
	 * acting as visual guide to shopfloor operations what WFActivities are outstanding.
	 * For what resources/plants/machine are in use in respective Production Schedule, refer createResourceAssignment
	 * Processing WFActivity will refer only attached PPMRP.processIt(). 
	 * No impact with PP_Order, S_ResourceAssignment or MRP data.
	 * @param mrp
	 * @param wf
	 */
	private void createWFActivity(MPPMRP mrp, I_AD_Workflow wf) {
		//WF must have Action=UserWindow, Window=MRP, Table hardcoded below. WFState=OS-Suspended for it to appear in Dashboard
		if (wf != null)
			try { 
				int Record_ID = mrp.get_ID(); 
				MWFActivity act = new Query(Env.getCtx(),MWFActivity.Table_Name,MWFActivity.COLUMNNAME_Record_ID+"=?",mrp.get_TrxName())
				.setParameters(Record_ID)
				.first();
				if (act!=null) {
					if (act.getWFState().equals(MWFActivity.WFSTATE_Suspended))
						act.delete(true); //red1 delete similar thread before update
					else log.severe("Workflow Activity Was Created and Processed Before This!"); 
				}
				int Table_ID = MPPOrder.Table_ID; 
				//process MFG_WF_Activity is pre-defined for this specific purpose
				int AD_Process_ID = MProcess.getProcess_ID("MFG_WF_Activity", mrp.get_TrxName());									
				//hard set MRP ID to WF
				PO po = mrp;
				wf.setAD_Table_ID(po.get_Table_ID());
				//create WFProcess
				ProcessInfo pi = new ProcessInfo(wf.getName(), AD_Process_ID, Table_ID, Record_ID);		
				pi.setTransactionName(mrp.get_TrxName());;
				pi.setPO(po);
				MWFProcess wfProcess = new MWFProcess((MWorkflow) wf, pi, mrp.get_TrxName());			
				wfProcess.startWork();
						 
			} catch (Exception e) {
				log.warning("Workflow Activity failed to work");
		}
	}

	protected BigDecimal convertDurationToResourceUOM(BigDecimal duration, int S_Resource_ID, I_AD_WF_Node node)
	{
		MResource resource = MResource.get(Env.getCtx(), S_Resource_ID);
		I_AD_Workflow wf = MWorkflow.get(Env.getCtx(), node.getAD_Workflow_ID());
		I_C_UOM resourceUOM = MUOM.get(Env.getCtx(), resource.getC_UOM_ID());
		return convertDuration(duration, wf.getDurationUnit(), resourceUOM);
	}
	
	@Override
	public BigDecimal getResourceBaseValue(int S_Resource_ID, I_PP_Cost_Collector cc)
	{
		return getResourceBaseValue(S_Resource_ID, null, cc);
	}
	@Override
	public BigDecimal getResourceBaseValue(int S_Resource_ID, I_AD_WF_Node node)
	{
		return getResourceBaseValue(S_Resource_ID, node, null);
	}
	protected BigDecimal getResourceBaseValue(int S_Resource_ID, I_AD_WF_Node node, I_PP_Cost_Collector cc)
	{
		if (node == null)
			node = cc.getPP_Order_Node().getAD_WF_Node();
		final Properties ctx = (node instanceof PO ? ((PO)node).getCtx() : Env.getCtx());
		final MResource resource = MResource.get(ctx, S_Resource_ID);
		final MUOM resourceUOM = MUOM.get(ctx, resource.getC_UOM_ID());
		//
		if (isTime(resourceUOM))
		{
			BigDecimal duration = calculateDuration(node, cc);
			I_AD_Workflow wf = MWorkflow.get(ctx, node.getAD_Workflow_ID());
			BigDecimal convertedDuration = convertDuration(duration, wf.getDurationUnit(), resourceUOM);
			return convertedDuration;
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @C_UOM_ID@ - "+resourceUOM);
		}
	}

	protected I_AD_WF_Node getAD_WF_Node(I_PP_Cost_Collector cc)
	{
		I_PP_Order_Node activity = cc.getPP_Order_Node();
		return activity.getAD_WF_Node();
	}
	
	/**
	 * Convert durationUnit to seconds
	 * @param durationUnit
	 * @return duration in seconds
	 */
	public long getDurationBaseSec (String durationUnit)
	{
		if (durationUnit == null)
			return 0;
		else if (X_AD_Workflow.DURATIONUNIT_Second.equals(durationUnit))
			return 1;
		else if (X_AD_Workflow.DURATIONUNIT_Minute.equals(durationUnit))
			return 60;
		else if (X_AD_Workflow.DURATIONUNIT_Hour.equals(durationUnit))
			return 3600;
		else if (X_AD_Workflow.DURATIONUNIT_Day.equals(durationUnit))
			return 86400;
		else if (X_AD_Workflow.DURATIONUNIT_Month.equals(durationUnit))
			return 2592000;
		else if (X_AD_Workflow.DURATIONUNIT_Year.equals(durationUnit))
			return 31536000;
		return 0;
	}	//	getDurationSec
	
	/**
	 * Convert uom to seconds
	 * @param uom time UOM 
	 * @return duration in seconds
	 * @throws AdempiereException if UOM is not supported
	 */
	public long getDurationBaseSec(I_C_UOM uom)
	{
		MUOM uomImpl = (MUOM)uom;
		//
		if(uomImpl.isWeek())
		{
			return 60*60*24*7;
		}
		if(uomImpl.isDay())
		{
			return 60*60*24;
		}
		else if (uomImpl.isHour())
		{
			return 60*60;
		}
		else if (uomImpl.isMinute())
		{
			return 60;
		}
		else if (uomImpl.isSecond())
		{
			return 1;
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @C_UOM_ID@="+uom.getName());
		}
	}
	
	/**
	 * Check if it's an UOM that measures time 
	 * @param uom 
	 * @return true if is time UOM
	 */
	public boolean isTime(I_C_UOM uom)
	{
		String x12de355 = uom.getX12DE355();
		return MUOM.X12_SECOND.equals(x12de355)
		|| MUOM.X12_MINUTE.equals(x12de355)
		|| MUOM.X12_HOUR.equals(x12de355)
		|| MUOM.X12_DAY.equals(x12de355)
		|| MUOM.X12_DAY_WORK.equals(x12de355)
		|| MUOM.X12_WEEK.equals(x12de355)
		|| MUOM.X12_MONTH.equals(x12de355)
		|| MUOM.X12_MONTH_WORK.equals(x12de355)
		|| MUOM.X12_YEAR.equals(x12de355)
		;
	}
	
	/**
	 * Convert duration from given UOM to given UOM
	 * @param duration
	 * @param fromDurationUnit duration UOM
	 * @param toUOM target UOM
	 * @return duration converted to toUOM
	 */
	public BigDecimal convertDuration(BigDecimal duration, String fromDurationUnit, I_C_UOM toUOM)
	{
		double fromMult = getDurationBaseSec(fromDurationUnit);
		double toDiv = getDurationBaseSec(toUOM);
		BigDecimal convertedDuration = BigDecimal.valueOf(duration.doubleValue() * fromMult / toDiv);
		// Adjust scale to UOM precision
/*		int precision = toUOM.getStdPrecision();
		if (convertedDuration.scale() > precision)
			convertedDuration = convertedDuration.setScale(precision, RoundingMode.HALF_UP);*/
		//
		return convertedDuration;
	}
	@Override
	public Timestamp getStartAssignTime(){
		return startAssignTime;
	}
}
