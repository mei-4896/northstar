package tech.quantit.northstar.strategy.api.policy.risk;


import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;


/**
 * 当价格超过限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("委托超价限制")
public class PriceExceededRule implements RiskControlRule{

	protected int priceDifTolerance;
	
	private SubmitOrderReqField orderReq;
	
//	@Override
//	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
//		Assert.isTrue(StringUtils.equals(tick.getUnifiedSymbol(), orderReq.getContract().getUnifiedSymbol()), "行情合约与订单不一致");
//		int factor = orderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
//		if(orderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_LimitPrice && factor * (tick.getLastPrice() - orderReq.getPrice()) > priceDifTolerance) {
//			log.info("委托超价限制：限制为{}，期望价为{}，实际价为{}", priceDifTolerance, orderReq.getPrice(), tick.getLastPrice());
//			return RiskAuditResult.REJECTED;
//		}
//		return RiskAuditResult.ACCEPTED;
//	}
//	
//	@Override
//	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
//		this.orderReq = orderReq;
//		return this;
//	}
	
	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.priceDifTolerance = initParams.priceDifTolerance;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超价限制")
		private int priceDifTolerance;
		
	}

}
