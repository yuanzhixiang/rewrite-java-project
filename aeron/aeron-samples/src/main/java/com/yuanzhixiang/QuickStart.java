package com.yuanzhixiang;

import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class QuickStart {
    public static void main(String[] args) {
        System.setProperty(CommonContext.DEBUG_TIMEOUT_PROP_NAME, "100000000000000");
        System.setProperty(CommonContext.DRIVER_TIMEOUT_PROP_NAME, "100000000000000");

        // 定义pub和sub之间的交互通道
        final String channel = "aeron:ipc";
        // 定义交互消息
        final String message = "my message";
        // 定义使用的空闲策略
        final IdleStrategy idle = new SleepingIdleStrategy();
        // 定义堆外缓冲区，用来保存要发送的消息
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        try (
                // 定义MediaDriver，MediaDriver管理所有IPC和网络活动
                MediaDriver driver = MediaDriver.launch();
                // 定义Aeron实例，Aeron实例为程序提供了主要的API
                Aeron aeron = Aeron.connect();
                // 定义订阅端，轮训接收消息
                Subscription sub = aeron.addSubscription(channel, 10);
                // 定义发布端，用于发送消息
                Publication pub = aeron.addPublication(channel, 10)
        ) {
            // 循环等待发布端链接成功，迭代之间有1微秒的暂停，直到连接成功
            while (!pub.isConnected()) {
                idle.idle();
            }
            // 把要发送的消息放入缓冲区
            unsafeBuffer.putStringAscii(0, message);
            System.out.println("sending:" + message);
            // 发布消息，返回值小于0表示发布遇到的问题，使用空闲策略轮训发布，直到成功
            while (pub.offer(unsafeBuffer) < 0) {
                idle.idle();
            }
            /**
             * 构造消息处理方法；
             * 此处消息较小，使用默认的FragmentHandler来处理消息；
             * 当消息比较大而被切分时，需要是用FragmentAssembler重新组装消息；
             */
            FragmentHandler handler = (buffer, offset, length, header) ->
                    System.out.println("received:" + buffer.getStringAscii(offset));
            // 轮训处理订阅的数据，返回值表示收到的帧数，使用空闲策略循环消费
            while (sub.poll(handler, 1) <= 0) {
                idle.idle();
            }
        }
    }
}
