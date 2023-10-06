## Granite安装向导

### Granite简介
Granite是一个开源XMPP服务器，具有有以下特征：
* XMPP兼容
* 高性能
* 高可用
* 跨平台
* 高度模块化
* 易于部署

### 部署模式
Granite XMPP Server提供两种部署模式：
* **Granite Lite**<br>
使用Granite Lite部署模式，系统被部署在一个单节点（物理机、虚拟机、Docker）上，仅依赖JVM。<br><br>
Granite Lite部署模式，适用于开发、测试环境，或者用于一些非关键应用场合（用户数量 < 1000，不强调系统的高可用性）。

* **Granite Cluster**<br>
使用Granite Cluster部署模式，系统由多个节点组成，具备高可用性和高扩展性。<br><br>
在严格的产品环境，我们总是推荐使用Granite Cluster部署版本，以保证系统的高性能、高可用及数据安全。

* **版本实现差异**<br>
Granite Lite和Granite Cluster部署版本，使用相同的通讯层和协议层实现，所以这两种部署形态对于客户端是完全透明的。<br><br>
Granite Cluster部署版本实现了Session Manager、Cache Manager、Router、
Resource Service组件等的多节点共享版本，而Granite Lite部署版本相关实现为单节点版本。<br><br>
Granite Cluster部署版本持久层采用MongoDB数据库，而Granite Lite部署版本使用一个嵌入式的数据库HSQL Server。

### Granite Lite部署
#### 前置准备
* ** 安装JDK或JRE **<br>
Granite Lite依赖JDK 11或者JRE 11。你需要将JDK或者JRE先安装到系统中。

#### 下载
点击链接下载 [Granite Lite Standard](https://github.com/TheFirstLineOfCode/granite/releases/download/1.0.5-RELEASE/granite-lite-standard-1.0.5-RELEASE.zip)

#### 安装
解压granite-lite-standard-1.0.5-RELEASE.zip。

#### 检查
进入解压后的系统安装目录，运行启动命令：
```
cd granite-lite-standard-1.0.5-RELEASE
java -jar granite-server-1.0.5-RELEASE.jar -console
```
启动Granite Lite XMPP Server后，可以在Granite Server Console里，执行services命令，检查Services是否都正常启动了。
```
$ services
```
如果看到以下的内容，说明Granite XMPP Server已经正常启动了。
```
$ services
id      State           Disabled        Service ID
0       Available       No              stream.service
1       Available       No              processing.service
2       Available       No              routing.service
3       Available       No              event.service
4       Available       No              parsing.service
$
```
可以在Granite Server Console中执行exit命令，终止Granite XMPP Server运行并退出Console。
```
$ exit
```
#### 配置
* **配置文件**
Granite的配置文件保存在$GRANITE_HOME/configuration目录下，包括以下配置文件：
	* server.ini
	* standard-component-binding-lite.ini
	* components.ini
	* plugins.ini

|      文件名                        |                  说明         |
|-----------------------------------|-------------------------------|
| server.ini    	               | 服务器主配置文件                  |
| standard-component-binding-lite.ini   | 服务器组件依赖管理配置文件             | 
| components.ini                    | 服务器组件参数配置文件                |
| plugins.ini              | 服务器插件参数配置文件             |

* **配置Domain**
根据XMPP规范要求，每个XMPP Server必须指定Domain。用户可以在在server.ini中配置Domain：
```
domain.name=im.mydomain.com
```

> 如果你仅是在自己的局域网测试Granite Lite，并未注册Internet域名，可将domain设置为服务器的局域网IP地址。

如果我们使用MUC（Multi-User Chat）服务，还需要将MUC服务域名配置为domain.alias.names：
```
domain.alias.names=muc.im.mydomain.com
```

* **配置服务器组件配置参数**
可以在components.ini配置文件中，修改服务器组件的配置参数。例如，我们需要修改XMPP Server启动端口号为5333（默认端口号为5222），可以在components.ini文件中socket.message.receiver章节，设置参数port：
```
[socket.message.receiver]
port=5333
```

* **配置服务器插件配置参数**
可以在plugins.ini配置文件中，修改服务器插件的配置参数。例如，我们需要修改MUC服务的域名，可以在plugins.ini文件中xeps.muc章节，设置参数muc.domain.name：
```
[xeps.muc]
muc.domain.name=muc.im.mydomain.com
```
**注**：这里的xeps.muc是要配置的插件的插件ID。 

### Granite Cluster部署

#### 概述
**Granite Cluster**<br>
Granite Cluster提供了Granite Lite不具备的以下特性：
* 可扩展性
系统可以由一组节点组成，可以通过增加节点的方式，提升系统的性能。
* 高可用性
当系统中某些节点出现故障时，系统依然能够保障服务。
<img src="./granite_cluster_architecture.png" width="800" height="640"/>

**部署Granite Cluster**<br>
为简化Granite Cluster的部署，系统被设计成集中式部署的模式。

* 管理节点(Management Node)<br>
管理节点集中管理系统的配置、升级。管理节点包含了以下内容：<br>
	* 配置方案(Deploy Plan)<br>
	配置方案描述了集群的部署形态，包括应用节点类型、节点上部署的协议、数据库配置等。<br><br>
	配置方案是用户部署系统时主要使用的交互接口，用户通过维护配置方案来部署、升级Granite Cluster。

	* 插件库(Plugins Repository)<br>
	Granite Cluster基于插件架构设计，系统功能模块被拆分打包成插件，用户可以按照实际的需求，选择需要部署的功能（插件），部署到应用节点上去。<br><br>
	管理节点的插件库里，包括了系统所有的可用插件，以满足多样化的部署方案。

	* 部署器(Deployer)<br>
	部署器读取配置方案，根据配置方案生成应用节点的运行时。并提供下载服务，允许应用节点下载和部署运行时。<br><br>

	> **注：** 管理节点仅在系统部署期使用，当部署完成后，可以从系统中移除管理节点，这并不会影响系统对外提供的XMPP服务。

* 应用节点(Application Node)<br>
应用节点是为系统提供XMPP服务的应用层组件。<br><br>
应用节点在启动时，会连接到管理节点，检查部署方案是否有更新。如果应用节点并未部署运行时，或者部署方案有了更新，应用节点会根据部署方案，从管理节点下载合适的运行时并进行部署。<br><br>
当运行时被正确部署后，应用节点启动运行时，正式成为集群中提供服务的节点。<br><br>
部署期结束后，管理节点可能会被从系统中移除。应用节点在本地保存了部署方案和依赖的运行时，当应用节点重启时，会读取本地的部署方案和运行时，正确启动。
<img src="./granite_cluster_deploying.png" width="800" height="640"/>

#### 安装Granite Cluster MgtNode
#####  前置准备
**安装JDK或JRE**
Granite Cluster MgtNode依赖JDK 11或者JRE 11。你需要将JDK或者JRE先安装到系统中。

##### 下载Granite Cluster MgtNode
Granite Cluster MgtNode [点击下载](https://github.com/TheFirstLineOfCode/granite/releases/download/1.0.5-RELEASE/granite-cluster-nodes-mgtnode-1.0.5-RELEASE.zip)

##### 安装
解压granite-cluster-nodes-mgtnode-1.0.5-RELEASE.zip。

##### 配置
Granite Cluster MgtNode的核心配置文件是$GRANITE_CLUSTER_MGTNODE_HOME/configuration/deploy-plan.ini，用户在此文件中定义系统的部署方案。

* ** 定义domain **
根据XMPP规范要求，每个XMPP Server必须指定Domain。用户可以在在deploy-plan.ini的cluster章节中配置Domain：

[cluster]
domain-name=im.example.com

> 如果你仅是在自己的局域网测试Granite Cluster，并未注册Internet域名，可以将domain设置为服务器的IP地址。

如果我们使用MUC（Multi-User Chat）服务，还需要将MUC服务域名配置为domain.alias.names：

```
domain.alias.names=muc.im.mydomain.com
```

* ** 选择部署协议 **

在app-node章节的设置XMPP服务支持的Protocol：

```
[app-node]
protocols=ibr, im, muc
```

* ** 设置数据库参数 **

Granite Cluster依赖MongoDB，假设我们已经安装了MongoDB，并创建了用户，需要在deploy-plan.ini的db章节配置数据库相关信息：

```
[db]
addresses=192.168.1.10:27017
db-name=granite
user-name=granite
password=mypassword
```

> 关于Granite Cluster部署方案配置的更多信息，请参考[Granite用户手册](http://www.firstlinecode.com/lithosphere/granite/docs/user_guide.html)

##### 检查
```
cd granite-cluster-nodes-mgtnode-1.0.5-RELEASE
java -jar granite-cluster-nodes-mgtnode-1.0.5-RELEASE.jar
```

正常启动后，可以看到类似以下的提示，说明MgtNode已经正常启动。
```
21:23:14.876 [main] INFO  c.c.g.cluster.node.mgtnode.Starter - Management node has joined the cluster.
21:23:15.376 [main] INFO  c.c.g.cluster.node.mgtnode.Starter - Starting console...
Commands:
help        Display help information.
exit        Exit system.
$
```

#### 安装Granite Cluster AppNode
#####  前置准备
**安装JDK或JRE**
Granite Cluster MgtNode依赖JDK 11或者JRE 11。你需要将JDK或者JRE先安装到系统中。

##### 下载
Granite Cluster AppNode [点击下载](https://github.com/TheFirstLineOfCode/granite/releases/download/1.0.5-RELEASE/granite-cluster-nodes-appnode-1.0.5-RELEASE.zip)

##### 安装
解压granite-cluster-nodes-appnode-1.0.5-RELEASE.zip。

##### 检查
```
cd granite-cluster-nodes-appnode-1.0.5-RELEASE
java -jar granite-cluster-nodes-appnode-1.0.5-RELEASE.jar
```

AppNode会自动连接到MgtNode，下载部署方案和运行时，并且启动运行时。

启动运行时后，可以在Granite Server Console里，运行services，检查services是否正常启动了。
```
$ services
```

如果看到以下的内容，说明Granite XMPP Server已经正常启动了。
```
$ services
id      State           Service ID
0       Available       stream.service
1       Available       processing.service
2       Available       routing.service
3       Available       event.service
4       Available       parsing.service
$
```

可以在Granite Server Console中执行exit命令，终止Granite XMPP Server运行。
```
$ exit
```

#### 配置Load Balancer
理论上，可以使用任何支持TCP协议的Load Balancer，我们以gobetween([http://gobetween.io](http://gobetween.io/))为例。

##### 安装
下载并安装gobetween，并安装到负责Load Balancing的机器节点上。

##### 配置
假设Load Balancer主机地址如下：
* 192.168.1.101。

我们有3个AppNode地址如下：
* 192.168.1.102
* 192.168.1.103
* 192.168.1.104

我们需要修改${GOBETWEEN_HOME}/config/gobetween.toml配置文件，配置以下的内容：
[servers]

[servers.granite-cluster]
protocol = "tcp"
bind = "192.168.1.101:5222"
balance="roundrobin"

[servers.granite-cluster.discovery]
kind = "static"
static_list = [
	"192.168.1.102:5222",
	"192.168.1.103:5222",
	"192.168.1.104:5222"
]

##### 检查
使用以下命令启动Load Balancer：
```
gobetween -c ./config/gobetween.toml
```

现在Granite Cluster已经配置完成了，由于Granite是一个XMPP标准兼容的服务器实现，可以使用标准的XMPP客户端连接到192.168.1.101:5222，并测试其功能。

### 从源码构建
#### 前置准备
* ** 安装JDK或JRE **<br>
Granite Lite依赖JDK 11或者JRE 11。你需要将JDK或者JRE先安装到系统中。

* ** 安装Maven **
Granite依赖Maven进行构建。你需要将Apache Maven 3.3.9+先安装到系统中。

#### 构建Granite
##### 下载代码
```
git clone https://github.com/TheFirstLineOfCode/granite.git
```

##### 构建所有插件
```
cd granite
mvn clean install
```

##### 打包Granite Lite
```
cd pack/lite
mvn clean package
cd target
java -jar granite-pack-lite-1.0.5-RELEASE.jar
```

在target目录下会看到打包好的granite-lite-standard-1.0.5-RELEASE.zip。

##### 打包Granite Cluster
* ** 打包MgtNode **
```
cd granite
cd pack/cluster-mgtnode
mvn clean package
cd target
java -jar granite-pack-cluster-mgtnode-1.0.5-RELEASE.jar
```

在target目录下会看到打包好的granite-cluster-nodes-mgtnode-1.0.5-RELEASE.zip。

* ** 打包AppNode **
```
cd granite
cd cluster/nodes/appnode
mvn clean package
```

在target目录下会看到打包好的granite-cluster-nodes-appnode-1.0.5-RELEASE.zip和granite-cluster-nodes-appnode-1.0.5-RELEASE.tar.gz。
