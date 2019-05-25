MQTT basic implementation for traccar

 Requires traccar 4.5 or later

global configuration parameters ( conf/traccar.xml ):

	extra.handlers -
		should point to : com.ivanfm.traccar.mqtt.MQTTHandler

	mqtt.url
		URL for mqtt server
		default value tcp://localhost:1883
	mqtt.clientid
		clientid used to connect
		default value traccar.mqtt.handler
	mqtt.topicRoot
		topic where traccar data will be published
		default value /traccar/
	mqtt.alarmTopics
		topics where alarms will be published 
		multiple topics can be used separated by ":"
		default - does not publish alarms 

device configuration attributes (attributes button while editing device) :
	
	mqtt.alias
		alias to be used instead of device name
		default - name of device
	mqtt.alarmTopics
		topic where alarms will be published
		multiple topics can be used separated by ":"
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


in case of errors please use the configuration in conf/traccar.xml :

	logger.fullStackTraces
		true 

To use this handler you must add it to your CLASSPATH environment or include
it in your execution command:

	
	java -cp ivanfm-traccar-mqtt-2.2.3-jar-with-dependencies.jar:tracker-server.jar org.traccar.Main conf/traccar.xml





Sample published data on mqtt:
```
/traccar/device/ivan/id 1
/traccar/device/ivan/name ZF3 - Ivanfm
/traccar/device/ivan/category person
/traccar/device/ivan/phone +5511999999999
/traccar/device/ivan/valid true
/traccar/device/ivan/latlon -99.999999,-99.999999
/traccar/device/ivan/fixtime 2018-03-25T23:59:23Z
/traccar/device/ivan/speed 0.0
/traccar/device/ivan/altitude 0.0
/traccar/device/ivan/protocol osmand
/traccar/device/ivan/attr/batteryLevel 40.0
/traccar/device/ivan/attr/alarm sos
/traccar/device/ivan/attr/distance 0.0
/traccar/device/ivan/attr/totalDistance 7.72461427E7
/traccar/device/ivan/geofence/fence1 1|30|1
/traccar/device/ivan/geofence/fence2 0|17342|0
/traccar/device/ivan/geofence/fence3 0|10906|0
/traccar/device/ivan/geofence/fence4 0|28285|0
/traccar/device/ivan/geofence/fence5 0|30489|0
```

For geofences the value format is : ___IN-OUT-CURRENT|DISTANCE-FROM-CENTER|IN-OUT-PREVIOUS___

* IN-OUT-* -  0 OUTSIDE -  1 INSIDE
* ___DISTANCE-FROM-CENTER___  calculated only for circles



