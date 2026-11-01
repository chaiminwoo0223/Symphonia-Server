package com.symphonia.notification.application.provider;

import com.symphonia.notification.application.dto.command.NotifySettlementsCommand;
import com.symphonia.notification.application.dto.result.NotificationTargetResult;
import java.util.List;

// 정산 메시지 발송을 시작할 때 누구에게 어떤 정산 건을 알릴지 정한다. 정산 건과 분담금은 gathering이 갖고 있어 gathering이 구현한다.
// 발송 기록을 만드는 트랜잭션 안에서 부른다. 정산 건에 발송 시각을 남기는 일과 발송 기록을 만드는 일이 함께 커밋되기 때문이다.
public interface NotificationTargetProvider {
    // 아직 정산 메시지를 보내지 않은 정산 건에 발송 시각을 남기고, 미입금 분담금이 있는 참여자마다 담을 정산 건을 돌려준다.
    // 보낼 정산 건이 없거나 미입금 분담금이 없으면 비어 있다.
    List<NotificationTargetResult> notifySettlements(NotifySettlementsCommand command);
}
