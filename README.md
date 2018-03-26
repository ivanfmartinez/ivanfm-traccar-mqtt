MQTT basic implementation for traccar

 Requires traccar 3.16-SNAPSHOT after 2018-03-25

global configuration parameters :

	extra.handlers -
		should point to : "com.ivanfm.traccar.MQTTHandler"

	mqtt.url
		URL for mqtt server
		default value tcp://localhost:1883
	mqtt.clientid
		clientid used to connect
		default value traccar.mqtt.handler
	mqtt.topicRoot
		topic where traccar data will be published
		default value /traccar/
	mqtt.alarmTopic
		topic where alarms will be published
		default - does not publish alarms in other topic

device configuration parameters :
	
	mqtt.alias
		alias to be used instead of device name
		default - name of device
	mqtt.alarmTopic
		topic where alarms will be published
		default - does not publish alarms in other topic
	mqtt.position.process.enabled
		enabled/disable publishing for the device
		default - true
	mqtt.position.process.alarms.enabled
		enable/disable publishing of alarms for the device
		default - true
	mqtt.geofence.GEOFENCE_ALIAS.topics
		where to publish geofence state changes for device
		multiple topics can be used separated by ":"
		default - does not publish changes in other topics



