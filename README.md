# Snice Commons

Even Super Nice things have things in Common.

Also because it seems that you cannot have a single project without a commons/utils/stuff project so this is that one place for all of that junk you have been hording over the years but have had nowhere to put it. This is that place. The only requirement - all Snice Common things can only be dependent on the JVM, nothing else.

## Issues connecting to modem

### Despite being in dialout, I cannot connect

If your user is indeed in the correct user group and therefore should have access to the modem but you still cannot connect to it, then it may be that the `ModemManager` (on Ubuntu - perhaps on Linux in general) is interfering. Solution, just turn it off:

```service ModemManager stop```

## References

*  ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250)


## Terminology

* DTE - Data Terminal Equipment
* DCE - Data Circuit-terminating Equipment (i.e. the modem)
