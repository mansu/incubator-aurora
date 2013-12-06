/*
 * Copyright 2013 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.aurora.scheduler.storage.mem;

import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;

import com.twitter.aurora.scheduler.SchedulerLifecycle;
import com.twitter.aurora.scheduler.storage.AttributeStore;
import com.twitter.aurora.scheduler.storage.JobStore;
import com.twitter.aurora.scheduler.storage.LockStore;
import com.twitter.aurora.scheduler.storage.QuotaStore;
import com.twitter.aurora.scheduler.storage.SchedulerStore;
import com.twitter.aurora.scheduler.storage.Storage;
import com.twitter.aurora.scheduler.storage.Storage.Volatile;
import com.twitter.aurora.scheduler.storage.TaskStore;
import com.twitter.common.inject.Bindings.KeyFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Binding module for an in-memory storage system.
 * <p>
 * Exposes bindings for storage components:
 * <ul>
 *   <li>{@link com.twitter.aurora.scheduler.storage.Storage}</li>
 *   <li>Keyed with keys provided by the provided{@code keyFactory}:</li>
 *     <ul>
 *       <li>{@link com.twitter.aurora.scheduler.storage.SchedulerStore}</li>
 *       <li>{@link com.twitter.aurora.scheduler.storage.JobStore}</li>
 *       <li>{@link com.twitter.aurora.scheduler.storage.TaskStore}</li>
 *       <li>{@link com.twitter.aurora.scheduler.storage.LockStore}</li>
 *       <li>{@link com.twitter.aurora.scheduler.storage.QuotaStore}</li>
 *       <li>{@link com.twitter.aurora.scheduler.storage.AttributeStore}</li>
 *     </ul>
 * </ul>
 */
public final class MemStorageModule extends PrivateModule {

  private final KeyFactory keyFactory;

  public MemStorageModule(KeyFactory keyFactory) {
    this.keyFactory = checkNotNull(keyFactory);
  }

  private <T> void bindStore(Class<T> binding, Class<? extends T> impl) {
    bind(binding).to(impl);
    bind(impl).in(Singleton.class);
    Key<T> key = keyFactory.create(binding);
    bind(key).to(impl);
    expose(key);
  }

  @Override
  protected void configure() {
    Key<Storage> storageKey = keyFactory.create(Storage.class);
    bind(storageKey).to(MemStorage.class);
    expose(storageKey);
    Key<Storage> exposedMemStorageKey = Key.get(Storage.class, Volatile.class);
    bind(exposedMemStorageKey).to(MemStorage.class);
    expose(exposedMemStorageKey);
    bind(MemStorage.class).in(Singleton.class);

    bindStore(SchedulerStore.Mutable.class, MemSchedulerStore.class);
    bindStore(JobStore.Mutable.class, MemJobStore.class);
    bindStore(TaskStore.Mutable.class, MemTaskStore.class);
    bindStore(LockStore.Mutable.class, MemLockStore.class);
    bindStore(QuotaStore.Mutable.class, MemQuotaStore.class);
    bindStore(AttributeStore.Mutable.class, MemAttributeStore.class);
  }

  @Provides
  Optional<SchedulerLifecycle> provideSchedulerLifeCycle(SchedulerLifecycle schedulerLifecycle) {
    return Optional.of(schedulerLifecycle);
  }
}
