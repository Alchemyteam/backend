# XGBoost 推荐打分机制分析

## 当前系统状态

### 现有排序机制
1. **向量搜索 (Qdrant)**: 基于语义相似度，但**未提取分数**
2. **传统搜索 (MySQL)**: 关键词匹配，**无明确排序**
3. **结果合并**: 简单去重，**未重新排序**

### 问题
- 搜索结果排序不够智能
- 无法结合多个信号（相似度、价格、销量、品牌等）
- 没有个性化推荐

---

## 是否需要 XGBoost？

### ❌ **当前不建议引入 XGBoost**

#### 原因 1: 缺少训练数据
- ❌ 无用户行为数据（点击、购买、评分）
- ❌ 无标注数据（相关性标注）
- ❌ 无用户画像数据
- ✅ 有历史订单数据 (`SalesData` 表)

#### 原因 2: 系统复杂度
- 需要模型训练、部署、定期重训
- 需要特征工程和数据管道
- 需要监控和 A/B 测试
- 增加运维成本

#### 原因 3: 有更简单的方案
- 基于规则的排序（加权融合）
- 提取 Qdrant 相似度分数
- 结合历史销量、价格、品牌等特征

---

## 推荐方案：分阶段实施

### 🎯 **阶段 1: 基于规则的排序（推荐先做）**

#### 优势
- ✅ **无需训练数据**，立即可用
- ✅ **实现简单**，易于调试
- ✅ **可解释性强**，便于调整
- ✅ **性能好**，无模型推理开销

#### 实现思路

**1. 提取 Qdrant 相似度分数**
```java
// Qdrant 返回结果包含 score 字段
{
  "result": [
    {"id": "xxx", "score": 0.95},
    {"id": "yyy", "score": 0.87}
  ]
}
```

**2. 设计加权融合公式**
```java
finalScore = 
  w1 * vectorSimilarityScore +      // 向量相似度 (0-1)
  w2 * keywordMatchScore +           // 关键词匹配度 (0-1)
  w3 * priceScore +                  // 价格合理性 (0-1)
  w4 * salesVolumeScore +            // 历史销量 (0-1)
  w5 * brandPopularityScore          // 品牌受欢迎度 (0-1)

// 权重示例
w1 = 0.4  // 语义相似度最重要
w2 = 0.3  // 关键词匹配次之
w3 = 0.1  // 价格
w4 = 0.1  // 销量
w5 = 0.1  // 品牌
```

**3. 特征计算示例**
```java
// 向量相似度: 直接使用 Qdrant score
double vectorScore = qdrantResult.getScore();

// 关键词匹配度: 计算匹配字段数量
double keywordScore = calculateKeywordMatch(product, query);

// 价格合理性: 基于历史价格分布
double priceScore = calculatePriceScore(product.getPrice(), historicalAvgPrice);

// 历史销量: 基于交易次数
double salesScore = normalizeSalesVolume(product.getTransactionCount());

// 品牌受欢迎度: 基于品牌总销量
double brandScore = calculateBrandPopularity(product.getBrandCode());
```

#### 预期效果
- 搜索结果更相关
- 热门/优质产品优先展示
- 价格合理的产品优先

---

### 🎯 **阶段 2: 如果效果不够好，再考虑 XGBoost**

#### 前提条件
1. ✅ 收集了足够的用户行为数据
   - 用户点击日志
   - 用户购买记录
   - 用户评分/反馈

2. ✅ 构建了特征工程
   - 用户特征（历史购买、偏好品类）
   - 产品特征（价格、品牌、销量）
   - 交互特征（用户-产品匹配度）

3. ✅ 有标注数据或隐式反馈
   - 点击 = 正样本
   - 购买 = 强正样本
   - 无交互 = 负样本

#### XGBoost 模型设计

**1. 特征列表**
```java
// 查询特征
- queryLength
- queryType (product_name, category, brand, etc.)

// 产品特征
- itemCode
- itemName
- brandCode
- category
- price
- historicalSalesVolume
- avgPrice
- priceVariance

// 相似度特征
- vectorSimilarityScore
- keywordMatchScore
- categoryMatchScore

// 用户特征（如果有）
- userPurchaseHistory
- userPreferredBrands
- userPriceRange

// 交互特征
- queryProductMatchScore
- brandQueryMatchScore
```

**2. 训练目标**
- **Learning to Rank (LTR)**: 学习排序
- **点击率预测 (CTR)**: 预测用户点击概率
- **转化率预测 (CVR)**: 预测用户购买概率

**3. 模型训练**
```python
import xgboost as xgb

# 准备训练数据
X_train = extract_features(queries, products, user_behaviors)
y_train = extract_labels(user_behaviors)  # 点击/购买 = 1, 未交互 = 0

# 训练模型
model = xgb.XGBRanker(
    objective='rank:pairwise',
    learning_rate=0.1,
    max_depth=6,
    n_estimators=100
)
model.fit(X_train, y_train, group=query_groups)

# 保存模型
model.save_model('product_ranking_model.json')
```

**4. Java 集成**
```java
// 使用 XGBoost4J (Java 绑定)
import ml.dmlc.xgboost4j.java.XGBoost;

// 加载模型
Booster booster = XGBoost.loadModel("product_ranking_model.json");

// 预测
float[] features = extractFeatures(query, product);
float[][] batch = new float[][]{features};
DMatrix dmatrix = new DMatrix(batch, null);
float[][] predictions = booster.predict(dmatrix);
double score = predictions[0][0];
```

---

## 实施建议

### 立即行动（阶段 1）
1. ✅ **修改 `QdrantService.searchSimilar()`** 提取分数
2. ✅ **创建 `ProductRankingService`** 实现加权排序
3. ✅ **在 `ChatService`** 中应用排序
4. ✅ **A/B 测试** 验证效果

### 未来考虑（阶段 2）
1. 📊 **收集用户行为数据**
   - 添加点击日志表
   - 记录搜索-点击-购买链路
2. 🔧 **构建特征工程**
   - 设计特征提取 pipeline
   - 建立特征存储
3. 🤖 **训练 XGBoost 模型**
   - 准备训练数据
   - 训练和评估模型
   - 部署到生产环境

---

## 总结

| 方案 | 复杂度 | 效果 | 数据需求 | 推荐度 |
|------|--------|------|----------|--------|
| **基于规则排序** | ⭐ 低 | ⭐⭐⭐ 好 | 无 | ✅ **推荐** |
| **XGBoost LTR** | ⭐⭐⭐ 高 | ⭐⭐⭐⭐⭐ 优秀 | 需要大量数据 | ⏳ 未来考虑 |

**建议**: 先实现基于规则的排序，如果效果满足需求，就不需要 XGBoost。如果后续有足够的数据和需求，再考虑引入 XGBoost。










