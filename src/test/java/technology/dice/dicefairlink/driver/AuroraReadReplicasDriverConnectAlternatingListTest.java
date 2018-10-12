/*
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package technology.dice.dicefairlink.driver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.AmazonRDSAsyncClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterMember;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Properties;
import technology.dice.dicefairlink.driver.AuroraReadReplicasDriverConnectTest.StepByStepExecutor;

@RunWith(PowerMockRunner.class)
public class AuroraReadReplicasDriverConnectAlternatingListTest {

  private static final String VALID_JDBC_URL = "jdbc:auroraro:mysql://aa:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_LOW_JDBC_URL_A = "jdbc:mysql://replica-1-ro:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_LOW_JDBC_URL_B = "jdbc:mysql://replica-2-ro:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_ENDPOINT_ADDRESS_A = "replica-1-ro";
  private static final String VALID_ENDPOINT_ADDRESS_B = "replica-2-ro";

  @Test
  @PrepareForTest({DriverManager.class, Regions.class, AmazonRDSAsyncClient.class, AmazonRDSAsyncClientBuilder.class, AuroraReadReplicasDriver.class})
  public void canConnectToValidUrlBasicAuth_thenListOfReplicasChanges() throws Exception {
    final String stubInstanceId_A = "123";
    final String stubInstanceId_B = "345";

    final Properties validProperties = new Properties();
    validProperties.put("replicaPollInterval", "1");
    validProperties.put("auroraDiscoveryAuthMode", "basic");
    validProperties.put("auroraDiscoveryKeyId", "TestAwsKey123");
    validProperties.put("auroraDiscoverKeySecret", "TestAwsSecret123");
    validProperties.put("auroraClusterRegion", "eu-west-1");

    final AmazonRDSAsyncClientBuilder mockAmazonRDSAsyncClientBuilder = mock(AmazonRDSAsyncClientBuilder.class);
    final AmazonRDSAsync mockAmazonRDSAsync = mock(AmazonRDSAsync.class);
    final DescribeDBClustersResult mockDescribeDBClustersResult = mock(DescribeDBClustersResult.class);
    final DBCluster mockDbCluster = mock(DBCluster.class);
    final DBClusterMember mockDbClusterMember_A = mock(DBClusterMember.class);
    final DBClusterMember mockDbClusterMember_B = mock(DBClusterMember.class);
    final DescribeDBInstancesResult mockDbInstancesResult_A = mock(DescribeDBInstancesResult.class);
    final DescribeDBInstancesResult mockDbInstancesResult_B = mock(DescribeDBInstancesResult.class);
    final DBInstance mockDbInstance_A = mock(DBInstance.class);
    final DBInstance mockDbInstance_B = mock(DBInstance.class);
    final Endpoint mockEndpoint_A = mock(Endpoint.class);
    final Endpoint mockEndpoint_B = mock(Endpoint.class);
    final Driver mockDriver = mock(Driver.class);

    PowerMock.mockStatic(DriverManager.class);
    DriverManager.registerDriver(EasyMock.anyObject(AuroraReadReplicasDriver.class));
    PowerMock.expectLastCall();
    EasyMock.expect(DriverManager.getDriver(VALID_LOW_JDBC_URL_A)).andReturn(mockDriver);
    EasyMock.expect(DriverManager.getDriver(VALID_LOW_JDBC_URL_B)).andReturn(mockDriver);
    PowerMock.replay(DriverManager.class);

    PowerMockito.mockStatic(AmazonRDSAsyncClient.class);
    PowerMockito.when(AmazonRDSAsyncClient.asyncBuilder()).thenReturn(mockAmazonRDSAsyncClientBuilder);

    Mockito.when(mockAmazonRDSAsyncClientBuilder.withRegion(Regions.EU_WEST_1.getName())).thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.withCredentials(any(AWSCredentialsProvider.class))).thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.build()).thenReturn(mockAmazonRDSAsync);
    Mockito.when(mockAmazonRDSAsync.describeDBClusters(any(DescribeDBClustersRequest.class))).thenReturn(mockDescribeDBClustersResult);
    Mockito.when(mockDescribeDBClustersResult.getDBClusters()).thenReturn(Arrays.asList(mockDbCluster));

    Mockito.when(mockDbCluster.getDBClusterMembers())
        .thenReturn(Arrays.asList(mockDbClusterMember_A, mockDbClusterMember_B));
    Mockito.when(mockDbClusterMember_A.isClusterWriter()).thenReturn(false);
    Mockito.when(mockDbClusterMember_A.getDBInstanceIdentifier()).thenReturn(stubInstanceId_A);

    Mockito.when(mockDbClusterMember_B.isClusterWriter()).thenReturn(false);
    Mockito.when(mockDbClusterMember_B.getDBInstanceIdentifier()).thenReturn(stubInstanceId_B);

    Mockito.when(mockAmazonRDSAsync.describeDBInstances(Mockito.any(DescribeDBInstancesRequest.class)))
        .thenReturn(mockDbInstancesResult_A)
        .thenReturn(mockDbInstancesResult_B)
        .thenReturn(mockDbInstancesResult_A)
        .thenReturn(mockDbInstancesResult_B)
        .thenReturn(mockDbInstancesResult_A)
        .thenReturn(mockDbInstancesResult_B);
    Mockito.when(mockDbInstancesResult_A.getDBInstances()).thenReturn(Arrays.asList(mockDbInstance_A));
    Mockito.when(mockDbInstancesResult_B.getDBInstances()).thenReturn(Arrays.asList(mockDbInstance_B));
    Mockito.when(mockDbInstance_A.getEndpoint()).thenReturn(mockEndpoint_A);
    Mockito.when(mockDbInstance_A.getDBInstanceStatus()).thenReturn("available");
    Mockito.when(mockDbInstance_B.getEndpoint()).thenReturn(mockEndpoint_B);
    Mockito.when(mockDbInstance_B.getDBInstanceStatus()).thenReturn("available");
    Mockito.when(mockEndpoint_A.getAddress()).thenReturn(VALID_ENDPOINT_ADDRESS_A);
    Mockito.when(mockEndpoint_B.getAddress()).thenReturn(VALID_ENDPOINT_ADDRESS_B);

    final StepByStepExecutor stepByStepExecutor = new StepByStepExecutor(1);
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver(stepByStepExecutor);
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);
    stepByStepExecutor.step();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);
    stepByStepExecutor.step();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);

    verify(mockDbClusterMember_A, times(3)).isClusterWriter(); // 3 - because of: Init step + 2 execution steps in stepByStepExecutor
    verify(mockDbClusterMember_A, times(3)).getDBInstanceIdentifier();
    verify(mockDbInstance_A, times(3)).getEndpoint();
    verify(mockEndpoint_A, times(3)).getAddress();

    verify(mockDbClusterMember_B, times(3)).isClusterWriter();
    verify(mockDbClusterMember_B, times(3)).getDBInstanceIdentifier();
    verify(mockDbInstance_B, times(3)).getEndpoint();
    verify(mockEndpoint_B, times(3)).getAddress();
  }

}