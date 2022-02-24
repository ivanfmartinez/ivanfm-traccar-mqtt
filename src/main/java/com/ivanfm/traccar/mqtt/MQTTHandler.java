package com.ivanfm.traccar.mqtt;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.Context;
import org.traccar.model.Position;

import com.ivanfm.mqtt.MQTTPublisher;


public class MQTTHandler extends BaseDataHandler {

	private final static Logger log = LoggerFactory.getLogger(MQTTHandler.class);
	
	private static MQTTPublisher globalPublisher;

	private synchronized MQTTPublisher getPublisher() {
		if (globalPublisher == null) {
			try {
				final Config config = Context.getConfig();
				globalPublisher = new MQTTPublisher(
						config.getString("mqtt.url", "tcp://localhost:1883"), 
						config.getString("mqtt.clientid", "traccar.mqtt.handler"),
						config.getString("mqtt.topicRoot", "/traccar/"));

				globalPublisher.publish("start", (new Date()).toString());
			} catch (Exception e) {
				log.error("", e);
			}
		}
		return globalPublisher;
	}
	
	
	/**
	 * Multiple hanlders are created but only one publisher...
	 * 
	 * @throws Exception
	 */
	public MQTTHandler() {
		// publisher is already instantiated....
	}
	
	
	@Override
	protected  Position  handlePosition(Position position) {
		try {
			publish(position);
		} catch (Throwable t) {
			log.error("", t);
		}
		return position;
	}

	public void publish(Position position) {
		final MQTTPublisher publisher = getPublisher();
		if (publisher != null) {
			final ProcessPosition ld = new ProcessPosition(publisher, position);
			ld.publish();
		}
	}


}
