package com.ivanfm.traccar.mqtt;

import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.database.DeviceManager;
import org.traccar.database.GeofenceManager;
import org.traccar.geofence.GeofenceCircle;
import org.traccar.geofence.GeofenceGeometry;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;
import org.traccar.notification.TextTemplateFormatter;

import com.ivanfm.mqtt.MQTTPublisher;

public class ProcessPosition {
	
	final static Logger log = LoggerFactory.getLogger(ProcessPosition.class);

	final MQTTPublisher publisher;
	final Position position;
	final Device device;
	private final String devAlias;
	private final Event event;
	private final List<String> alarmTopics;
	private final User user;
	private final DeviceManager dm = Context.getDeviceManager();
	private final GeofenceManager gm = Context.getGeofenceManager();

	
	private static String nameCleanUp(String name) {
		return name.replaceAll("[- /]", "_").replaceAll("__+", "_").toLowerCase();
	}
	
	public ProcessPosition(MQTTPublisher publisher, Position position) {
		this.publisher = publisher;
		this.position = position;
		device = Context.getDeviceManager().getById(position.getDeviceId());
		final Set<Long> users =  dm.getUserItems(device.getId());
		user = Context.getPermissionsManager().getUser( users.isEmpty() ? 1 : users.iterator().next() );

		devAlias = nameCleanUp(dm.lookupAttributeString(position.getDeviceId(), "mqtt.alias", device.getName(), true, false));
		// Current configuration should use alarmTopics instead of alarmTopic
		alarmTopics = splitTopics(dm.lookupAttributeString(position.getDeviceId(), "mqtt.alarmTopics", dm.lookupAttributeString(position.getDeviceId(), "mqtt.alarmTopic", "", true, true), true, true));
		
		event = new Event("MQTTX", position);
		
	}
	
	private static List<String> splitTopics(String topics) {
		if (StringUtils.isNotBlank(topics)) {
			final List<String> split = new ArrayList<>(Arrays.asList(topics.split(":")));
			int x = 0;
			while (x < split.size()) {
				if (StringUtils.isBlank(split.get(x))) {
					split.remove(x);
				} else {
					x++;
				}
			}
		
			return split;
		} else {
			return Collections.<String>emptyList();
		}
	}
	
	void publish(String path, String value) {
		publisher.publish("device/" +  devAlias + "/" + path , value);
	}

	private void publishIfNotBlank(String path, String value) {
		if (StringUtils.isNotBlank(value)) {
			publisher.publish("device/" +  devAlias + "/" + path , value);
		}
	}

	private void publishIfNotZero(String path, long value) {
		if (value > 0) {
			publisher.publish("device/" +  devAlias + "/" + path , Long.toString(value));
		}
	}

	private void publishIfNotNull(String path, Object value) {
		if (value != null ) {
			publishIfNotBlank(path, value.toString());
		}
	}

	

	void publish() {
		final Map<String,Object> attrs = position.getAttributes();
		if (dm.lookupAttributeBoolean(device.getId(), "mqtt.position.process.enabled", true, true, true)) {
			final SimpleDateFormat ISO_8601_Z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			ISO_8601_Z.setTimeZone(TimeZone.getTimeZone("UTC"));
			/**
			 * All data available on MQTT, but for many devices can be an overload
			 * should be configurable, data published and which devices
			 */
			publishIfNotZero("id", device.getId());
			publish("name", ""+device.getName());
			publishIfNotBlank("category", ""+device.getCategory());
			publishIfNotBlank("model", device.getModel());
			publishIfNotBlank("phone", device.getPhone());
			publishIfNotBlank("contact", device.getContact());
			publishIfNotZero("group", device.getGroupId());

			publishIfNotZero("positionId", position.getId());
			publish("valid", ""+position.getValid());
			publish("latlon", position.getLatitude() +"," + position.getLongitude());
			publish("fixtime", ISO_8601_Z.format(position.getFixTime()));
			publish("speed", Double.toString(position.getSpeed()));
			publish("altitude", Double.toString(position.getAltitude()));
			publish("accuracy", Double.toString(position.getAccuracy()));
			publishIfNotBlank("type", position.getType());
			publishIfNotBlank("protocol", position.getProtocol());
			for (Entry<String,Object> e : attrs.entrySet()) {
				publishIfNotNull( "attr/" + e.getKey(), e.getValue());
			}

			processGeofences();
		}

		if (dm.lookupAttributeBoolean(device.getId(), "mqtt.position.process.alarms.enabled", true, true, true)) {
			if (!alarmTopics.isEmpty()) {
				publishAlarm(attrs);
			}
		}
	}

	private void processGeofences() {
		boolean stateChanged = false;
		for (long geofenceId : gm.getAllDeviceItems(position.getDeviceId())) {
			final Geofence g = gm.getById(geofenceId);
			final String geofenceAlias = nameCleanUp(g.getName());
			final GeofenceGeometry geometry = g.getGeometry(); 
// if running with the changes to support accuracy in  geometry use this line
			final boolean inside = geometry.containsPoint(position.getLatitude(), position.getLongitude(), position.getAccuracy());
//			final boolean inside = geometry.containsPoint(position.getLatitude(), position.getLongitude());
			stateChanged = processGeofence(g, geofenceAlias, inside) || stateChanged;
		}
		if (stateChanged) {
			try {
				dm.updateDeviceStatus(device);
			} catch (SQLException e) {
				log.error("Error updating device", e);
			}
		}
	}

	private boolean processGeofence(Geofence geofence, String geofenceAlias, boolean inside) {
		final String topicsSt = dm.lookupAttributeString(position.getDeviceId(), "mqtt.geofence." + geofenceAlias + ".topics", "", true, false);
		final List<String> topics = splitTopics(topicsSt);
		final String insideSt = inside ? "1" : "0";

		// Use attribute to keep state control even when traccar is restarted
		final String insideKey = "mqtt.geofence." + geofenceAlias + ".inside";
		final String prevInside = (device.getString(insideKey) == null) ? "-" : device.getString(insideKey);
		
		final long distance = calculateDistance(position, geofence, insideSt.equals("1"));
		
		publish("geofence/" + geofenceAlias , insideSt + "|" + distance + "|" + prevInside);
		if (!insideSt.equalsIgnoreCase(prevInside)) {
			device.set(insideKey, insideSt);
			for (String topic : topics) {
			    try {
   			        final VelocityContext velocityContext = prepareContext();
				velocityContext.put("geofence", geofence);
				velocityContext.put("areaName", geofence.getName());
				velocityContext.put("in_out", inside ? "IN" : "OUT");
				velocityContext.put("distance", ""+distance);
										
				publisher.publishOnRoot(topic,  
						format(velocityContext, "mqtt-moved/").getBytes(), 
						MQTTPublisher.AT_LEAST_ONCE, false);
			    } catch (org.apache.velocity.exception.ResourceNotFoundException e) {
			        log.debug("{}", e.getMessage());
			    }
			}
			return true;
		} else {
			return false;
		}	
	}
	
	public static final long UNKNOWN_DISTANCE = 999999;

	public static long calculateDistance(final Position position, final Geofence geofence, final boolean inside) {
		if (geofence.getGeometry() instanceof GeofenceCircle) {
			final GeofenceCircle circle = (GeofenceCircle)(geofence.getGeometry());
			return (long)circle.distanceFromCenter(position.getLatitude(), position.getLongitude());
		} else {
			return inside ? 0 : UNKNOWN_DISTANCE;
		}
	}
	
	public void publishAlarm(final Map<String, Object> attrs) {
		
		if (attrs.containsKey(Position.KEY_ALARM)) {
			final String alarm = attrs.get(Position.KEY_ALARM).toString();
			if (alarm.length() > 0) {
				if (!"tracker".equalsIgnoreCase(alarm) && !"et".equalsIgnoreCase(alarm)) {

				    try {
				        final VelocityContext velocityContext = prepareContext();
							
    				        for (String alarmTopic : alarmTopics) {
   						publisher.publishOnRoot(alarmTopic, 
								format(velocityContext, "mqtt-alarm/").getBytes(),
								MQTTPublisher.AT_LEAST_ONCE, false);
				        }
				    } catch (ResourceNotFoundException e) {
   			                log.debug("{}", e.getMessage());
				    }
				}
			}
		}
	}
	
	VelocityContext prepareContext() {
	    final VelocityContext velocityContext = NotificationFormatter.prepareContext(user.getId(), event, position);
		velocityContext.put("deviceAlias", devAlias);
		return velocityContext;
	}
	
	String format(VelocityContext velocityContext, String path) {
	        NotificationMessage message = TextTemplateFormatter.formatMessage(velocityContext, event.getType(), path);
		return message.getBody();
	}
	
	

	
}