package com.monkeybits.edilcloud;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;

public class NotificationServiceExtension extends NotificationExtenderService {
    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult receivedResult) {
        // Read properties from result.
        return false;
    }

}