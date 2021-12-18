package tech.quantit.northstar.domain;

import tech.quantit.northstar.common.constant.ConnectionState;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.model.GatewayDescription;

/**
 * 网关连接
 * @author KevinHuangwl
 *
 */
public abstract class GatewayConnection {
	
	protected GatewayDescription gwDescription;
	
	protected boolean errorFlag;
	
	public GatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		this.gwDescription = gwDescription;
	}
	
	public void onConnected() {
		gwDescription.setConnectionState(ConnectionState.CONNECTED);
		errorFlag = false;
	}
	
	public void onDisconnected() {
		gwDescription.setConnectionState(ConnectionState.DISCONNECTED);
	}
	
	public void onConnecting() {
		gwDescription.setConnectionState(ConnectionState.CONNECTING);
	}
	
	public void onDisconnecting() {
		gwDescription.setConnectionState(ConnectionState.DISCONNECTING);
	}
	
	public boolean isConnected() {
		return ConnectionState.CONNECTED.equals(gwDescription.getConnectionState());
	}
	
	public void onError() {
		errorFlag = true;
	}
	
	public boolean hasConnectionError() {
		return errorFlag;
	}
	
	public GatewayDescription getGwDescription() {
		return gwDescription;
	}

	public void setGwDescription(GatewayDescription gwDescription) {
		this.gwDescription = gwDescription;
	}
	
}