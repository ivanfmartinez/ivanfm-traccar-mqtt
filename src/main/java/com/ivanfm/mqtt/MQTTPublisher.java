package com.ivanfm.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MQTTPublisher implements MqttCallback {

	public static final int AT_LEAST_ONCE = 1;
	private static final Logger LOGGER = LoggerFactory.getLogger(MQTTPublisher.class);
	
	private final MqttAsyncClient mqtt;
	private final String rootTopic;
	private transient IMqttToken connectToken;

	public MQTTPublisher(String url, String clientId, String rootTopic) throws MqttException {
		this.rootTopic = rootTopic;
		mqtt = new MqttAsyncClient(url, clientId);
		mqtt.setCallback(this);
		doConnect();
		connectToken.waitForCompletion(2000);
	}
	
	private final class ConnectThread extends Thread {
		
		@Override
		public void run() {
			boolean running = true; 
			while (running) {
				try {
					LOGGER.info("trying to connect to mqtt : " + mqtt.getServerURI());
					final MqttConnectOptions opt = new MqttConnectOptions();
				    opt.setCleanSession(false);
				     
					connectToken = mqtt.connect(opt);
					connectToken.waitForCompletion(2000);
					running = false;
				} catch (MqttException e) {
					LOGGER.warn("Connect error", e);
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
			}
			LOGGER.info("connected to mqtt : " + mqtt.getServerURI());
			
		}
	}

	private void doConnect() {
		final Thread t = new ConnectThread();
		t.start();
		// Wait connection to start...
		while (connectToken == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				LOGGER.warn("Error waiting for mqtt conection", e);
			}
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {
		doConnect();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
	}



	public void publish(String topic, byte[] msg) {
		publishOnRoot(rootTopic + topic, msg, AT_LEAST_ONCE, false);
	}
	
	public void publishOnRoot(String topic, byte[] msg, boolean retained) {
		publishOnRoot(topic, msg, AT_LEAST_ONCE, retained);
	}

	private final int MAX_RETRY = 5;

	public void publishOnRoot(String topic, byte[] msg, int qos, boolean retained) {
		int retryCount = 0;
		boolean ok = false;
		do {
			try {
				retryCount++;
				mqtt.publish(topic, msg, qos , retained);
				ok = true;
			} catch (MqttPersistenceException e) {
				LOGGER.warn("Error in mqtt publish", e);
			} catch (MqttException e) {
				if ((e.getReasonCode() == MqttException.REASON_CODE_MAX_INFLIGHT) && (retryCount < MAX_RETRY)) {
					// ignore 
				} else {
					LOGGER.warn("error publising to mqtt" , e);
				}
			}
		} while (!ok && (retryCount < MAX_RETRY));
	}


}
