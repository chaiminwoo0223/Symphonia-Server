package com.symphonia.notification.domain.publisher;

import java.util.List;

// 발송 기록의 id만 큐에 발행한다. 큐에 개인정보가 남지 않고, 컨슈머가 보내기 직전에 상태를 다시 읽게 하기 위해서다.
// 브로커가 받았다고 확인한 발송 기록의 id만 돌려준다. 돌려주지 않은 발송 기록은 발행하지 않은 것으로 본다.
public interface NotificationDeliveryPublisher {
    List<Long> publishAll(List<Long> deliveryIds);
}
