package io.snice.usb;

import io.hektor.core.Actor;
import io.hektor.core.ActorContext;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.UsbManagerData;
import io.snice.usb.fsm.UsbManagerFsm;
import io.snice.usb.fsm.UsbManagerState;
import org.junit.Before;
import org.junit.Test;

import javax.usb.UsbServices;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UsbManagerActorTest extends UsbTestBase {

    private UsbServices usbServices;
    private UsbManagerContext usbManagerContext;
    private UsbManagerData data;
    private UsbScanner scanner;

    private OnStartFunction<UsbManagerContext, UsbManagerData> onStart;
    private OnStopFunction<UsbManagerContext, UsbManagerData> onStop;

    private FsmActor<UsbManagerState, UsbManagerContext, UsbManagerData> actor;

    @Before
    public void setup() throws Exception {
        super.setup();;
        data = new UsbManagerData();
        usbServices = mock(UsbServices.class);
        scanner = mock(UsbScanner.class);

        usbManagerContext = new ActorUsbManagerContext(usbServices, scanner, config);

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
