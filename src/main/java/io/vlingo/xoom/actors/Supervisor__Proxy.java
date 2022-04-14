// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors;

import io.vlingo.xoom.common.SerializableConsumer;

public class Supervisor__Proxy implements Supervisor {
  private static final String representationInform1 = "inform(Throwable, Supervised)";

  private final Actor actor;
  private final Mailbox mailbox;

  public Supervisor__Proxy(final Actor actor, final Mailbox mailbox) {
    this.actor = actor;
    this.mailbox = mailbox;
  }

  public void inform(final Throwable throwable, final Supervised supervised) {
    if (!actor.isStopped()) {
      final SerializableConsumer<Supervisor> consumer = (actor) -> actor.inform(throwable, supervised);
      if (mailbox.isPreallocated()) { mailbox.send(actor, Supervisor.class, consumer, null, representationInform1); }
      else { mailbox.send(new LocalMessage<Supervisor>(actor, Supervisor.class, consumer, representationInform1)); }
    } else {
      actor.deadLetters().failedDelivery(new DeadLetter(actor, representationInform1));
    }
  }

  public SupervisionStrategy supervisionStrategy() {
    return null;
  }
}
