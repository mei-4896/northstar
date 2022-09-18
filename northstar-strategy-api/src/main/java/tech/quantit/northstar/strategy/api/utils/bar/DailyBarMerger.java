package tech.quantit.northstar.strategy.api.utils.bar;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{

	private final int numOfDayPerBar;
	
	private Set<String> tradingDaySet = new HashSet<>();
	
	public DailyBarMerger(int numOfDayPerBar, ContractField bindedContract, Consumer<BarField> callback) {
		super(0, bindedContract, callback);
		this.numOfDayPerBar = numOfDayPerBar;
	}

	@Override
	public void updateBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), bindedContract.getUnifiedSymbol())) {
			return;
		}
		
		if(Objects.nonNull(barBuilder) && tradingDaySet.size() == numOfDayPerBar && !tradingDaySet.contains(bar.getTradingDay())) {
			doGenerate();
		}
		
		tradingDaySet.add(bar.getTradingDay());

		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerger(bar);
	}

	@Override
	protected void doGenerate() {
		callback.accept(barBuilder.build());
		barBuilder = null;
		tradingDaySet.clear();
	}
}