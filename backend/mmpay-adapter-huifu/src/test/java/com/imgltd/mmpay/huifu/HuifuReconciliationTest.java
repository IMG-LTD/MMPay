package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.imgltd.mmpay.gateway.TransactionStatus;
import org.junit.jupiter.api.Test;

class HuifuReconciliationTest {
  @Test
  void mapsHuifuSettlementStatusesToGatewayTransactionStatuses() {
    HuifuSettlementStatusMapper mapper = new HuifuSettlementStatusMapper();

    assertEquals(TransactionStatus.SUCCEEDED, mapper.toTransactionStatus("SUCCESS"));
    assertEquals(TransactionStatus.FAILED, mapper.toTransactionStatus("FAILED"));
  }
}
