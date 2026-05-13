# Spring Cloud Alibaba Nacos 配置与服务发现完整教程

> 本教程由浅入深，带你从零掌握 Nacos 作为配置中心和服务注册发现中心的使用方法。

---

## 目录

1. [Nacos 简介](#1-nacos-简介)
2. [环境准备](#2-环境准备)
3. [本地如何连接到 Nacos](#3-本地如何连接到-nacos)
4. [如何通过 Nacos 管理配置](#4-如何通过-nacos-管理配置)
5. [服务注册与发现](#5-服务注册与发现)
6. [配置动态刷新](#6-配置动态刷新)
7. [多环境管理（Namespace / Group）](#7-多环境管理namespace--group)
8. [生产环境最佳实践](#8-生产环境最佳实践)
9. [常见问题排查](#9-常见问题排查)

---

## 1. Nacos 简介

**Nacos**（Dynamic Naming and Configuration Service）是阿里巴巴开源的一个集**服务注册发现**和**配置管理**于一体的平台。

### 核心功能

| 功能 | 说明 |
|------|------|
| 配置管理 | 集中管理应用配置，支持动态刷新，无需重启 |
| 服务发现 | 服务实例自动注册与发现，支持健康检查 |
| 动态 DNS | 基于权重的路由策略 |
| 服务健康检测 | 阻止请求发送到不健康的实例 |

### 核心概念

- **Namespace（命名空间）**：用于隔离不同环境（如 dev / test / prod）
- **Group（分组）**：同一命名空间下对配置/服务的进一步分类
- **Data ID**：配置文件的唯一标识，通常为 `应用名.后缀`
- **Service**：一组提供相同功能的实例集合

---

## 2. 环境准备

### 2.1 下载并启动 Nacos Server

```bash
# 下载 Nacos（以 2.4.x 为例）
wget https://github.com/alibaba/nacos/releases/download/2.4.3/nacos-server-2.4.3.zip
unzip nacos-server-2.4.3.zip
cd nacos/bin

# 单机模式启动（开发环境）
# Linux/Mac
sh startup.sh -m standalone

# Windows
startup.cmd -m standalone
```

启动成功后访问控制台：http://127.0.0.1:8848/nacos

默认账号密码：`nacos` / `nacos`

### 2.2 Docker 方式启动（推荐开发环境）

```bash
docker run -d \
  --name nacos-server \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v2.4.3
```

> **注意**：9848 端口是 gRPC 通信端口，2.x 版本必须开放。

### 2.3 验证 Nacos 是否正常运行

```bash
curl -X GET "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=test&group=DEFAULT_GROUP"
```

如果返回空或 JSON 结果，说明 Nacos 运行正常。

---

## 3. 本地如何连接到 Nacos

### 3.1 引入依赖

在 `pom.xml` 中添加 Spring Cloud Alibaba Nacos 相关依赖：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2025.1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Nacos 配置中心 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        <version>2025.1.0.0</version>
    </dependency>

    <!-- Nacos 服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        <version>2025.1.0.0</version>
    </dependency>

    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 3.2 在 `application.yaml` 中书写配置

```yaml
spring:
  application:
    name: nacos-config-demo  # 在 Nacos 中展示的应用名称
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848  # Nacos Server 地址
      discovery:
        username: nacos
        password: nacos
        namespace: public       # 命名空间，默认 public
        group: DEFAULT_GROUP    # 分组，默认 DEFAULT_GROUP
  config:
    import:
      # 从 Nacos 导入配置文件
      - nacos:datasource.properties?group=DATABASE&refreshEnabled=true
      - nacos:feature-flags.properties?group=DEFAULT_GROUP&refreshEnabled=true

server:
  port: 18084
```

### 3.3 配置项详细说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.cloud.nacos.server-addr` | Nacos 服务端地址 | 无（必填） |
| `spring.cloud.nacos.discovery.username` | 认证用户名 | 无 |
| `spring.cloud.nacos.discovery.password` | 认证密码 | 无 |
| `spring.cloud.nacos.discovery.namespace` | 命名空间 ID | public |
| `spring.cloud.nacos.discovery.group` | 服务分组 | DEFAULT_GROUP |
| `spring.config.import` | 要导入的 Nacos 配置列表 | 无 |

### 3.4 `spring.config.import` 语法解析

```
nacos:<dataId>?group=<GROUP>&refreshEnabled=<true|false>
```

- `dataId`：Nacos 中配置文件的 Data ID（如 `datasource.properties`）
- `group`：配置所属分组
- `refreshEnabled`：是否开启动态刷新（修改配置后自动生效）

---

## 4. 如何通过 Nacos 管理配置

### 4.1 通过控制台创建配置

1. 访问 http://127.0.0.1:8848/nacos ，使用 nacos/nacos 登录
2. 进入 **配置管理** → **配置列表**
3. 点击右上角 **+** 号创建配置：

**示例 1：数据源配置**

| 字段 | 值 |
|------|------|
| Data ID | `datasource.properties` |
| Group | `DATABASE` |
| 配置格式 | `Properties` |

配置内容：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/mydb?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

**示例 2：功能开关配置**

| 字段 | 值 |
|------|------|
| Data ID | `feature-flags.properties` |
| Group | `DEFAULT_GROUP` |
| 配置格式 | `Properties` |

配置内容：

```properties
feature.newUI.enabled=true
feature.darkMode.enabled=false
feature.maxRetryCount=3
```

### 4.2 通过 Open API 管理配置

Nacos 提供 RESTful API 用于程序化操作配置。

#### 发布配置

```bash
curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs" \
  -d "dataId=datasource.properties" \
  -d "group=DATABASE" \
  -d "content=spring.datasource.url=jdbc:mysql://127.0.0.1:3306/mydb"
```

#### 获取配置

```bash
curl -X GET "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=datasource.properties&group=DATABASE"
```

#### 删除配置

```bash
curl -X DELETE "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=datasource.properties&group=DATABASE"
```

#### 监听配置变化

```bash
curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs/listener" \
  -H "Long-Pulling-Timeout: 30000" \
  -d "Listening-Configs=datasource.properties^DATABASE^^tenant^md5值"
```

### 4.3 通过 Java SDK 管理配置

```java
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import java.util.Properties;
import java.util.concurrent.Executor;

public class NacosConfigExample {

    public static void main(String[] args) throws Exception {
        String serverAddr = "127.0.0.1:8848";
        String dataId = "datasource.properties";
        String group = "DATABASE";

        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("username", "nacos");
        properties.put("password", "nacos");

        // 创建 ConfigService
        ConfigService configService = NacosFactory.createConfigService(properties);

        // 获取配置
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println("当前配置内容：\n" + content);

        // 监听配置变化
        configService.addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
                return null; // 使用默认线程
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("配置已更新：\n" + configInfo);
            }
        });

        // 发布配置
        boolean published = configService.publishConfig(dataId, group,
            "spring.datasource.url=jdbc:mysql://localhost:3306/newdb");
        System.out.println("发布结果：" + published);

        // 保持程序运行以接收变更通知
        Thread.sleep(Long.MAX_VALUE);
    }
}
```

### 4.4 在 Spring Boot 中读取 Nacos 配置

#### 方式一：使用 `@Value` 注解

```java
@RestController
@RefreshScope  // 必须加此注解才能动态刷新
public class ConfigController {

    @Value("${feature.newUI.enabled:false}")
    private boolean newUIEnabled;

    @Value("${feature.maxRetryCount:1}")
    private int maxRetryCount;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("newUIEnabled", newUIEnabled);
        result.put("maxRetryCount", maxRetryCount);
        return result;
    }
}
```

#### 方式二：使用 `@ConfigurationProperties` 绑定

```java
@Component
@ConfigurationProperties(prefix = "spring.datasource")
@Data  // Lombok
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}
```

#### 方式三：使用 `Environment` 对象

```java
@Service
public class FeatureService {

    @Autowired
    private Environment environment;

    public boolean isFeatureEnabled(String featureName) {
        return Boolean.parseBoolean(
            environment.getProperty("feature." + featureName + ".enabled", "false")
        );
    }
}
```

---

## 5. 服务注册与发现

### 5.1 服务自动注册

引入 `spring-cloud-starter-alibaba-nacos-discovery` 后，应用启动时会**自动注册**到 Nacos。

在启动类上确保有 `@EnableDiscoveryClient`（Spring Boot 3.x 中可省略）：

```java
@SpringBootApplication
@EnableDiscoveryClient
public class NacosConfigDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosConfigDemoApplication.class, args);
    }
}
```

启动后在 Nacos 控制台 → **服务管理** → **服务列表** 中可看到注册的实例。

### 5.2 服务间调用（使用 RestTemplate + LoadBalancer）

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // 启用负载均衡
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

```java
@Service
public class OrderService {

    @Autowired
    private RestTemplate restTemplate;

    public String getProductInfo(Long productId) {
        // 使用服务名替代 IP + 端口
        String url = "http://product-service/api/products/" + productId;
        return restTemplate.getForObject(url, String.class);
    }
}
```

### 5.3 使用 OpenFeign 进行声明式调用（推荐）

添加依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

定义 Feign 客户端：

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id);

    @GetMapping("/api/products")
    List<ProductDTO> listProducts();
}
```

启用 Feign：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## 6. 配置动态刷新

### 6.1 原理说明

Nacos 客户端通过**长轮询（Long Polling）**机制监听配置变化：

```
客户端 → 发起长轮询请求 → Nacos Server
            ↓ 30s 超时或配置变更
客户端 ← 返回变更的 dataId 列表
客户端 → 拉取最新配置内容
```

### 6.2 开启动态刷新

在 `spring.config.import` 中设置 `refreshEnabled=true`：

```yaml
spring:
  config:
    import:
      - nacos:feature-flags.properties?group=DEFAULT_GROUP&refreshEnabled=true
```

### 6.3 使用 `@RefreshScope` 实现热更新

```java
@RestController
@RefreshScope
@RequestMapping("/api/features")
public class FeatureController {

    @Value("${feature.newUI.enabled:false}")
    private boolean newUIEnabled;

    @GetMapping("/new-ui")
    public ResponseEntity<Map<String, Object>> checkNewUI() {
        return ResponseEntity.ok(Map.of(
            "newUIEnabled", newUIEnabled,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
```

在 Nacos 控制台修改 `feature.newUI.enabled` 的值后，无需重启服务，再次调用接口即可获取最新值。

### 6.4 监听配置变更事件

```java
@Component
@Slf4j
public class NacosConfigChangeListener {

    @NacosConfigListener(dataId = "feature-flags.properties", groupId = "DEFAULT_GROUP")
    public void onConfigChanged(String newConfig) {
        log.info("配置发生变更: {}", newConfig);
        // 执行自定义逻辑，如清除缓存、重建连接池等
    }
}
```

或使用 Spring Event 方式：

```java
@Component
@Slf4j
public class RefreshEventListener {

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        log.info("配置刷新完成，RefreshScope Bean 已重建");
    }
}
```

---

## 7. 多环境管理（Namespace / Group）

### 7.1 Namespace 隔离环境

**推荐做法**：每个环境使用独立的 Namespace。

在 Nacos 控制台 → **命名空间** 中创建：

| 命名空间名 | 命名空间 ID（自动生成） |
|------------|------------------------|
| dev | `a1b2c3d4-xxxx-xxxx-xxxx` |
| test | `e5f6g7h8-xxxx-xxxx-xxxx` |
| prod | `i9j0k1l2-xxxx-xxxx-xxxx` |

配置文件中使用 Namespace ID：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        namespace: a1b2c3d4-xxxx-xxxx-xxxx  # dev 环境的 namespace ID
```

### 7.2 Group 细分配置

在同一 Namespace 下，使用 Group 对配置进行逻辑分类：

```yaml
spring:
  config:
    import:
      - nacos:datasource.properties?group=DATABASE&refreshEnabled=true
      - nacos:redis.properties?group=CACHE&refreshEnabled=true
      - nacos:feature-flags.properties?group=FEATURE&refreshEnabled=true
      - nacos:logging.properties?group=LOGGING&refreshEnabled=true
```

### 7.3 多环境配置方案对比

| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| Namespace 隔离 | 环境级别隔离（dev/test/prod） | 完全隔离，互不影响 | 配置无法跨命名空间共享 |
| Group 分类 | 同环境下按业务模块分类 | 灵活分组，易于管理 | 隔离性不如 Namespace |
| Data ID 后缀 | 按 profile 区分配置 | 与 Spring Profile 天然契合 | 配置文件数量可能较多 |

### 7.4 使用 Spring Profile 结合 Nacos

```yaml
# application.yaml
spring:
  profiles:
    active: dev

---
# application-dev.yaml
spring:
  cloud:
    nacos:
      server-addr: dev-nacos.internal:8848
      discovery:
        namespace: dev-namespace-id

---
# application-prod.yaml
spring:
  cloud:
    nacos:
      server-addr: prod-nacos.internal:8848
      discovery:
        namespace: prod-namespace-id
```

---

## 8. 生产环境最佳实践

### 8.1 Nacos 集群部署

生产环境**必须**使用集群模式（至少 3 节点）：

```bash
# conf/cluster.conf
192.168.1.10:8848
192.168.1.11:8848
192.168.1.12:8848
```

使用外部数据库（MySQL）持久化：

```properties
# conf/application.properties
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://mysql-host:3306/nacos?characterEncoding=utf8&connectTimeout=1000
db.user.0=nacos
db.password.0=secure_password
```

### 8.2 安全配置

```yaml
# 生产环境开启鉴权
nacos:
  core:
    auth:
      enabled: true
      system:
        type: nacos
      token:
        secret:
          key: "YourBase64EncodedSecretKeyAtLeast32Bytes..."  # 至少32字节的Base64密钥
        expire:
          seconds: 18000
```

### 8.3 配置加密

敏感配置（数据库密码、API Key 等）应加密存储：

```java
// 使用 Jasypt 加密
@Configuration
public class EncryptionConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(System.getenv("JASYPT_ENCRYPTOR_PASSWORD"));
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setConfig(config);
        return encryptor;
    }
}
```

Nacos 中的加密配置写法：

```properties
spring.datasource.password=ENC(encrypted_password_here)
```

### 8.4 配置版本管理

- Nacos 自带**历史版本**功能，可在控制台查看配置变更历史
- 支持**回滚**到任意历史版本
- 建议结合 Git 管理配置文件的变更审计

### 8.5 灰度发布配置

Nacos 支持 **Beta 发布**，可以将配置变更先推送给部分实例：

1. 在控制台编辑配置时选择 "Beta 发布"
2. 指定目标 IP 列表
3. 验证通过后再全量发布

---

## 9. 常见问题排查

### 9.1 连接失败

**现象**：`Client not connected, current status: STARTING`

**排查步骤**：

```bash
# 1. 检查 Nacos 是否运行
curl http://127.0.0.1:8848/nacos/

# 2. 检查 gRPC 端口是否可达（默认 server-addr 端口 + 1000）
telnet 127.0.0.1 9848

# 3. 检查防火墙规则
sudo iptables -L -n | grep 8848
sudo iptables -L -n | grep 9848
```

### 9.2 配置无法动态刷新

**检查清单**：

1. `refreshEnabled=true` 是否设置
2. Bean 上是否添加了 `@RefreshScope`
3. 配置是否用 `@Value` 注入（static 字段不支持刷新）
4. 是否使用了 `@ConfigurationProperties`（天然支持刷新，无需 `@RefreshScope`）

### 9.3 服务注册后立即下线

**常见原因**：

- 应用启动后很快退出（缺少 Web 依赖）
- 健康检查失败（检查 actuator 端点）

```yaml
# 确保健康检查正常
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

### 9.4 多网卡环境注册 IP 错误

```yaml
spring:
  cloud:
    nacos:
      discovery:
        ip: 192.168.1.100        # 手动指定注册 IP
        # 或使用网卡名
        network-interface: eth0
```

### 9.5 日志调试

开启 Nacos 客户端详细日志：

```yaml
logging:
  level:
    com.alibaba.nacos: DEBUG
    com.alibaba.cloud: DEBUG
```

---

## 完整项目结构参考

```
nacos-config-demo/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/demo/
│       │   ├── NacosConfigDemoApplication.java
│       │   ├── controller/
│       │   │   └── ConfigController.java
│       │   ├── config/
│       │   │   └── DataSourceProperties.java
│       │   └── listener/
│       │       └── NacosConfigChangeListener.java
│       └── resources/
│           └── application.yaml
```

---

## Reference

- [Nacos 官方文档 - Java SDK](https://nacos.io/docs/latest/guide/user/sdk/#%E8%8E%B7%E5%8F%96%E9%85%8D%E7%BD%AE)
- [Spring Cloud Alibaba - Nacos 快速开始](https://sca.aliyun.com/docs/2023/user-guide/nacos/quick-start/)
- [Nacos GitHub](https://github.com/alibaba/nacos)
- [Spring Cloud Alibaba GitHub](https://github.com/alibaba/spring-cloud-alibaba)

---

> **提示**：本教程基于 Spring Cloud Alibaba 2025.1.0.0 版本编写，不同版本配置方式可能略有差异，请以官方文档为准。
