//package io.aeron.driver;
//
//import org.agrona.concurrent.Agent;
//
//import java.util.concurrent.TimeUnit;
//
//import static io.aeron.CommonContext.*;
//
///**
// * Driver Conductor that takes commands from publishers and subscribers, and orchestrates the media driver.
// */
//public final class DriverConductor implements Agent {
//    private static final long CLOCK_UPDATE_INTERNAL_NS = TimeUnit.MILLISECONDS.toNanos(1);
//    private static final String[] INVALID_DESTINATION_KEYS =
//            {
//                    MTU_LENGTH_PARAM_NAME,
//                    RECEIVER_WINDOW_LENGTH_PARAM_NAME,
//                    SOCKET_RCVBUF_PARAM_NAME,
//                    SOCKET_SNDBUF_PARAM_NAME
//            };
//
//    @Override
//    public void onStart() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public int doWork() throws Exception {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void onClose() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public String roleName() {
//        throw new UnsupportedOperationException();
//    }
//}
