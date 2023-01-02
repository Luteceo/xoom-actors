// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.supervision;

import java.util.concurrent.atomic.AtomicInteger;

import io.vlingo.xoom.actors.Actor;
import io.vlingo.xoom.actors.Supervised;
import io.vlingo.xoom.actors.SupervisionStrategy;
import io.vlingo.xoom.actors.Supervisor;
import io.vlingo.xoom.actors.testkit.AccessSafely;

public class RestartFiveInOneSupervisorActor extends Actor implements Supervisor {
  private final RestartFiveInOneSupervisorTestResults testResults;

  public RestartFiveInOneSupervisorActor(final RestartFiveInOneSupervisorTestResults testResults) {
    this.testResults = testResults;
  }

  private final SupervisionStrategy strategy =
          new SupervisionStrategy() {
            @Override
            public int intensity() {
              return 5;
            }

            @Override
            public long period() {
              return 1000;
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

  public static class RestartFiveInOneSupervisorTestResults {
    public AccessSafely access = AccessSafely.afterCompleting(0);
    public final AtomicInteger informedCount = new AtomicInteger(0);

    public AccessSafely afterCompleting(final int times) {
      access =
        AccessSafely.afterCompleting(times)
        .writingWith("informedCount", (Integer increment) -> informedCount.incrementAndGet())
        .readingWith("informedCount", () -> informedCount.get());
      return access;
    }
  }
}
