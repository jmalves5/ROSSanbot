package com.example.rossanbot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.ImageRegistrationMode;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoFrameRef;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.android.OpenNIHelper;
import org.openni.android.OpenNIView;
import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import org.ros.android.view.RosImageView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class MainActivity extends RosActivity {


    private OpenNIHelper openNIHelper;
    private Device device;
    private VideoStream videoStream;
    private VideoStream video2Stream;
    private Thread streamThread;
    private boolean startStream = true;
    private final Object m_sync = new Object();

    private RosImageView rosImageView;

    public MainActivity() {
        super("CameraTutorial", "CameraTutorial");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        rosImageView = findViewById(R.id.ros_camera_preview_view);

        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

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


            device.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);


            //FIRST STREAM

            //change video-mode
            List<VideoMode> videoModes = videoStream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();

                //set this video-mode
                if (X == 640 && Y == 480 && mode.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                    videoStream.setVideoMode(mode);
                }
            }


            //SECOND STREAM

            //change video-mode
            videoModes = video2Stream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();

                //set this video-mode
                if (X == 640 && Y == 480 && mode.getPixelFormat() == PixelFormat.YUYV && mode.getFps()==30) {
                    video2Stream.setVideoMode(mode);
                }
            }


            //starting the thread to avoid freezing GUI
            startStreamThread();
        }

        @Override
        public void onDeviceOpenFailed(String s) {
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

                            ByteBuffer videoStreamBytes = videoStream.readFrame().getData();
                            ByteBuffer videoStream2Bytes = video2Stream.readFrame().getData();

                            byte[] imageBytes= new byte[videoStreamBytes.remaining()];
                            videoStreamBytes.get(imageBytes);
                            Bitmap bmp=BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);


                            rosImageView.setImageBitmap(bmp);
                        }
                    }

                }
            }
        };
        streamThread.start();
    }

    

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(rosImageView, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }

    }

}