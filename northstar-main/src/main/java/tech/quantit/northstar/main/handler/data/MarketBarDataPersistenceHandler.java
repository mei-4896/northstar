package tech.quantit.northstar.main.handler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 负责处理行情持久化
 * 
 * @author KevinHuangwl
 *
 */
public class MarketBarDataPersistenceHandler {

	private MarketDataRepository mdRepo;
	
	private static final int DEFAULT_SIZE = 4096;
	
	private static final int DEFAULT_LEN = 300;
	
	private Map<String, Queue<TickField>> ticksQMap = new HashMap<>(DEFAULT_SIZE);
	
	private static final int CORE_SIZE = 10;
	private static final int MAX_SIZE = 100;
	private static final int KEEP_ALIVE = 80;
	private static final int APPROX_CONTRACTS = 512;
	private Executor exec = new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new ArrayBlockingQueue<>(APPROX_CONTRACTS));
	
	public MarketBarDataPersistenceHandler(MarketDataRepository mdRepo) {
		this.mdRepo = mdRepo;
	}
	
	public void onTick(TickField tick) {
		if(!ticksQMap.containsKey(tick.getUnifiedSymbol())) {
			ticksQMap.put(tick.getUnifiedSymbol(), new LinkedList<>());
		}
		ticksQMap.get(tick.getUnifiedSymbol()).offer(tick);
	}
	
	public void onBar(BarField bar) {
		long barTime = bar.getActionTimestamp();
		Queue<TickField> ticksQ = ticksQMap.get(bar.getUnifiedSymbol());
		List<TickField> minTicks = new ArrayList<>(DEFAULT_LEN);
		while(!ticksQ.isEmpty() && ticksQ.peek().getActionTimestamp() <= barTime + 60000) {
			minTicks.add(ticksQ.poll());
		}
		exec.execute(() -> mdRepo.insert(MinBarDataPO.builder()
					.gatewayId(bar.getGatewayId())
					.unifiedSymbol(bar.getUnifiedSymbol())
					.barData(bar.toByteArray())
					.ticksData(minTicks.stream().map(TickField::toByteArray).toList())
					.updateTime(barTime + 60000)
					.build()));
	}
	
}