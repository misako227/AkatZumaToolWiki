# Repomap 生成提示词指南

---

## 一、什么是 Repomap

Repomap 是一份**精简的代码库全局地图**，仅包含模块划分、类/函数签名、调用关系等骨架信息，体积通常为原代码库的 1–5%。AI 每次工作前先读地图，自主判断需要深入哪个文件，避免盲目遍历全量代码。

---

## 二、生成 Repomap 的提示词

将以下提示词发送给 AI，即可让它为你的项目生成 Repomap：

---

### 提示词（完整版）

```
你是一个代码库分析专家。请分析我提供的代码库，生成一份结构化的 Repomap 文档。

## 你的任务

1. **扫描所有源码文件**，提取以下信息：
   - 模块 / 目录的职责说明（一句话）
   - 每个文件导出的类、函数、常量签名（不含函数体）
   - 文件之间的关键依赖关系（import/require/调用）
   - 标注"高重要性"符号（被多处引用、核心逻辑、对外接口）

2. **按以下格式输出 Markdown 文件**，保存为 `REPOMAP.md`，放在根目录REPOMAP文件夹下。

## 输出格式规范

### 顶层结构

# REPOMAP — {项目名}
> 生成时间：{日期}  |  版本：{git commit 或版本号}

## 项目概览
{2–3 句话描述项目整体用途和技术栈}

## 目录结构
{只列出 src/ 下的目录树，不展开文件内容，最多 5 层}

## 模块索引
{每个模块一节，格式见下方}

### 文件节格式（每个源码文件一节）

#### `路径/文件名.ts`
**职责**：{一句话}

| 符号 | 类型 | 签名 | 重要性 |
|------|------|------|--------|
| `FunctionName` | function | `(param: Type): ReturnType` | ⭐ 高 |
| `ClassName` | class | `class Foo extends Bar` | 普通 |
| `CONST_NAME` | const | `string` | 普通 |

**对外依赖**：`moduleA`, `moduleB`
**被以下引用**：`moduleC`, `moduleD`

---

## 调用关系图（核心链路）
{用 Mermaid flowchart 绘制最重要的 3–5 条调用链}

```mermaid
flowchart LR
  A[入口] --> B[核心逻辑] --> C[数据层]
```

---

## 高重要性符号汇总
{列出所有标注 ⭐ 高重要性的符号，方便快速检索}

## 注意事项

- **不要**复制函数体、注释或实现细节
- **不要**包含测试文件和构建产物（node_modules、dist、.git）
- 签名中的类型尽量保留，有助于 AI 理解接口契约
- 如果项目有多个入口（如微服务），每个入口单独分节
- 文件超过 500 个时，只索引 src/ 核心目录，忽略 utils/helpers 中的低重要性工具函数

请开始分析并输出 REPOMAP.md。
```

---

## 三、增量更新提示词

代码发生变更后，用以下提示词**只更新改动部分**，而非重新生成全量：

```
当前 REPOMAP.md 如下：
{粘贴现有 REPOMAP 内容}

以下文件发生了变更：
{粘贴 git diff --name-only 的输出，或直接列出改动文件}

请：
1. 仅更新 REPOMAP 中涉及上述文件的节
2. 如有新增/删除文件，在目录结构和模块索引中同步修改
3. 重新检查调用关系图是否需要更新
4. 在文件顶部更新"生成时间"和"版本"字段
5. 输出完整的新版 REPOMAP.md
```

---

## 四、使用 Repomap 进行精准定位的提示词

生成 Repomap 后，每次让 AI 处理任务时，在提示词开头加上：

```
以下是项目的 REPOMAP，请先阅读它，再决定需要查看哪些具体文件：

{粘贴 REPOMAP.md 内容}

---

我的任务是：{描述具体需求}

请按以下步骤操作：
1. 根据 REPOMAP 定位相关模块和文件（列出文件路径）
2. 只读取你列出的文件，不要遍历其他文件
3. 完成任务后，如果有新增/修改的符号，告诉我需要更新 REPOMAP 的哪些部分
```

---

## 五、自动化维护建议

| 场景 | 操作 |
|------|------|
| 每次 PR 合并后 | 在 CI 中运行增量更新提示词，自动提交新的 REPOMAP.md |
| 大规模重构后 | 重新运行完整版生成提示词 |
| 团队协作 | 将 REPOMAP.md 纳入版本控制，像对待代码一样维护它 |
| 与 Claude Code 配合 | 在 `CLAUDE.md` 中引用 REPOMAP.md 的路径，让 AI 每次自动读取 |

---

## 六、示例：REPOMAP 片段

下方是一个 Node.js 项目的 REPOMAP 示例，供参考：

```markdown
# REPOMAP — my-api-server
> 生成时间：2025-05-25  |  版本：a3f9c12

## 项目概览
基于 Express + TypeScript 的 RESTful API 服务，使用 PostgreSQL 存储数据，
JWT 鉴权，支持多租户。

## 目录结构
src/
├── controllers/   # 路由处理层
├── services/      # 业务逻辑层
├── repositories/  # 数据访问层
├── models/        # 数据模型定义
├── middleware/    # 中间件
└── utils/         # 工具函数

## 模块索引

#### `src/services/user.service.ts`
**职责**：用户账号的创建、查询、权限校验核心逻辑

| 符号 | 类型 | 签名 | 重要性 |
|------|------|------|--------|
| `UserService` | class | `class UserService` | ⭐ 高 |
| `getUserById` | method | `(id: string): Promise<User>` | ⭐ 高 |
| `createUser` | method | `(dto: CreateUserDto): Promise<User>` | ⭐ 高 |
| `validatePermission` | method | `(userId: string, resource: string): Promise<boolean>` | ⭐ 高 |

**对外依赖**：`UserRepository`, `JwtService`
**被以下引用**：`UserController`, `AuthMiddleware`
```

---

*将此文件提交到仓库根目录，团队成员和 AI 均可直接使用。*
