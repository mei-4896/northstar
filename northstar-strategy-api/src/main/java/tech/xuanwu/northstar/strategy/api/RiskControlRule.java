package tech.xuanwu.northstar.strategy.api;

import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskControlRule extends DynamicParamsAware, StateChangeListener, AccountAware {

	RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick);
}
