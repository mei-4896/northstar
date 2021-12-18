package tech.quantit.northstar.main.service;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;

@Slf4j
public class SMSTradeService {

	private FastEventEngine feEngine;
	
	public SMSTradeService(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	public void dispatchMsg(String text) {
		log.info("收到消息：{}", text);
		feEngine.emitEvent(NorthstarEventType.EXT_MSG, text);
	}
}