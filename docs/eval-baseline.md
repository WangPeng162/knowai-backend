# RAG 检索质量基线（Baseline）

> 评估对象：Embedding(text-embedding-v4) + Qdrant 检索链路，不含 Agent/生成层（隔离变量）
> 数据集：eval/dataset.json，20 题，knowledgeId=22（广西农业职业技术大学介绍.pdf，documentId=17，共 19 个 chunk）

## 指标定义

| 指标 | 含义 |
|------|------|
| HitRate@K | Top-K 检索结果中命中该题答案线索的题目占比 |
| MRR | 第一个命中 chunk 排名倒数的平均值 |

判分口径：
- chunk 命中 = chunk 文本包含该题任一 answerKeyword（OR，忽略大小写）
- loose=true（宽松题）：Top-K 拼接文本含任一关键词即命中
- loose=false（严格题，多关键词答案）：Top-K 拼接文本须覆盖全部关键词

## 运行方式

前提：MySQL 已启动、Qdrant(localhost:6334) 已启动、环境变量 DASHSCOPE_API_KEY 已配置。

```
mvnw.cmd test -Dtest=EvalRunner
```

## 基线分数

**2026-09-06 · 基线（RecursiveChunkSplitter 默认参数，Top-5 检索，knowledgeId 过滤）**

| 指标 | 分数 |
|------|------|
| HitRate@1 | 9/20 = **45.0%** |
| HitRate@2 | 15/20 = **75.0%** |
| HitRate@5 | 19/20 = **95.0%** |
| MRR | **0.6742** |

参考：线上 Agent 实际只用 Top-2 → 真实线上命中率约 75%。

## 实验 1：rerank 精排对比（2026-09-08）

**配置**：粗召回 Top-20（embedding，knowledgeId 过滤）→ gte-rerank-v2 打分重排，同一召回池对比两条流水线（EvalRunner.runCompare 一次跑出）

| 指标 | embedding 直排 | embedding + rerank | 提升 |
|------|---------------|-------------------|------|
| HitRate@1 | 45.0%（9/20） | **60.0%（12/20）** | **+15.0%** |
| HitRate@2 | 75.0%（15/20） | **90.0%（18/20）** | **+15.0%** |
| HitRate@5 | 95.0%（19/20） | 95.0%（19/20） | 0（@5 已到上限，Q8 数据缺失救不回） |
| MRR | 0.6742 | **0.7667** | **+0.0925** |

**结论**：
1. **rerank 治排序不治召回**——@5 不变（都能找到），但答案被系统性"顶到前面"（@1/@2 各 +15%）。线上 Agent 只看 Top-2 → **线上命中率 75% → 90%**，这是实打实的体验提升
2. **排名提前 6 题**：Q3(4→2)、Q7(2→1)、Q11(5→2)、Q12(5→2)、Q18(3→1)、Q20(2→1)——正是基线里"答案排太靠后"的那批弱命中题（年份类、长关键词类）
3. **轻微退化 2 题**：Q10(1→2)、Q2(2→3)——rerank 并非完美，未来可做 embedding 分数与 rerank 分数的 hybrid 融合排序
4. 工程落地：rerank API 失败时降级回 embedding 排序（KnowledgeSearchTools 已加 try-catch + 分数数量防御）

## 后续对比实验计划

1. ✅ rerank 精排（已完成，+15% @1/@2）
2. chunk 大小调整 / chunk 清洗（当前 19 个 chunk 有截断杂质、句子腰斩，是 @5=95% 的天花板之下最大的存量问题）
3. rerank 与 embedding 分数 hybrid 融合（观察能否消除 Q10/Q2 的退化）
4. query rewriting（多轮指代消解）前后对比 → 观察 Q14-Q17 类指代题
5. 数据集修正：Q8 关键词改为"两万多人"（或移除）
