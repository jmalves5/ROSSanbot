package com.example.rossanbot;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.ImageRegistrationMode;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.android.OpenNIHelper;
import org.openni.android.OpenNIView;
import org.ros.android.AppCompatRosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatRosActivity {


    private Talker talker;
    private OpenNIView openNIView;
    private OpenNIView openNIView2;
    private OpenNIHelper openNIHelper;
    private Device device;
    private static VideoStream videoStream;
    private static VideoStream video2Stream;
    private Thread streamThread;
    private boolean startStream = true;
    private final Object m_sync = new Object();

    public static VideoStream getVideoStream(){
        return videoStream;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

        setContentView(R.layout.main);

        openNIView = findViewById(R.id.frame1View);
        openNIView2 = findViewById(R.id.frame2View);

        openNIHelper = new OpenNIHelper(getApplicationContext());
        openNIHelper.requestDeviceOpen(deviceOpenListener);


    }


    OpenNIHelper.DeviceOpenListener deviceOpenListener = new OpenNIHelper.DeviceOpenListener() {
        @Override
        public void onDeviceOpened(UsbDevice usbDevice) {
            //list devices
            List<DeviceInfo> deviceList=OpenNI.enumerateDevices();
            if(deviceList.size()==0){
                return;
            }
            //open the device
            for (int i = 0; i < deviceList.size(); i ++){
                if(deviceList.get(i).getUsbProductId() == usbDevice.getProductId()){
                    device = Device.open();
                }
            }

            //create video streams
            if(device!=null){
                videoStream = VideoStream.create(device, SensorType.DEPTH);
                video2Stream = VideoStream.create(device, SensorType.COLOR);
            }

            //some experiments with device
            device.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);
            //device.setDepthColorSyncEnabled(true);
            //device.setDepthOptimizationEnable(true);

            //FIRST STREAM
            //change video-mode
            List<VideoMode> videoModes = videoStream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();

                //set this video-mode
                if (mode.getResolutionX() == 640 && mode.getResolutionY() == 480 && mode.getPixelFormat() == PixelFormat.JPEG) {
                    videoStream.setVideoMode(mode);
                }
            }

            //change video-mode
            videoModes = video2Stream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();

                //set this video-mode
                if (mode.getResolutionX() == 640 && mode.getResolutionY() == 480 && mode.getPixelFormat() == PixelFormat.YUYV) {
                    video2Stream.setVideoMode(mode);
                }
            }

            //starting the thread to avoid freezing GUI
            startStreamThread();
        }

        @Override
        public void onDeviceOpenFailed(java.lang.String s) {
            System.out.println("OnDeviceFailed");
        }


    };

    private void startStreamThread(){
        streamThread = new Thread() {
            @Override
            public void run(){
                //list of streams
                List <VideoStream> streams = new ArrayList<>();
                //adding to the list the streams
                streams.add(videoStream);
                streams.add(video2Stream);

                //starting the stream
                videoStream.start();
                video2Stream.start();


                while(startStream){
                    try{
                        //waiting for stream
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e){
                        e.printStackTrace();
                    }
                    //synchronized
                    synchronized (m_sync) {
                        if(videoStream!=null){
                            //VideoFrameRef videoFrameRef = videoStream.readFrame();
                            //ByteBuffer buf = videoFrameRef.getData();
                            //update view
                            openNIView.update(videoStream);
                            openNIView2.update(video2Stream);
                            //videoFrameRef.release();
                        }
                    }

                }
            }
        };
        streamThread.start();
    }

    protected void onDestroy() {
        OpenNI.shutdown();
        super.onDestroy();
    }

    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("Pubsub Tutorial", "Pubsub Tutorial");
    }




    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        talker = new Talker();

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(talker, nodeConfiguration);

    }
}



