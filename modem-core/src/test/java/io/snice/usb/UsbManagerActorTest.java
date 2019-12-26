package io.snice.usb;

import io.hektor.core.Actor;
import io.hektor.core.ActorContext;
import io.hektor.core.ActorRef;
import io.snice.hektor.FsmActor;
import io.snice.hektor.OnStartFunction;
import io.snice.hektor.OnStopFunction;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.UsbManagerData;
import io.snice.usb.fsm.UsbManagerFsm;
import io.snice.usb.fsm.UsbManagerState;
import io.snice.usb.hektor.ActorUsbManagerContext;
import io.snice.usb.impl.LinuxUsbScanner;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UsbManagerActorTest extends UsbTestBase {

    private UsbManagerContext usbManagerContext;
    private UsbManagerData data;
    private LinuxUsbScanner scanner;

    private OnStartFunction<UsbManagerContext, UsbManagerData> onStart;
    private OnStopFunction<UsbManagerContext, UsbManagerData> onStop;

    private FsmActor<UsbManagerState, UsbManagerContext, UsbManagerData> actor;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        data = new UsbManagerData();
        scanner = mock(LinuxUsbScanner.class);
        final var ref = mock(ActorRef.class);

        usbManagerContext = new ActorUsbManagerContext(ref, scanner, config, knownUsbVendors);

        onStart = mock(OnStartFunction.class);
        onStop = mock(OnStopFunction.class);

        final var ctx = mock(ActorContext.class);
        Actor._ctx.set(ctx);

        final var props = FsmActor.of(UsbManagerFsm.definition)
                .withContext(usbManagerContext)
                .withData(data)
                .withStartFunction(onStart)
                .withStopFunction(onStop)
                .build();
        actor = (FsmActor)props.creator().get();
        actor.start();
        verify(onStart).start(ctx, usbManagerContext, data);
    }

    @Test
    public void testOnUsbAttachEvent() {
        //
    }
}
