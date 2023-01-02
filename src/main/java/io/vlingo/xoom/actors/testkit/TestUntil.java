// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.testkit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TestUntil {
  private CountDownLatch latch;
  
  public static TestUntil happenings(final int times) {
    final TestUntil waiter = new TestUntil(times);
    return waiter;
  }

  public void completeNow() {
    while (latch.getCount() > 0) {
      happened();
    }
  }

  public void completes() {
    try {
      latch.await();
    } catch (Exception e) {
      // ignore
    }
  }

  public boolean completesWithin(final long timeout) {
    try {
      latch.await(timeout,TimeUnit.MILLISECONDS);
      return latch.getCount() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  public TestUntil happened() {
    latch.countDown();
    return this;
  }

  public int remaining() {
    return (int) latch.getCount();
  }

  public void resetHappeningsTo(final int times) {
    this.latch = new CountDownLatch(times);
  }

  @Override
  public String toString() {
    return "TestUntil[count=" + latch.getCount() + "]";
  }

  private TestUntil(final int count) {
    this.latch = new CountDownLatch(count);
  }
}
