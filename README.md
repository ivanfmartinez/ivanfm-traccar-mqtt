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



