/**
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
package org.apache.ambari.server.state.alerts;

import junit.framework.Assert;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.listeners.AlertServiceStateListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests that {@link AmbariEvent} instances are fired correctly and that alert
 * data is bootstrapped into the database.
 */
public class AlertEventPublisherTest {

  private AlertDispatchDAO dispatchDao;
  private AlertDefinitionDAO definitionDao;
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private AmbariMetaInfo metaInfo;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // force singleton init via Guice so the listener registers with the bus
    injector.getInstance(AlertServiceStateListener.class);

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);

    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    metaInfo.init();

    clusterName = "foo";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP", "2.0.6"));
    Assert.assertNotNull(cluster);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * Tests that a default {@link AlertGroupEntity} is created when a service is
   * installed.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultAlertGroupCreation() throws Exception {
    Assert.assertEquals(0, dispatchDao.findAllGroups().size());
    installHdfsService();
    Assert.assertEquals(1, dispatchDao.findAllGroups().size());
  }

  /**
   * Tests that all {@link AlertDefinitionEntity} instances are created for the
   * installed service.
   * 
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionInsertion() throws Exception {
    Assert.assertEquals(0, definitionDao.findAll().size());
    installHdfsService();
    Assert.assertEquals(4, definitionDao.findAll().size());
  }

  /**
   * Calls {@link Service#persist()} to mock a service install.
   */
  private void installHdfsService() throws Exception {
    String serviceName = "HDFS";
    Service service = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(service);
    service.persist();
    service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
  }
}
