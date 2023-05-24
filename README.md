### Granite XMPP Server

#### 概述
Granite XMPP Server是一个实现XMPP协议的IM服务器，具备以下特征：

* 标准兼容
* 高度模块化
* 高可用性和高扩展性
* 易于扩展和集成

##### 标准兼容

* 实现互联网标准RFC3920、RFC3921，以及多种XEP扩展协议。
* 实现TLS（SSL）、SASL等标准安全协议。

##### 高度模块化

* 所有XEP通讯协议都被封装成了插件。
* 支持在运行时，动态部署和卸载协议模块。

##### 高可用性和高扩展性

* Granite集群提供高可用性支持，在部分节点宕机的情况下，依然可以持续为应用提供服务。
* Granite集群通过简单的增加集群节点，就可以扩展应用服务能力。

##### 易于扩展和集成
* 无缝集成SpringFramework，可在Granite Component中直接注入Spring Bean，使得开发XMPP协议扩展更为简单。
* Jabber Component Protocol（XEP-0114）协议支持，易于和第三方系统集成。

#### 使用

##### 如何安装

请阅读[《安装手册》](./docs/HOW_TO_INSTALL.md)

##### 进阶使用

请阅读《用户手册》(Coming soon....)

##### 插件开发

请阅读《开发者手册》(Coming soon....)
