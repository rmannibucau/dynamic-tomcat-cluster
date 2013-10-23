# Goal

Be able to add dynamically members to a tomcat cluster without multicast.

# Installation

Add `com.github.rmannibucau.tomcat.cluster.listener.DynamicClusterListener` listener:

```xml
<?xml version='1.0' encoding='utf-8'?>
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="com.github.rmannibucau.tomcat.cluster.listener.DynamicClusterListener" />

  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />

    <Engine name="Catalina" defaultHost="localhost">
      <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster"/>

      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
      </Host>
    </Engine>
  </Service>
</Server>

```

# Configuration (optional)

This is configurable by system properties:

* `rmannibucau.cluster.monitor.context` - default=/rmannibucau-tomcat-monitor - the webapp added to be able to update member list
* `rmannibucau.cluster.monitor.controller` - default=/controller - the servlet endpoint
* `rmannibucau.cluster.monitor.aliveTime` - default=0 - the aliveTime of the dynamic members
* `rmannibucau.cluster.monitor.auth` - default=NONE - the security to apply, values can be NONE, BASIC, DIGEST, CLIENT-CERT
* `rmannibucau.cluster.monitor.realm` - default=no default - the realm to associate when auth != NONE
* `rmannibucau.cluster.monitor.role` - default=system - the role used by security when auth != NONE
* `rmannibucau.cluster.monitor.transport-guarantee` - default=ONE - the transport guarantee when auth != NONE (values can be NONE or CONFIDENTIAL)
* `rmannibucau.cluster.monitor.valve` - default=no default - custom valve for the context

# Usage

Once deployed you can send a HTTP request to /rmannibucau-tomcat-monitor/controller (adapt it if you configured it).
You need to use `POST` method and the body needs to be the list of members. For instance:

```
foo1.com:1234
foo2.com:5678
foo3.com:9012
```
