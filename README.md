# Android RxBluetooth Service

[![Build Status](https://travis-ci.org/eove/android-rxbluetooth-service.svg?branch=master)](https://travis-ci.org/eove/android-rxbluetooth-service)

This lib provides an android service which handles the communication with bluetooth enabled devices. A demo app is also provided to show how to integrate this service into your code. The **Rx** stands for ReactiveX as this service is based on the [Ivan Baranov's RxBluetooth](https://github.com/IvBaranov/RxBluetooth/) library.

## Build

```
./gradlew build
```

## Some details

This service relies on a fork of [RxBluetooth](https://github.com/eove/RxBluetooth) which adds some features to the upstream lib.

Communication with the service is done wih [EventBus](https://github.com/greenrobot/EventBus) to ensure a well decoupled architecture.

## Disclaimer

This lib was tested on my *quite old now* Moto X 2013 device running `android 5.1`. You should be able to change build options to run it on latest Android versions.

## Demo app screenshots

![Device discovery](./images/discovery.png?raw=true "Device discovery")

![Device logs](./images/device_logs.png?raw=true "Device logs")


