package Backend.ElectionVote.service;

import Backend.ElectionVote.entity.SystemUser;
import Backend.ElectionVote.enums.DeliveryMethod;

public interface NotificationSender {

    void send(DeliveryMethod method, SystemUser to, String subject, String body);

    void sendEmail(SystemUser to, String subject, String body);

    void sendSms(SystemUser to, String body);

    // In-app is just persistence; no transport needed here.
}
