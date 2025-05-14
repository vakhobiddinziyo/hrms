package itpu.uz.itpuhrms.services.hikvision

import com.fasterxml.jackson.annotation.JsonProperty

import java.util.*

data class EventResponse(
    @JsonProperty("dateTime") val dateTime: Date,
    @JsonProperty("macAddress") val macAddress: String? = null,
    @JsonProperty("portNo") val portNo: Int? = null,
    @JsonProperty("protocol") val protocol: String? = null,
    @JsonProperty("eventState") val eventState: String? = null,
    @JsonProperty("eventDescription") val eventDescription: String? = null,
    @JsonProperty("ipAddress") val ipAddress: String? = null,
    @JsonProperty("activePostCount") val activePostCount: Int? = null,
    @JsonProperty("eventType") val eventType: String? = null,
    @JsonProperty("channelID") val channelID: Int? = null,
    @JsonProperty("AccessControllerEvent") val accessControllerEvent: AccessControllerEvent? = null
) {
    override fun toString(): String {
        return "EventResponse(dateTime=$dateTime," +
                "\n macAddress=$macAddress," +
                "\n portNo=$portNo," +
                "\n protocol=$protocol," +
                "\n eventState=$eventState," +
                "\n eventDescription=$eventDescription," +
                "\n ipAddress=$ipAddress," +
                "\n activePostCount=$activePostCount," +
                "\n eventType=$eventType," +
                "\n channelID=$channelID," +
                "\n accessControllerEvent=$accessControllerEvent)"
    }
}


data class AccessControllerEvent(
    @JsonProperty("cardReaderKind") val cardReaderKind: Int? = null,
    @JsonProperty("frontSerialNo") val frontSerialNo: Int? = null,
    @JsonProperty("cardReaderNo") val cardReaderNo: Int? = null,
    @JsonProperty("statusValue") val statusValue: Int? = null,
    @JsonProperty("majorEventType") val majorEventType: Int? = null,
    @JsonProperty("employeeNoString") val employeeNoString: String? = null,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("deviceName") val deviceName: String,
    @JsonProperty("currentVerifyMode") val currentVerifyMode: String? = null,
    @JsonProperty("serialNo") val serialNo: Int? = null,
    @JsonProperty("purePwdVerifyEnable") val purePwdVerifyEnable: Boolean? = null,
    @JsonProperty("FaceRect") val faceRect: FaceRect? = null,
    @JsonProperty("subEventType") val subEventType: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("verifyNo") val verifyNo: Int? = null,
    @JsonProperty("userType") val userType: String? = null,
    @JsonProperty("picturesNumber") val picturesNumber: Int? = null,
    @JsonProperty("attendanceStatus") val attendanceStatus: String? = null,
    @JsonProperty("mask") val mask: String? = null
) {
    override fun toString(): String {
        return "AccessControllerEvent(cardReaderKind=$cardReaderKind," +
                "\n frontSerialNo=$frontSerialNo," +
                "\n cardReaderNo=$cardReaderNo," +
                "\n statusValue=$statusValue," +
                "\n majorEventType=$majorEventType," +
                "\n employeeNoString=$employeeNoString," +
                "\n label=$label," +
                "\n deviceName='$deviceName'," +
                "\n currentVerifyMode=$currentVerifyMode," +
                "\n serialNo=$serialNo," +
                "\n purePwdVerifyEnable=$purePwdVerifyEnable," +
                "\n faceRect=$faceRect," +
                "\n subEventType=$subEventType," +
                "\n name=$name," +
                "\n verifyNo=$verifyNo," +
                "\n userType=$userType," +
                "\n picturesNumber=$picturesNumber," +
                "\n attendanceStatus=$attendanceStatus," +
                "\n mask=$mask)"
    }
}



data class FaceRect(
    @JsonProperty("width") val width: Any? = null,
    @JsonProperty("x") val x: Any? = null,
    @JsonProperty("y") val y: Any? = null,
    @JsonProperty("height") val height: Any? = null
) {
    override fun toString(): String {
        return "FaceRect(width=$width, x=$x, y=$y, height=$height)"
    }
}

