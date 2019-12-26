package io.snice.usb.hektor;

import io.hektor.core.ActorRef;
import io.snice.usb.UsbScanner;
import io.snice.usb.event.Subscribe;
import io.snice.usb.linux.LibUsbConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class ActorUsbManagerContextTest {

    private ActorUsbManagerContext ctx;

    @Mock
    private ActorRef actorRef;

    @Mock
    private UsbScanner scanner;

    @Mock
    private LibUsbConfiguration config;

    @Before
    public void setUp() throws Exception {
        ctx = new ActorUsbManagerContext(actorRef, scanner, config, new HashMap<>());
    }

    @Test
    public void testCreateSubscription() {
        final var sub = Subscribe.from(actorRef).build();
        final var subscription = ctx.createSubscription(sub);

    }

}