package org.idempiere.component;


import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.eevolution.model.MPPProductPlanning;
import org.libero.model.*;

public class MFG_ModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		 if (tableName.equals(MDDNetworkDistribution.Table_Name)) {
		     return MDDNetworkDistribution.class;
		     
		   } else if (tableName.equals(MDDNetworkDistributionLine.Table_Name)) {
				 return MDDNetworkDistributionLine.class;
				     
		   } else if (tableName.equals(MPPCostCollector.Table_Name)) {
				 return MPPCostCollector.class;
				     
		   } else if (tableName.equals(MPPCostCollectorMA.Table_Name)) {
			     return MPPCostCollectorMA.class;
			     
		   } else if (tableName.equals(MPPMRP.Table_Name)) {
			     return MPPMRP.class;
			     
		   } else if (tableName.equals(MPPOrder.Table_Name)) {
			     return MPPOrder.class;
			     
		   } else if (tableName.equals(MPPOrderBOM.Table_Name)) {
			     return MPPOrderBOM.class;
			     
		   } else if (tableName.equals(MPPOrderCost.Table_Name)) {
			     return MPPOrderCost.class;

		   } else if (tableName.equals(MPPOrderBOMLine.Table_Name)) {
			     return MPPOrderBOMLine.class;

		   } else if (tableName.equals(MPPOrderNode.Table_Name)) {
			     return MPPOrderNode.class;
			     
		   } else if (tableName.equals(MPPOrderNodeAsset.Table_Name)) {
			     return MPPOrderNodeAsset.class;
			     
		   } else if (tableName.equals(MPPOrderNodeNext.Table_Name)) {
			     return MPPOrderNodeNext.class;
			     
		   } else if (tableName.equals(MPPOrderNodeProduct.Table_Name)) {
			     return MPPOrderNodeProduct.class;

		   } else if (tableName.equals(MPPOrderWorkflow.Table_Name)) {
			     return MPPOrderWorkflow.class;     
			     
		   } else if (tableName.equals(MPPWFNodeAsset.Table_Name)) {
			     return MPPWFNodeAsset.class;
			     
		   } else if (tableName.equals(MPPWFNodeProduct.Table_Name)) {
			     return MPPWFNodeProduct.class;
			     
		   } else if (tableName.equals(MQMSpecification.Table_Name)) {
			     return MQMSpecification.class;

		   } else if (tableName.equals(MQMSpecificationLine.Table_Name)) {
			     return MQMSpecificationLine.class;

		   } else if (tableName.equals(MPPProductBOM.Table_Name)) {
			     return MPPProductBOM.class;

		   } else if (tableName.equals(MPPProductBOMLine.Table_Name)) {
			     return MPPProductBOMLine.class;
			     
		   } else if (tableName.equals(MPPProductPlanning.Table_Name)) {
			     return MPPProductPlanning.class;

		   } else if (tableName.equals(MDDOrder.Table_Name)) {
			     return MDDOrder.class;

		   } else if (tableName.equals(MDDOrderLine.Table_Name)) {
			     return MDDOrderLine.class;

		   } else if (tableName.equals(MOrder.Table_Name)) {
			     return MOrder.class;
		   } else if (tableName.equals(LiberoMovementLine.Table_Name)){
			   return LiberoMovementLine.class;
		   } else 	   
			   return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		 if (tableName.equals(MDDNetworkDistribution.Table_Name)) {
		     return new MDDNetworkDistribution(Env.getCtx(), Record_ID, trxName);
		     
		   } else if (tableName.equals(MDDNetworkDistributionLine.Table_Name)) {
				 return new MDDNetworkDistributionLine(Env.getCtx(), Record_ID, trxName);
				     
		   } else if (tableName.equals(MPPCostCollector.Table_Name)) {
				 return new MPPCostCollector(Env.getCtx(), Record_ID, trxName);
				     
		   } else if (tableName.equals(MPPCostCollectorMA.Table_Name)) {
			     return new MPPCostCollectorMA(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPMRP.Table_Name)) {
			     return new MPPMRP(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrder.Table_Name)) {
			     return new MPPOrder(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderBOM.Table_Name)) {
			     return new MPPOrderBOM(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderBOMLine.Table_Name)) {
			     return new MPPOrderBOMLine(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderCost.Table_Name)) {
			     return new MPPOrderCost(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MPPOrderNode.Table_Name)) {
			     return new MPPOrderNode(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeAsset.Table_Name)) {
			     return new MPPOrderNodeAsset(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeNext.Table_Name)) {
			     return new MPPOrderNodeNext(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeProduct.Table_Name)) {
			     return new MPPOrderNodeProduct(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MPPOrderWorkflow.Table_Name)) {
			     return new MPPOrderWorkflow(Env.getCtx(), Record_ID, trxName);     
			     
		   } else if (tableName.equals(MPPWFNodeAsset.Table_Name)) {
			     return new MPPWFNodeAsset(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MPPWFNodeProduct.Table_Name)) {
			     return new MPPWFNodeProduct(Env.getCtx(), Record_ID, trxName);
			     
		   } else if (tableName.equals(MQMSpecification.Table_Name)) {
			     return new MQMSpecification(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MQMSpecificationLine.Table_Name)) {
			     return new MQMSpecificationLine(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MPPProductBOM.Table_Name)) {
			     return new MPPProductBOM(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MPPProductBOMLine.Table_Name)) {
			     return new MPPProductBOMLine(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MDDOrder.Table_Name)) {
			     return new MDDOrder(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MDDOrderLine.Table_Name)) {
			     return new MDDOrderLine(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MPPProductPlanning.Table_Name)) {
			     return new MPPProductPlanning(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MOrderLine.Table_Name)) {
			     return new MOrderLine(Env.getCtx(), Record_ID, trxName);

		   } else if (tableName.equals(MOrder.Table_Name)) {
			     return new MOrder(Env.getCtx(), Record_ID, trxName);
		   }else if (tableName.equals(LiberoMovementLine.Table_Name)){
			   return new LiberoMovementLine(Env.getCtx(), Record_ID, trxName);
		   }
		   return null;
	}
	
	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		 if (tableName.equals(MDDNetworkDistribution.Table_Name)) {
		     return new MDDNetworkDistribution(Env.getCtx(), rs, trxName);
		     
		   } else if (tableName.equals(MDDNetworkDistributionLine.Table_Name)) {
				 return new MDDNetworkDistributionLine(Env.getCtx(), rs, trxName);
				     
		   } else if (tableName.equals(MPPCostCollector.Table_Name)) {
				 return new MPPCostCollector(Env.getCtx(), rs, trxName);
				     
		   } else if (tableName.equals(MPPCostCollectorMA.Table_Name)) {
			     return new MPPCostCollectorMA(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPMRP.Table_Name)) {
			     return new MPPMRP(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrder.Table_Name)) {
			     return new MPPOrder(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderBOM.Table_Name)) {
			     return new MPPOrderBOM(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderBOMLine.Table_Name)) {
			     return new MPPOrderBOMLine(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderCost.Table_Name)) {
			     return new MPPOrderCost(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MPPOrderNode.Table_Name)) {
			     return new MPPOrderNode(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeAsset.Table_Name)) {
			     return new MPPOrderNodeAsset(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeNext.Table_Name)) {
			     return new MPPOrderNodeNext(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPOrderNodeProduct.Table_Name)) {
			     return new MPPOrderNodeProduct(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MPPOrderWorkflow.Table_Name)) {
			     return new MPPOrderWorkflow(Env.getCtx(), rs, trxName);     
			     
		   } else if (tableName.equals(MPPWFNodeAsset.Table_Name)) {
			     return new MPPWFNodeAsset(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MPPWFNodeProduct.Table_Name)) {
			     return new MPPWFNodeProduct(Env.getCtx(), rs, trxName);
			     
		   } else if (tableName.equals(MQMSpecification.Table_Name)) {
			     return new MQMSpecification(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MQMSpecificationLine.Table_Name)) {
			     return new MQMSpecificationLine(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MQMSpecificationLine.Table_Name)) {
			     return new MQMSpecificationLine(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MPPProductBOM.Table_Name)) {
			     return new MPPProductBOM(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MPPProductBOMLine.Table_Name)) {
			     return new MPPProductBOMLine(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MDDOrder.Table_Name)) {
			     return new MDDOrder(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MDDOrderLine.Table_Name)) {
			     return new MDDOrderLine(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MPPProductPlanning.Table_Name)) {
			     return new MPPProductPlanning(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MOrderLine.Table_Name)) {
			     return new MOrderLine(Env.getCtx(), rs, trxName);

		   } else if (tableName.equals(MOrder.Table_Name)) {
			     return new MOrder(Env.getCtx(), rs, trxName);

		   }
		   return null;
	}

}
