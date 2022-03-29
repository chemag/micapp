# micapp

micapp is a tool to characterizate microphones in Android.


# 1. Prerequisites

For running encapp:
* adb connection to the device being tested.


# 2. Operation: Install App

Install the app:
```
$ ./gradlew installDebug
...
BUILD SUCCESSFUL in 2s
27 actionable tasks: 1 executed, 26 up-to-date
```

Check the app has been installed:
```
$ adb shell pm list packages |grep micapp
package:com.facebook.micapp
```

Uninstall the app:
```
$ adb shell cmd package uninstall com.facebook.micapp
Success
$ adb shell pm list packages |grep micapp
$
```

Build the app:
```
$ ./gradlew build
...
BUILD SUCCESSFUL in 2s
61 actionable tasks: 1 executed, 60 up-to-date
```


# 3. Operation: Get List of Available Mics and Properties

...
