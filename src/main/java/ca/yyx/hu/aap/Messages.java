package ca.yyx.hu.aap;

import android.media.AudioManager;

import com.google.protobuf.nano.MessageNano;

import java.util.ArrayList;

import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.ByteArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.SensorSourceService;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.MediaSinkService.VideoConfiguration;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.InputSourceService.TouchConfig;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Messages {
    static final int DEF_BUFFER_LENGTH = 131080;

    static final int BTN_UP = 0x13;
    static final int BTN_DOWN = 0x14;
    static final int BTN_LEFT = 0x15;
    static final int BTN_RIGHT = 0x16;
    static final int BTN_BACK = 0x04;
    static final int BTN_ENTER = 0x17;
    static final int BTN_MIC = 0x54;
    static final int BTN_PHONE = 0x5;
    static final int BTN_START = 126;

    public static final int BTN_PLAYPAUSE = 0x55;
    public static final int BTN_NEXT = 0x57;
    public static final int BTN_PREV = 0x58;
    public static final int BTN_STOP = 127;

    static ByteArray createMessage(int chan, int flags, int type, byte[] data, int size) {

        ByteArray buffer = new ByteArray(6 + size);

        buffer.put(chan, flags);

        if (type >= 0) {
            buffer.encodeInt(size + 2);
            // If type not negative, which indicates encrypted type should not be touched...
            buffer.encodeInt(type);
        } else {
            buffer.encodeInt(size);
        }

        buffer.put(data, size);
        return buffer;
    }

    static byte[] createButtonEvent(long timeStamp, int button, boolean isPress)
    {
        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.KeyEvent keyEvent = new Protocol.KeyEvent();
        inputReport.timestamp = timeStamp;
        inputReport.keyEvent = keyEvent;

        keyEvent.keys = new Protocol.Key[1];
        keyEvent.keys[0] = new Protocol.Key();
        keyEvent.keys[0].keycode = button;
        keyEvent.keys[0].down = isPress;

        return createByteArray(MsgType.Input.EVENT, inputReport);
    }

    static byte[] createTouchEvent(long timeStamp, int action, int x, int y) {

        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.TouchEvent touchEvent = new Protocol.TouchEvent();
        inputReport.timestamp = timeStamp;
        inputReport.touchEvent = touchEvent;

        touchEvent.pointerData = new Protocol.TouchEvent.Pointer[1];
        Protocol.TouchEvent.Pointer pointer = new Protocol.TouchEvent.Pointer();
        pointer.x = x;
        pointer.y = y;
        touchEvent.pointerData[0] = pointer;
        touchEvent.actionIndex = 0;
        touchEvent.action = action;

        return createByteArray(MsgType.Input.EVENT, inputReport);
    }

    static byte[] createNightModeEvent(boolean enabled) {
        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.nightMode = new Protocol.SensorBatch.NightMode[1];
        sensorBatch.nightMode[0] = new Protocol.SensorBatch.NightMode();
        sensorBatch.nightMode[0].isNight = enabled;

        return createByteArray(MsgType.Sensor.EVENT, sensorBatch);
    }

    static byte[] createDrivingStatusEvent(int status) {
        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.drivingStatus = new Protocol.SensorBatch.DrivingStatus[1];
        sensorBatch.drivingStatus[0] = new Protocol.SensorBatch.DrivingStatus();
        sensorBatch.drivingStatus[0].status = status;

        return createByteArray(MsgType.Sensor.EVENT, sensorBatch);
    }

    static byte[] VERSION_REQUEST = { 0, 1, 0, 1 };
    static byte[] BYEBYE_REQUEST = { 0x00, 0x0f, 0x08, 0x00 };
    static byte[] BYEBYE_RESPONSE = { 0x00, 16, 0x08, 0x00 };

    static byte[] createServiceDiscoveryResponse(String btAddress) {
        Protocol.ServiceDiscoveryResponse carInfo = new Protocol.ServiceDiscoveryResponse();
        carInfo.make = "AACar";
        carInfo.model = "0001";
        carInfo.year = "2016";
        carInfo.headUnitModel = "ChangAn S";
        carInfo.headUnitMake = "Roadrover";
        carInfo.headUnitSoftwareBuild = "SWB1";
        carInfo.headUnitSoftwareVersion = "SWV1";
        carInfo.driverPosition = true;

        ArrayList<Service> services = new ArrayList<>();

        Service sensors = new Service();
        sensors.id = Channel.AA_CH_SEN;
        sensors.sensorSourceService = new SensorSourceService();
        sensors.sensorSourceService.sensors = new SensorSourceService.Sensor[2];
        sensors.sensorSourceService.sensors[0] = new SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS;
        sensors.sensorSourceService.sensors[1] = new SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[1].type = Protocol.SENSOR_TYPE_NIGHT_DATA;
        services.add(sensors);

        Service video = new Service();
        video.id = Channel.AA_CH_VID;
        video.mediaSinkService = new Service.MediaSinkService();
        video.mediaSinkService.availableType = Protocol.MEDIA_CODEC_VIDEO;
        video.mediaSinkService.availableWhileInCall = true;
        video.mediaSinkService.videoConfigs = new VideoConfiguration[1];
        VideoConfiguration videoConfig = new VideoConfiguration();
        videoConfig.codecResolution = VideoConfiguration.VIDEO_RESOLUTION_800x480;
        videoConfig.frameRate = VideoConfiguration.VIDEO_FPS_60;
        videoConfig.density = 120;
        video.mediaSinkService.videoConfigs[0] = videoConfig;
        services.add(video);

        Service touch = new Service();
        touch.id = Channel.AA_CH_TOU;
        touch.inputSourceService = new Service.InputSourceService();
        touch.inputSourceService.touchscreen = new TouchConfig();
        touch.inputSourceService.touchscreen.width = 800;
        touch.inputSourceService.touchscreen.height = 480;
        services.add(touch);

        Service audio0 = new Service();
        audio0.id = Channel.AA_CH_AUD;
        audio0.mediaSinkService = new Service.MediaSinkService();
        audio0.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio0.mediaSinkService.audioType = Protocol.CAR_STREAM_MEDIA;
        audio0.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio0.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.AA_CH_AUD);
        services.add(audio0);

        Service audio1 = new Service();
        audio1.id = Channel.AA_CH_AU1;
        audio1.mediaSinkService = new Service.MediaSinkService();
        audio1.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio1.mediaSinkService.audioType = Protocol.CAR_STREAM_SYSTEM;
        audio1.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio1.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.AA_CH_AU1);
        services.add(audio1);

        Service mic = new Service();
        mic.id = Channel.AA_CH_MIC;
        mic.mediaSourceService = new Service.MediaSourceService();
        mic.mediaSourceService.type = Protocol.MEDIA_CODEC_AUDIO;
        Protocol.AudioConfiguration micConfig = new Protocol.AudioConfiguration();
        micConfig.sampleRate = 16000;
        micConfig.numberOfBits = 16;
        micConfig.numberOfChannels = 1;
        mic.mediaSourceService.audioConfig = micConfig;
        services.add(mic);

        if (btAddress != null) {
            Service bluetooth = new Service();
            bluetooth.id = Channel.AA_CH_BTH;
            bluetooth.bluetoothService = new Service.BluetoothService();
            bluetooth.bluetoothService.carAddress = btAddress;
            bluetooth.bluetoothService.supportedPairingMethods = new int[]{2, 3};
            services.add(bluetooth);
        } else {
            AppLog.i("BT MAC Address is null. Skip bluetooth service");
        }

        carInfo.services = services.toArray(new Service[0]);
        return createByteArray(MsgType.Control.SERVICEDISCOVERYRESPONSE, carInfo);
    }

    static byte[] createByteArray(int msgType, MessageNano msg)
    {
        byte[] result = new byte[msg.getSerializedSize() + 2];
        return serializeByteArray(msgType, msg, result);
    }

    private static byte[] serializeByteArray(int msgType, MessageNano msg, byte[] buf)
    {
        // Header
        buf[0] = (byte) (msgType >> 8);
        buf[1] = (byte) (msgType & 0xFF);
        MessageNano.toByteArray(msg, buf, 2, msg.getSerializedSize());
        return buf;
    }

}
