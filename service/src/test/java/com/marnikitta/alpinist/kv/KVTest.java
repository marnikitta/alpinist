package com.marnikitta.alpinist.kv;

import com.marnikitta.alpinist.model.LinkPayload;
import org.testng.annotations.Test;

public class KVTest {

  @Test
  public void testSerialize() {
    final KV kv = new KV();
    kv.put("a", "b");
    kv.put("c", "d");

    final LinkPayload payload = kv.toPayload();
    final KV kv2 = new KV(payload);
    System.out.println(kv2);
  }
}