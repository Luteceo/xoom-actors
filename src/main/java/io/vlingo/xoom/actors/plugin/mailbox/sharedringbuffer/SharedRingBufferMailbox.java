// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.plugin.mailbox.sharedringbuffer;

import io.vlingo.xoom.actors.*;
import io.vlingo.xoom.common.SerializableConsumer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SharedRingBufferMailbox implements Mailbox {
  private final AtomicBoolean closed;
  private final Dispatcher dispatcher;
  private final int mailboxSize;
  private final Message[] messages;
  private final boolean notifyOnSend;
  private final AtomicLong sendIndex;
  private final AtomicLong readyIndex;
  private final AtomicLong receiveIndex;

  @Override
  public void close() {
    if (!closed.get()) {
      closed.set(true);
      dispatcher.close();
    }
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public boolean isDelivering() {
    throw new UnsupportedOperationException("SharedRingBufferMailbox does not support this operation.");
  }

  @Override
  public boolean isPreallocated() {
    return true;
  }

  @Override
  public int concurrencyCapacity() {
    return 1;
  }

  @Override
  public void resume(final String name) {
    // TODO: Consider supporting Stowage here
    throw new UnsupportedOperationException("SharedRingBufferMailbox does not support this operation.");
  }

  @Override
  public void send(final Message message) {
    throw new UnsupportedOperationException("Use preallocated mailbox send(Actor, ...).");
  }

  @Override
  public void suspendExceptFor(final String name, final Class<?>... overrides) {
    // TODO: Consider supporting Stowage here
    throw new UnsupportedOperationException("SharedRingBufferMailbox does not support this operation.");
  }

  @Override
  public boolean isSuspended() {
    return false;
  }

  @Override
  public void send(final Actor actor, final Class<?> protocol, final SerializableConsumer<?> consumer, final Returns<?> returns, final String representation) {
    final long messageIndex = sendIndex.incrementAndGet();
    final int ringSendIndex = (int) (messageIndex % mailboxSize);

    int retries = 0;
    while (ringSendIndex == (int) (receiveIndex.get() % mailboxSize)) {
      if (++retries >= mailboxSize) {
        if (closed.get()) {
          return;
        } else {
          retries = 0;
        }
      }
    }

    messages[ringSendIndex].set(actor, protocol, consumer, returns, representation);

    while (!readyIndex.compareAndSet(messageIndex - 1, messageIndex))
      ;

    if (notifyOnSend) {
      dispatcher.execute(this);
    }
  }

  @Override
  public Message receive() {
    final long messageIndex = receiveIndex.get();

    if (messageIndex < readyIndex.get()) {
      final int index = (int) (receiveIndex.incrementAndGet() % mailboxSize);

      return messages[index];
    }

    return null;
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("SharedRingBufferMailbox does not support this operation.");
  }

  /* @see io.vlingo.xoom.actors.Mailbox#pendingMessages() */
  @Override
  public int pendingMessages() {
    throw new UnsupportedOperationException("SharedRingBufferMailbox does not support this operation");
  }

  protected SharedRingBufferMailbox(final Dispatcher dispatcher, final int mailboxSize, final boolean notifyOnSend) {
    this.dispatcher = dispatcher;
    this.mailboxSize = mailboxSize;
    this.closed = new AtomicBoolean(false);
    this.messages = new Message[mailboxSize];
    this.readyIndex = new AtomicLong(-1);
    this.receiveIndex = new AtomicLong(-1);
    this.sendIndex = new AtomicLong(-1);
    this.notifyOnSend = notifyOnSend;

    initPreallocated();
  }

  private void initPreallocated() {
    for (int idx = 0; idx < mailboxSize; ++idx) {
      messages[idx] = new LocalMessage<>(this);
    }
  }
}
