package com.monkeybits.edilcloud;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationReceivedResult;
import com.onesignal.OneSignal;

public class NotificationServiceExtension extends NotificationExtenderService {
    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult receivedResult) {
        // Read properties from result.
        OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSRemoteNotificationReceivedHandler fired!" +
                " with OSNotificationReceived: " + receivedResult.toString());
        return false;
    }

}