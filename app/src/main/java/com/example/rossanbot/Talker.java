package com.example.rossanbot;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openni.VideoFrameRef;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import static org.jboss.netty.buffer.ChannelBuffers.LITTLE_ENDIAN;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

public class Talker extends AbstractNodeMain{



    @Override
    public GraphName getDefaultNodeName() {
         return GraphName.of("sanbot/talker");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<sensor_msgs.CompressedImage> publisher = connectedNode.newPublisher("chatter", sensor_msgs.CompressedImage._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {

            @Override
            protected void loop() throws InterruptedException {
                if(MainActivity.getVideoStream()!=null){

                    sensor_msgs.CompressedImage image= publisher.newMessage();
                    VideoFrameRef videoFrameRef = MainActivity.getVideoStream().readFrame();
                    if(videoFrameRef!=null){
                        ChannelBuffer channelBuffer = dynamicBuffer(LITTLE_ENDIAN,614400);

                        channelBuffer.setBytes(0, videoFrameRef.getData());

                        image.setData(channelBuffer);

                        image.setFormat("jpeg");

                        publisher.publish(image);

                        Thread.sleep(1000);
                    }

                }
            }
        });
    }
}