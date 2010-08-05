/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Abortable;
import org.apache.hadoop.hbase.HServerAddress;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.zookeeper.ZKUtil;
import org.apache.hadoop.hbase.zookeeper.ZooKeeperNodeTracker;
import org.apache.hadoop.hbase.zookeeper.ZooKeeperWatcher;
import org.apache.zookeeper.KeeperException;

/**
 * Manages the location of the current active Master for this RegionServer.
 * <p>
 * Listens for ZooKeeper events related to the master address. The node /master
 * will contain the address of the current master. This listener is interested
 * in NodeDeleted and NodeCreated events on /master.
 * <p>
 * Utilizes {@link ZooKeeperNodeTracker} for zk interactions.
 * <p>
 * You can get the current master via {@link #getMasterAddress()} or the
 * blocking method {@link #waitMasterAddress()}.
 */
public class MasterAddressManager extends ZooKeeperNodeTracker {
  private static final Log LOG = LogFactory.getLog(MasterAddressManager.class);

  /**
   * Construct a master address listener with the specified zookeeper reference.
   * <p>
   * This constructor does not trigger any actions, you must call methods
   * explicitly.  Normally you will just want to execute {@link #start()} to
   * begin tracking of the master address.
   *
   * @param watcher zk reference and watcher
   * @param abortable abortable in case of fatal error
   */
  public MasterAddressManager(ZooKeeperWatcher watcher, Abortable abortable) {
    super(watcher, watcher.masterAddressZNode, abortable);
  }

  /**
   * Get the address of the current master if one is available.  Returns null
   * if no current master.
   *
   * Use {@link #waitMasterAddress} if you want to block until the master is
   * available.
   * @return server address of current active master, or null if none available
   */
  public HServerAddress getMasterAddress() {
    byte [] data = super.getData();
    return data == null ? null : new HServerAddress(Bytes.toString(data));
  }

  /**
   * Check if there is a master available.
   * @return true if there is a master set, false if not.
   */
  public boolean hasMaster() {
    return super.getData() != null;
  }

  /**
   * Get the address of the current master.  If no master is available, method
   * will block until one is available, the thread is interrupted, or timeout
   * has passed.
   *
   * @param timeout maximum time to wait for master in millis, 0 for forever
   * @return server address of current active master, null if timed out
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public synchronized HServerAddress waitForMaster(long timeout)
  throws InterruptedException {
    byte [] data = super.blockUntilAvailable(timeout);
    return data == null ? null : new HServerAddress(Bytes.toString(data));
  }

  @Override
  protected Log getLog() {
    return LOG;
  }
}
