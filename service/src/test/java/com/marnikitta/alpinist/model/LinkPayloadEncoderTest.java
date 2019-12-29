package com.marnikitta.alpinist.model;

import com.marnikitta.alpinist.service.LinkEncoder;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LinkPayloadEncoderTest {
  @Test(invocationCount = 1000)
  public void testRandomEncodeDecode() {
    final LinkPayload expected = new LinkGenerator().get();
    final LinkEncoder encoder = new LinkEncoder();
    final LinkPayload actual = encoder.decode(encoder.encode(expected));

    Assert.assertEquals(actual, expected);
  }
}