// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.plugin.mailbox.agronampscarrayqueue;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import io.vlingo.xoom.actors.Dispatcher;
import io.vlingo.xoom.actors.Mailbox;
import io.vlingo.xoom.actors.Message;

public class ManyToOneConcurrentArrayQueueMailbox implements Mailbox {
  private final Dispatcher dispatcher;
  private final boolean notifyOnSend;
  private final ManyToOneConcurrentArrayQueue<Message> queue;
  private final int totalSendRetries;

  @Override
  public void close() {
    dispatcher.close();
    queue.clear();
  }

  @Override
  public boolean isClosed() {
    return dispatcher.isClosed();
  }

  @Override
  public boolean isDelivering() {
    throw new UnsupportedOperationException("ManyToOneConcurrentArrayQueueMailbox does not support this operation.");
  }

  @Override
  public int concurrencyCapacity() {
    return 1;
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("ManyToOneConcurrentArrayQueueMailbox does not support this operation.");
  }

  @Override
  public void resume(final String name) {
    // TODO: Consider supporting Stowage here
    System.out.println("WARNING: ManyToOneConcurrentArrayQueueMailbox does not support resume(): " + name);
  }

  @Override
  public void send(final Message message) {
    // This code causes a deadlock when (1) the queue is full and (2) the actor tries to send a message to itself.
    // To avoid this, any write to full queue needs to raise an exception.
    for (int tries = 0; tries < totalSendRetries; tries++) {
      if (queue.offer(message)) {
        if (notifyOnSend) {
          dispatcher.execute(this);
        }
        return;
      }
    }
    throw new IllegalStateException("Count not enqueue message due to busy mailbox.");
  }

  @Override
  public void suspendExceptFor(final String name, final Class<?>... overrides) {
    // TODO: Consider supporting Stowage here
    if (!name.equals(Mailbox.Stopping)) {
      System.out.println("WARNING: ManyToOneConcurrentArrayQueueMailbox does not support suspendExceptFor(): " + name + " overrides: " + overrides);
    }
  }

  @Override
  public boolean isSuspended() {
    return false;
  }

  @Override
  public final Message receive() {
    return queue.poll();
  }

  /* @see io.vlingo.xoom.actors.Mailbox#pendingMessages() */
  @Override
  public int pendingMessages() {
    return queue.size();
  }

  protected ManyToOneConcurrentArrayQueueMailbox(final Dispatcher dispatcher, final int mailboxSize, final int totalSendRetries, final boolean notifyOnSend) {
    this.dispatcher = dispatcher;
    this.queue = new ManyToOneConcurrentArrayQueue<>(mailboxSize);
    this.totalSendRetries = totalSendRetries;
    this.notifyOnSend = notifyOnSend;
  }
}
