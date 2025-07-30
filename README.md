# ElderMind LLM Server

基于SpringAI和智谱AI实现的视频行为分析LLM推理服务，用于老年人行为监护系统。

## 功能特性

- 🎥 **视频推理**: 支持通过URL直接访问视频片段进行智谱AI推理分析
- 🌐 **URL支持**: 直接接收视频URL，无需本地文件上传
- 🚦 **限流控制**: 智能的并发和频率限制
- 🔄 **重试机制**: 自动重试失败的推理请求
- 📊 **结果保存**: 自动保存推理结果到本地文件
- 🔧 **配置灵活**: 丰富的配置选项
- 📈 **状态监控**: 实时服务状态查询

## 快速开始

### 1. 环境要求

- Java 21+
- Maven 3.6+
- 智谱AI API Key

### 2. 配置

#### 方式一：使用 .env 文件（推荐）

1. 复制 `.env.example` 文件为 `.env`：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，填入你的智谱AI API Key：
```env
ZHIPUAI_API_KEY=your-zhipu-api-key-here
```

#### 方式二：环境变量

```bash
export ZHIPUAI_API_KEY=your-zhipu-api-key-here
```

#### 方式三：直接修改配置文件

在 `application.yml` 中配置智谱AI API Key：

```yaml
spring:
  ai:
    zhipuai:
      api-key: your-zhipu-api-key-here
```

> **注意**: 为了安全起见，建议使用 `.env` 文件或环境变量的方式配置API Key，避免将敏感信息提交到版本控制系统。

### 3. 启动服务

```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

## API 接口

### 视频推理接口

**POST** `/api/llm/inference`

请求体：
```json
{
  "videoUrl": "https://example.com/video.mp4",
  "customPrompt": "请分析视频中的行为是否存在跌倒风险"
}
```

响应：
```json
{
  "success": true,
  "result": {
    "action_analysis": {
      "detected_action": "正常行走",
      "confidence_score": 0.95,
      "action_category": "日常活动",
      "risk_level": "低",
      "description": "老人正常行走，步态稳定"
    },
    "context_analysis": {
      "environment": "室内客厅",
      "person_state": "行走状态良好",
      "objects_involved": ["沙发", "茶几"],
      "time_duration": "约5秒"
    },
    "recommendations": {
      "immediate_action": "否",
      "monitoring_level": "常规",
      "alert_type": "无",
      "suggestions": ["继续正常监控"]
    }
  },
  "rawResponse": "...",
  "inferenceTime": 2.5,
  "videoPath": "https://example.com/video.mp4",
  "timestamp": "2024-01-20T10:30:00",
  "mode": "api"
}
```

### 服务状态接口

**GET** `/api/llm/status`

响应：
```json
{
  "enabled": true,
  "mode": "api",
  "rateLimiter": {
    "enabled": true,
    "concurrent": {
      "current": 1,
      "max": 3,
      "available": 2
    },
    "rateLimits": {
      "perMinute": {
        "current": 5,
        "max": 30,
        "remaining": 25
      },
      "perHour": {
        "current": 45,
        "max": 500,
        "remaining": 455
      }
    }
  }
}
```

### 健康检查接口

**GET** `/api/llm/health`

响应：
```json
{
  "status": "UP",
  "service": "LLM Inference Service",
  "timestamp": 1705737000000
}
```

## 配置说明

### 核心配置

```yaml
llm:
  inference:
    enabled: true          # 是否启用LLM推理
    mode: api              # 推理模式：api
    model: glm-4v-plus     # 智谱AI模型名称
    timeout: 60            # 推理超时时间（秒）
    max-retries: 3         # 最大重试次数
```

### 限流配置

```yaml
llm:
  inference:
    rate-limiter:
      enabled: true                    # 是否启用限流
      max-concurrent-requests: 3       # 最大并发请求数
      max-requests-per-minute: 30      # 每分钟最大请求数
      max-requests-per-hour: 500       # 每小时最大请求数
      queue-timeout: 30                # 队列等待超时时间（秒）
      retry-delay: 1.0                 # 重试延迟时间（秒）
```

### 提示词配置

```yaml
llm:
  inference:
    prompt-config:
      system-prompt: |
        你是一个专业的视频行为分析专家...
      user-prompt: "请仔细分析这个视频中的人体动作行为"
```

## 使用示例

### cURL 示例

```bash
# 视频推理
curl -X POST http://localhost:8080/api/llm/inference \
  -H "Content-Type: application/json" \
  -d '{
    "videoUrl": "https://example.com/video.mp4",
    "customPrompt": "分析视频中是否存在跌倒行为"
  }'

# 查看状态
curl http://localhost:8080/api/llm/status

# 健康检查
curl http://localhost:8080/api/llm/health
```

### Python 示例

```python
import requests

# 视频推理
response = requests.post('http://localhost:8080/api/llm/inference', json={
    'videoUrl': 'https://example.com/video.mp4',
    'customPrompt': '分析视频中的行为风险'
})

result = response.json()
if result['success']:
    print(f"推理成功，耗时: {result['inferenceTime']}秒")
    print(f"检测到的动作: {result['result']['action_analysis']['detected_action']}")
else:
    print(f"推理失败: {result['error']}")
```

## 日志

服务日志保存在 `logs/eldermind-llm.log`，包含详细的推理过程和错误信息。

## 注意事项

1. **API Key**: 确保配置了有效的智谱AI API Key
2. **视频URL**: 只支持可公开访问的视频URL，不支持本地文件路径
3. **视频格式**: 支持常见的视频格式（MP4、AVI等）
4. **网络访问**: 确保服务器和智谱AI都能够访问视频URL
5. **限流**: 注意API调用频率限制
6. **存储**: 推理结果会保存到本地 `llm_results` 目录

## 故障排除

### 常见问题

1. **API Key 错误**: 检查智谱AI API Key是否正确配置
2. **视频无法访问**: 确认视频URL可以正常访问
3. **推理超时**: 调整 `timeout` 配置或检查网络连接
4. **限流错误**: 降低请求频率或调整限流配置

### 日志查看

```bash
tail -f logs/eldermind-llm.log
```

## 开发

### 构建

```bash
mvn clean package
```

### 运行测试

```bash
mvn test
```

### Docker 部署

```bash
# 构建镜像
docker build -t eldermind-llm-server .

# 运行容器
docker run -p 8080:8080 -e ZHIPU_API_KEY=your-api-key eldermind-llm-server
```
