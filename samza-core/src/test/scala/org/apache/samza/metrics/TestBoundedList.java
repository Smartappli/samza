/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.samza.metrics;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import org.apache.samza.diagnostics.BoundedList;
import org.junit.Assert;
import org.junit.Test;


/**
 * Class to encapsulate test-cases for {@link BoundedList}
 */
public class TestBoundedList {

  private final static Duration THREAD_TEST_TIMEOUT = Duration.ofSeconds(10);

  private <T> BoundedList<T> getBoundedListForTest() {
    return new BoundedList<T>("sampleListGauge", 10, Duration.ofSeconds(60));
  }

  @Test
  public void basicTest() {
    BoundedList<String> boundedList = getBoundedListForTest();
    boundedList.add("sampleValue");
    Assert.assertEquals("Names should be the same", boundedList.getName(), "sampleListGauge");
    Assert.assertEquals("List sizes should match", boundedList.getValues().size(), 1);
    Assert.assertEquals("BoundedList should contain sampleGauge", boundedList.getValues().contains("sampleValue"), true);
  }

  @Test
  public void testSizeEnforcement() {
    BoundedList boundedList = getBoundedListForTest();
    for (int i = 15; i > 0; i--) {
      boundedList.add("v" + i);
    }
    Assert.assertEquals("List sizes should be as configured at creation time", boundedList.getValues().size(), 10);

    int valueIndex = 10;
    Collection<String> currentList = boundedList.getValues();
    Iterator iterator = currentList.iterator();
    while (iterator.hasNext()) {
      String gaugeValue = (String) iterator.next();
      Assert.assertTrue(gaugeValue.equals("v" + valueIndex));
      valueIndex--;
    }
  }

  @Test
  public void testThreadSafety() throws InterruptedException {
    BoundedList<Integer> boundedList = getBoundedListForTest();

    Thread thread1 = new Thread(new Runnable() {
      @Override
      public void run() {
        for (int i = 1; i <= 100; i++) {
          boundedList.add(i);
        }
      }
    });

    Thread thread2 = new Thread(new Runnable() {
      @Override
      public void run() {
        for (int i = 1; i <= 100; i++) {
          boundedList.add(i);
        }
      }
    });

    thread1.start();
    thread2.start();

    thread1.join(THREAD_TEST_TIMEOUT.toMillis());
    thread2.join(THREAD_TEST_TIMEOUT.toMillis());

    Assert.assertTrue("BoundedList should have the last 10 values", boundedList.getValues().size() == 10);
    for (Integer gaugeValue : boundedList.getValues()) {
      Assert.assertTrue("Values should have the last 10 range", gaugeValue <= 100 && gaugeValue > 90);
    }
  }
}
