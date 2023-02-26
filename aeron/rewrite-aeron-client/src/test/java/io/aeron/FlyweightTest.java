package io.aeron;

import io.aeron.protocol.HeaderFlyweight;
import io.aeron.protocol.NakFlyweight;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlyweightTest {

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private final UnsafeBuffer aBuff = new UnsafeBuffer(buffer);

    private final NakFlyweight encodeNakHeader = new NakFlyweight();
    private final NakFlyweight decodeNakHeader = new NakFlyweight();

    @Test
    void shouldEncodeAndDecodeNakCorrectly() {
        encodeNakHeader.wrap(aBuff);
        encodeNakHeader.version((short) 1);
        encodeNakHeader.flags((byte) 0);
        encodeNakHeader.headerType(HeaderFlyweight.HDR_TYPE_NAK);
        encodeNakHeader.frameLength(NakFlyweight.HEADER_LENGTH);
        encodeNakHeader.sessionId(0xdeadbeef);
        encodeNakHeader.streamId(0x44332211);
        encodeNakHeader.termId(0x99887766);
        encodeNakHeader.termOffset(0x22334);
        encodeNakHeader.length(512);

        decodeNakHeader.wrap(aBuff);
        assertEquals(1, decodeNakHeader.version());
        assertEquals(0, decodeNakHeader.flags());
        assertEquals(HeaderFlyweight.HDR_TYPE_NAK, decodeNakHeader.headerType());
        assertEquals(NakFlyweight.HEADER_LENGTH, decodeNakHeader.frameLength());
        assertEquals(0xdeadbeef, decodeNakHeader.sessionId());
        assertEquals(0x44332211, decodeNakHeader.streamId());
        assertEquals(0x99887766, decodeNakHeader.termId());
        assertEquals(0x22334, decodeNakHeader.termOffset());
        assertEquals(512, decodeNakHeader.length());
    }

}
