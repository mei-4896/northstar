package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.HHV;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.LLV;

import java.util.function.Function;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * 波浪指标
 * @author KevinHuangwl
 *
 */
public class WAVE {

	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	
	/**
	 * 威廉波浪计算函数
	 * @param n
	 * @param m
	 * @return	
	 */
	public static Function<BarWrapper, TimeSeriesValue> wr(int n, int m){
		final LWR lwr = LWR.of(n, m, m);
		final Function<BarWrapper, TimeSeriesValue> fast = lwr.fast();
		final Function<BarWrapper, TimeSeriesValue> slow = lwr.slow();
		int ref = m * 2;
		final TimeSeriesUnaryOperator barllv = LLV(ref);
		final TimeSeriesUnaryOperator barhhv = HHV(ref);
		final TimeSeriesUnaryOperator fastllv = LLV(ref);
		final TimeSeriesUnaryOperator fasthhv = HHV(ref);
		final AtomicDouble lastFast = new AtomicDouble();
		final AtomicDouble lastSlow = new AtomicDouble();
		return bar -> {
			TimeSeriesValue barllvV = barllv.apply(new TimeSeriesValue(bar.getBar().getLowPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue barhhvV = barhhv.apply(new TimeSeriesValue(bar.getBar().getHighPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue fastV = fast.apply(bar);
			TimeSeriesValue slowV = slow.apply(bar);
			TimeSeriesValue fastTV = new TimeSeriesValue(fastV.getValue(), bar.getBar().getActionTimestamp(), bar.isUnsettled());
			TimeSeriesValue fastllvV = fastllv.apply(fastTV);
			TimeSeriesValue fasthhvV = fasthhv.apply(fastTV);
			TimeSeriesValue result = TV_PLACEHOLDER; // 空值
			if(lastFast.get() == 0) 
				lastFast.set(fastV.getValue());
			if(lastSlow.get() == 0) 
				lastSlow.set(slowV.getValue());
			if(lastFast.get() < lastSlow.get() && fastV.getValue() > slowV.getValue() && fastllvV.getValue() < -60) 
				result = barllvV;
			if(lastFast.get() > lastSlow.get() && fastV.getValue() < slowV.getValue() && fasthhvV.getValue() > -40) 
				result = barhhvV;
			if(!bar.isUnsettled()) {				
				lastFast.set(fastV.getValue());
				lastSlow.set(slowV.getValue());
			}
			return result;
		};
	}
	
	/**
	 * MACD波浪计算函数
	 * @param n1
	 * @param n2
	 * @param m
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> macd(int n1, int n2, int m){
		final MACD macd = MACD.of(n1, n2, m);
		final TimeSeriesUnaryOperator diff = macd.diff();
		final TimeSeriesUnaryOperator dea = macd.dea();
		int ref = m * 2;
		final TimeSeriesUnaryOperator barllv = LLV(ref);
		final TimeSeriesUnaryOperator barhhv = HHV(ref);
		final AtomicDouble lastDif = new AtomicDouble();
		final AtomicDouble lastDea = new AtomicDouble();
		return bar -> {
			TimeSeriesValue result = TV_PLACEHOLDER;
			TimeSeriesValue tv = new TimeSeriesValue(bar.getBar().getClosePrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled());
			TimeSeriesValue difVal = diff.apply(tv);
			TimeSeriesValue deaVal = dea.apply(tv);
			TimeSeriesValue llv = barllv.apply(new TimeSeriesValue(bar.getBar().getLowPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue hhv = barhhv.apply(new TimeSeriesValue(bar.getBar().getHighPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			if(difVal.getValue() < deaVal.getValue() && lastDif.get() > lastDea.get() && lastDea.get() > 0) 
				result = hhv;
			if(difVal.getValue() > deaVal.getValue() && lastDif.get() < lastDea.get() && lastDea.get() < 0) 
				result = llv;
			if(!bar.isUnsettled()) {
				lastDif.set(difVal.getValue());
				lastDea.set(deaVal.getValue());
			}
			return result;
		};
	}
}
