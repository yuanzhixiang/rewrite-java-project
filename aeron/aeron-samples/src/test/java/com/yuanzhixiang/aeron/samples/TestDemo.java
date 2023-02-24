package com.yuanzhixiang.aeron.samples;

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.ControlledFragmentHandler;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuan Zhixiang
 */
public class TestDemo {

    @Test
    public void test() {
        System.setProperty("aeron.debug.timeout", "1000000000000000");
        System.out.println("---");
//        FileUtil.del("/var/folders/c9/yw0ngl0d0yxfqm1fqhyxkz400000gn/T/aeron-oker");

        // okcoin-push-console.dev-a-okex.svc.dev.local
//        final String channel = "aeron:udp?endpoint=192.168.1.160:20001|control=10.254.108.128:7006|control-mode=dynamicnull";
//        final String channel = "aeron:udp?endpoint=192.168.1.160:20001|control=127.0.0.1:8001|control-mode=dynamicnull";
        final String channel = "aeron:udp?endpoint=192.168.1.160:20001|control=push-console.dev-a-okex.svc.dev.local:20001|control-mode=dynamicnull";
//        final String channel = "aeron:udp?endpoint=192.168.1.160:20001|control=127.0.0.1:20001|control-mode=dynamicnull";

        String pubChannel = "aeron:udp?endpoint=push-console.dev-a-okex.svc.dev.local:20000";
//        String pubChannel = "aeron:udp?endpoint=127.0.0.1:20000";
        MediaDriver.Context context = new MediaDriver.Context();
        context.dirDeleteOnStart(true);
        context.termBufferSparseFile(false);
        MediaDriver driver = MediaDriver.launch(context);

        Aeron.Context ctx = new Aeron.Context();
        ctx.aeronDirectoryName(driver.aeronDirectoryName());

        try (
                Aeron aeron = Aeron.connect(ctx);
                Subscription sub = aeron.addSubscription(channel, 2001);
                ConcurrentPublication pub = aeron.addPublication(pubChannel, 2000)
        ) {
            ControlledFragmentHandler handler = (buffer, offset, length, header) -> {
                System.out.println("received");
                return ControlledFragmentHandler.Action.COMMIT;
            };
            while (true) {
                TimeUnit.SECONDS.sleep(1);

//                if (!pub.isConnected()) {
//                    System.out.println(LocalDateTime.now() + " pub connection isn't connected");
//                    continue;
//                } else {
//                    System.out.println(LocalDateTime.now() + " pub connection is connected");
//                }

                if (!sub.isConnected()) {
                    System.out.println(LocalDateTime.now() + " sub connection isn't connected");
                    continue;
                }

                sub.controlledPoll(handler, 1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
