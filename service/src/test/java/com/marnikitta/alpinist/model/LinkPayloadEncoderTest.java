package com.marnikitta.alpinist.model;

import com.marnikitta.alpinist.repository.LinkEncoder;
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

  @Test(invocationCount = 1000)
  public void testRandomDecodeEncode() {
    final LinkEncoder encoder = new LinkEncoder();
    final String expected = encoder.encode(new LinkGenerator().get());
    final String actual = encoder.encode(encoder.decode(expected));

    Assert.assertEquals(actual, expected);
  }
}