# Android Bluetooth Service

This lib provides an android service which handles the communication with bluetooth enabled devices. A demo app is also provided to show how to integrate this service into your code.

## Build

```
./gradlew build
```

## Some details

This service relies on a fork of [RxBluetooth](https://github.com/eove/RxBluetooth) which adds some features to the upstream lib.

Communication with the service is done wih [EventBus](https://github.com/greenrobot/EventBus) to ensure a well decoupled architecture.
