#!/bin/bash
#firebase appdistribution:distribute build/app/outputs/apk/release/app-release.apk --app 1:1081576602759:android:b9fff8e8b1c67fe6d8cc22 --groups "admin" --release-notes "Android pure app"  # --testers "bellinitom97@gmail.com"
firebase appdistribution:distribute app/build/outputs/apk/debug/app-debug.apk --app 1:1081576602759:android:b9fff8e8b1c67fe6d8cc22 --groups "admin" --release-notes "fix social login"  # --testers "bellinitom97@gmail.com"