# 修复报告 — GlobalCalcService 编译错误

> 日期：2026-05-23
> 文件：`backend/src/main/java/com/obe/platform/modulec/service/GlobalCalcService.java`

## 问题

第 79 行编译报错：`无法解析符号 'CourseStatus'`

```java
// 修复前（第 79 行）
.map(CourseStatus::classId)
```

## 原因

`CourseStatus` 是 `DashboardData` 的内部 record。在 `DashboardData` 类体外引用时，必须使用全限定名 `DashboardData.CourseStatus`。此处 `.map()` 调用位于 `getDashboard()` 方法中，不在 `DashboardData` 的作用域内，编译器无法解析简写名。

## 修复

```java
// 修复后（第 79 行）
.map(DashboardData.CourseStatus::classId)
```

同一文件中其他 6 处 `DashboardData.CourseStatus` 引用均使用了全限定名，仅此一处遗漏。

## 影响范围

单行修复，无其他文件受影响。
