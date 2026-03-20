# KKMall 商城后端 API

## 项目简介

KKMall商城后端API服务，提供面向消费者的商城业务接口，包括商品浏览、购物车、订单、收货地址等功能。

## 技术栈

- **框架**: Spring Boot 3.2.2
- **Java版本**: Java 21
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **认证**: Spring Security + JWT
- **API文档**: Knife4j (Swagger增强)

## 快速启动

### 1. 环境准备

- Java 21
- Maven 3.9+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库初始化

```bash
# 连接MySQL
mysql -u root -p

# 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS kkmall DEFAULT CHARACTER SET utf8mb4;

# 执行商城表结构脚本
mysql -u root -p kkmall < src/main/resources/sql/mall_tables.sql
```

### 3. 配置文件

编辑 `src/main/resources/application.yml`，配置数据库和Redis连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kkmall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password

  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 1
```

### 4. 启动服务

```bash
# 编译
mvn clean package

# 运行
mvn spring-boot:run

# 或直接运行JAR
java -jar target/kkmall-mall-api-1.0.0.jar
```

服务启动后访问：
- **API服务**: http://localhost:38081
- **API文档**: http://localhost:38081/doc.html

## API接口

### 认证接口 (`/api/mall/auth`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/auth/register` | POST | 用户注册 | 公开 |
| `/api/mall/auth/login` | POST | 用户登录 | 公开 |
| `/api/mall/auth/info` | GET | 获取当前用户信息 | 需登录 |
| `/api/mall/auth/logout` | POST | 退出登录 | 需登录 |

### 商品接口 (`/api/mall/products`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/products` | GET | 获取商品列表 | 公开 |
| `/api/mall/products/{id}` | GET | 获取商品详情 | 公开 |
| `/api/mall/products/hot` | GET | 获取热门商品 | 公开 |
| `/api/mall/products/search` | GET | 搜索商品 | 公开 |

### 分类接口 (`/api/mall/categories`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/categories/tree` | GET | 获取分类树 | 公开 |
| `/api/mall/categories/{id}` | GET | 获取分类详情 | 公开 |

### 购物车接口 (`/api/mall/cart`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/cart` | GET | 获取购物车列表 | 需登录 |
| `/api/mall/cart` | POST | 添加到购物车 | 需登录 |
| `/api/mall/cart/{id}` | PUT | 更新数量 | 需登录 |
| `/api/mall/cart/{id}` | DELETE | 删除商品 | 需登录 |
| `/api/mall/cart/clear` | DELETE | 清空购物车 | 需登录 |
| `/api/mall/cart/select` | PUT | 批量选中/取消选中 | 需登录 |

### 订单接口 (`/api/mall/orders`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/orders` | POST | 创建订单 | 需登录 |
| `/api/mall/orders` | GET | 获取订单列表 | 需登录 |
| `/api/mall/orders/{id}` | GET | 获取订单详情 | 需登录 |
| `/api/mall/orders/{id}/cancel` | PUT | 取消订单 | 需登录 |
| `/api/mall/orders/{id}/confirm` | PUT | 确认收货 | 需登录 |
| `/api/mall/orders/{id}` | DELETE | 删除订单 | 需登录 |

### 收货地址接口 (`/api/mall/addresses`)

| 端点 | 方法 | 描述 | 权限 |
|-----|------|------|------|
| `/api/mall/addresses` | GET | 获取地址列表 | 需登录 |
| `/api/mall/addresses/{id}` | GET | 获取地址详情 | 需登录 |
| `/api/mall/addresses` | POST | 新增地址 | 需登录 |
| `/api/mall/addresses/{id}` | PUT | 更新地址 | 需登录 |
| `/api/mall/addresses/{id}` | DELETE | 删除地址 | 需登录 |
| `/api/mall/addresses/{id}/default` | PUT | 设置默认地址 | 需登录 |

## 测试流程

### 1. 注册用户

```bash
curl -X POST http://localhost:38081/api/mall/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "phone": "13800138000",
    "nickname": "测试用户"
  }'
```

### 2. 用户登录

```bash
curl -X POST http://localhost:38081/api/mall/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

返回的 `accessToken` 用于后续需要认证的接口。

### 3. 浏览商品

```bash
# 获取商品列表
curl http://localhost:38081/api/mall/products?pageNum=1&pageSize=10

# 获取商品详情
curl http://localhost:38081/api/mall/products/1

# 搜索商品
curl http://localhost:38081/api/mall/products/search?keyword=手机
```

### 4. 添加购物车

```bash
curl -X POST http://localhost:38081/api/mall/cart \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

### 5. 创建订单

```bash
# 先添加收货地址
curl -X POST http://localhost:38081/api/mall/addresses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "province": "广东省",
    "city": "深圳市",
    "district": "南山区",
    "detail": "科技园南区",
    "isDefault": true
  }'

# 创建订单
curl -X POST http://localhost:38081/api/mall/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "addressId": 1,
    "cartIds": [1, 2],
    "remark": "请尽快发货"
  }'
```

## 项目结构

```
kkmall-mall-api/
├── src/main/java/com/ab/kkmallapimall/
│   ├── KkmallMallApiApplication.java    # 应用入口
│   ├── controller/                      # 控制器层
│   │   ├── AuthController.java
│   │   ├── ProductController.java
│   │   ├── CategoryController.java
│   │   ├── CartController.java
│   │   ├── OrderController.java
│   │   └── AddressController.java
│   ├── service/                         # 业务逻辑层
│   │   ├── AuthService.java
│   │   ├── ProductService.java
│   │   ├── CategoryService.java
│   │   ├── CartService.java
│   │   ├── OrderService.java
│   │   └── AddressService.java
│   ├── mapper/                          # 数据访问层
│   ├── entity/                          # 实体类
│   ├── dto/                             # 数据传输对象
│   ├── common/                          # 通用类
│   ├── config/                          # 配置类
│   ├── security/                        # 安全相关
│   ├── exception/                       # 异常处理
│   └── util/                            # 工具类
└── src/main/resources/
    ├── application.yml                  # 配置文件
    └── sql/
        └── mall_tables.sql              # 数据库脚本
```

## 注意事项

1. **数据共享**: 商品数据由管理后台维护，商城后端只读
2. **用户分离**: 商城用户（`mall_user`）与管理后台用户（`sys_user`）独立
3. **库存管理**: 创建订单时自动扣减库存，取消订单时恢复库存
4. **订单状态**: 0-待付款，1-待发货，2-待收货，3-已完成，4-已取消
5. **JWT过期**: Token默认7天过期，需要前端处理过期情况

## Docker部署

```bash
# 构建镜像
docker build -t kkmall-mall-api:latest .

# 运行容器
docker run -d \
  -p 38081:38081 \
  -e DB_PASSWORD=your_password \
  -e REDIS_PASSWORD=your_redis_password \
  --name kkmall-mall-api \
  kkmall-mall-api:latest
```

## 开发建议

1. 使用Knife4j文档进行API测试
2. 所有需要登录的接口在请求头添加 `Authorization: Bearer {token}`
3. 统一响应格式：`{ code, message, data }`
4. 异常会被全局异常处理器捕获并返回友好提示
