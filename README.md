# UnsafeClassAgent

一個用於捕獲並導出通過 `sun.misc.Unsafe.defineClass` 或其他方式動態加載的 Java 類字節碼的 Java Agent。

## 功能

- 自動攔截並保存動態生成的類字節碼。
- 支援通過參數自定義導出路徑。
- 使用 Javassist 進行字節碼操作。

## 編譯

確保你已安裝 Maven，然後執行以下命令：

```bash
mvn clean package
```

編譯完成後，你會在 `target` 目錄下看到 `unsafe-class-agent-1.0-SNAPSHOT-jar-with-dependencies.jar`。

## 使用方法

在啟動目標 Java 應用程序時，添加 `-javaagent` 參數。

### 基本用法（默認導出到 `dumped_classes` 目錄）：

```bash
java -javaagent:target/unsafe-class-agent-1.0-SNAPSHOT-jar-with-dependencies.jar -jar your-app.jar
```

### 指定導出路徑：

你可以將路徑作為參數傳遞給 Agent，格式為 `-javaagent:path/to/agent.jar=export_path`。

```bash
java -javaagent:target/unsafe-class-agent-1.0-SNAPSHOT-jar-with-dependencies.jar=/tmp/my_dumped_classes -jar your-app.jar
```

## 注意事項

- 本工具主要用於安全分析與逆向工程。
- 如果 `sun.misc.Unsafe.defineClass` 是 native 方法（在較新的 JVM 版本中），字節碼注入可能無法工作，但 `ClassFileTransformer` 仍然能捕捉到大部分動態加載的類。
