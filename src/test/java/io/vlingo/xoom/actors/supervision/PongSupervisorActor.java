// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.supervision;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.vlingo.xoom.actors.Actor;
import io.vlingo.xoom.actors.Supervised;
import io.vlingo.xoom.actors.SupervisionStrategy;
import io.vlingo.xoom.actors.Supervisor;
import io.vlingo.xoom.actors.testkit.AccessSafely;

public class PongSupervisorActor extends Actor implements Supervisor {
  public static final AtomicReference<PongSupervisorActor> instance = new AtomicReference<>();

  public final PongSupervisorTestResults testResults;

  public PongSupervisorActor() {
    System.out.println("*********** PongSupervisorActor");
    this.testResults = new PongSupervisorTestResults();
    instance.set(this);
  }

  private final SupervisionStrategy strategy =
          new SupervisionStrategy() {
            @Override
            public int intensity() {
              return 10;
            }

            @Override
            public long period() {
              return 3000;
            }

            @Override
            public Scope scope() {
              return Scope.One;
            }
          };

  @Override
  public void inform(final Throwable throwable, final Supervised supervised) {
    supervised.restartWithin(strategy.period(), strategy.intensity(), strategy.scope());
    testResults.access.writeUsing("informedCount", 1);
  }

  @Override
  public SupervisionStrategy supervisionStrategy() {
    return strategy;
  }

  public static class PongSupervisorTestResults {
    public AccessSafely access = afterCompleting(0);

    public AtomicInteger informedCount = new AtomicInteger(0);

    public AccessSafely afterCompleting(final int times) {
      access =
        AccessSafely.afterCompleting(times)
        .writingWith("informedCount", (Integer increment) -> informedCount.incrementAndGet())
        .readingWith("informedCount", () -> informedCount.get());

      return access;
    }
  }
}
