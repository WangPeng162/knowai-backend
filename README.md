# KnowAI · 基于 Java 的 RAG 知识库问答系统

> 上传你的文档，用自然语言提问，AI 基于你的知识库回答，并给出**可溯源的引用**。

一个从 0 到 1 实现的检索增强生成（RAG）后端系统：**Java 17 + Spring Boot + LangChain4j + 通义千问 + Qdrant**，
并配有一套检索质量评估体系，用数据驱动的方式把检索命中率从 **45% 迭代到 85%**（Top-2 命中率 **100%**）。

---

## ✨ 核心特性

| 能力 | 说明 |
|------|------|
| **RAG 全链路** | 上传 → 解析（PDF/TXT）→ 语义切分 → 向量化 → 检索 → 大模型生成 |
| **两级检索** | embedding 粗召回 Top-20 → rerank 精排 Top-2，兼顾召回率与精度 |
| **Query Rewriting** | 多轮对话指代消解：把"它有什么缺点？"补全为"RAG 有什么缺点？"再检索 |
| **引用溯源** | 每次回答返回引用来源（文档名 / 页码 / 相似度） |
| **Agent 工具调用** | 模型自主决定何时检索（LangChain4j AiServices + `@Tool`） |
| **多轮记忆** | 按 sessionId 隔离的会话记忆（MessageWindowChatMemory） |
| **评估体系** | 20+3 题评估集 + HitRate@K / MRR 量化 + 失败题归因 |
| **内容隔离** | 知识库/文档归属校验；未指定知识库时只检索"自己的全部知识库" |
| **删除与级联清理** | 删除文档/知识库时同步清理 Qdrant 向量与映射，不留孤儿数据 |

## 📊 效果数据（同一份评估集，可复现）

| 迭代 | 做法 | HitRate@1 | HitRate@2 | MRR |
|------|------|-----------|-----------|-----|
| v0 | 纯 embedding 检索（基线） | 45% | 75% | 0.674 |
| v1 | + rerank 精排 | 60% | 90% | 0.767 |
| v2 | + 语义切分升级（recursive） | 80% | 95% | 0.875 |
| v3 | + 评估数据集修正 | **85%** | **100%** | 0.925 |
| v4 | + Query Rewriting（指代题） | 指代题 @1 **33% → 100%** | 100% | 1.000 |

> 三条关键结论：① **切分质量是检索的地基**（切分一改，@1 直接 +20%）；② **rerank 治排序不治召回**（@5 不变、把答案顶前）；③ **优化要靠数据说话**，每一步改动都有评估数字兜底。
> 完整实验记录见 [docs/eval-baseline.md](docs/eval-baseline.md)，踩坑与设计决策见 [docs/lessons.md](docs/lessons.md)。

## 🧱 技术栈

| 层 | 选型 |
|----|------|
| 语言/框架 | Java 17 · Spring Boot 4.1 · MyBatis-Plus 3.5 |
| 存储 | MySQL 8（业务数据）· Qdrant（向量）· 阿里云 OSS（原始文件） |
| AI | LangChain4j 1.18 · 通义千问 `qwen-plus`（对话）· `text-embedding-v4`（向量化）· `gte-rerank-v2`（精排） |
| 鉴权 | JWT（jjwt 0.12）+ ThreadLocal 用户上下文 |
| 前端 | Vue 3 · Vite · Element Plus · axios |
| 文档 | springdoc-openapi（Swagger UI） |

## 🏗 请求链路

```
上传文档
  └─ OSS 存原件 → 写 knowledge_document
解析（POST /document/parse/{id}）
  └─ DocumentParser(PDF/TXT) → DocumentPage → RecursiveChunkSplitter(语义切分)
     → knowledge_chunk 入库 → 批量向量化 → Qdrant 写入 + knowledge_embedding 建立映射

问答（POST /chat）
  └─ 归属校验 → 取会话记忆 → QueryRewriter(指代消解) → Embedding 粗召回 Top-20(knowledgeId 过滤)
     → rerank 精排 Top-2 → 拼上下文 → 大模型生成 → 返回答案 + references
```

## 🚀 快速开始

**前置依赖**

- JDK 17、Maven（项目自带 `mvnw`）
- MySQL 8（建库 `knowai`）
- Qdrant：`docker run -d -p 6333:6333 -p 6334:6334 qdrant/qdrant`

**配置**

`src/main/resources/application-local.yml`（已加入 `.gitignore`，需自行创建）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowai?useUnicode=true&characterEncoding=utf8
    username: <你的数据库账号>
    password: <你的数据库密码>
```

环境变量（对话/向量化/精排均依赖通义千问）：

```bash
export DASHSCOPE_API_KEY=<阿里云百炼 API Key>
```

**启动后端**

```bash
./mvnw spring-boot:run          # 默认 8080，Swagger: http://localhost:8080/swagger-ui.html
```

**启动前端**

```bash
cd knowai-frontend
npm install
npm run dev                     # http://localhost:5173（Vite 已配置 /api 代理到 8080）
```

**接口自测（curl）**

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}' | jq -r .data.token)

# 建库 → 上传 → 解析
curl -X POST localhost:8080/knowledge -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"我的知识库"}'
curl -X POST localhost:8080/knowledge/document -H "Authorization: Bearer $TOKEN" \
  -F "knowledgeId=1" -F "file=@./demo.pdf"
curl -X POST localhost:8080/document/parse/1 -H "Authorization: Bearer $TOKEN"

# 提问（带多轮）
curl -X POST localhost:8080/chat -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"question":"这份文档讲了什么？","knowledgeId":1,"sessionId":"s-1"}'
```

**评估（检索质量回归测试）**

```bash
./mvnw test -Dtest=EvalRunner    # 需要 MySQL + Qdrant + DASHSCOPE_API_KEY
```

## 📁 项目结构

```
src/main/java/com/knowai/knowaibackend
├── agent/          # Agent 工具（KnowledgeSearchTools：粗召回 + rerank 两级检索）
├── parser/         # 文档解析（PdfParser / TxtParser）
├── splitter/       # 文本切分（RecursiveChunkSplitter：LangChain4j recursive）
├── service/        # 业务编排（ChatService / DocumentParseService / KnowledgeEditService …）
├── controller/     # REST 接口（Auth / Knowledge / KnowledgeDocument / Chat / DocumentParse）
├── entity/ dto/ vo/# 数据模型
├── mapper/         # MyBatis-Plus Mapper
├── filter/         # JWT 鉴权过滤器
└── common/         # UserContext（ThreadLocal）、统一返回 Result、分页

eval/               # 评估集（dataset.json）
docs/               # 评估实验记录 + 踩坑沉淀
knowai-frontend/    # Vue 3 前端（知识库管理 + 对话 + 引用展示）
```

## 🔍 精选设计决策

- **多知识库隔离靠元数据过滤**：向量入库时写入 `knowledgeId`，检索时按范围过滤；未指定知识库时收紧为"当前用户自己的全部知识库"（而非全站）。
- **删除顺序铁律：先删向量、再删映射、最后删 chunk**——反序会找不到 vectorId，导致 Qdrant 里留下无法追溯的孤儿向量。
- **跨数据源不做强事务**：MySQL 与 Qdrant 无法用单一事务保证一致性，因此不加 `@Transactional`，改用"顺序 + 补偿/清理"策略，并把风险显式记录。
- **编排层独立**：删除文档/知识库需要同时操作多个 Service，为避免 Spring 循环依赖，统一放在最上层编排服务（依赖别人、不被依赖）。
- **评估集与文档版本绑定**：删除文档前必须检查评估集依赖——否则会"悄悄删掉标准答案"（真实踩过）。

## 🗺 后续规划

- [ ] 混合检索（embedding + BM25 分数融合）
- [ ] 会话记忆持久化到 Redis（替换当前的内存 Map）
- [ ] 异步向量化（当前同步，大文档解析耗时）
- [ ] 敏感内容过滤与更细粒度的权限模型

---

**作者备注**：本项目为个人学习与作品集项目，聚焦 RAG 检索质量工程（切分 / 精排 / 改写 / 评估）与后端工程实践（鉴权、隔离、级联清理、依赖分层）。
