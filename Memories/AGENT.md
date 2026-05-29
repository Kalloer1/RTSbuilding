每次運行前查看detailed_project.txt
做出任何修改後，都要把對核心tech stack和項目架構修改寫入detailed_project.txt，顆粒度高，而且結構嚴謹（不是“我這次新改了什麼‘l,而是明確大小標題展示項目架構）
以及寫入Journal.txt，高顆粒度，方便追蹤，不過結構可以不需要嚴謹
設計任何 UI 元素時，必須同時考慮英文、簡體中文、繁體中文三套文案與排版長度。
項目事實：根目錄 E:\RTSbuilding 是主項目，主項目就是 1.21.1；不要把 sister-projects\rtsbuilding-neoforge-26.1 當作 1.21.1 sister 項目，它完全是實驗性質，默認忽略、不要同步、不要構建、不要修改，除非用戶明確點名要求處理這個完整路徑。
本機正式 Java 21 路徑：C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot；需要 Java 21 構建時優先設定 JAVA_HOME 到此目錄，不要使用 .gradle-user-home\jdks 下的臨時/快取 JDK。

GitHub / 公開倉庫決策：
- GitHub 上不要把本地 `sister-projects` 作為主倉庫的一部分整包上傳；`sister-projects` 只是一個本機移植/對照工作區。
- 公開倉庫應以「每個分支根目錄都是可獨立構建的對應版本項目」為原則，而不是在同一個分支中塞多個版本子項目。
- 1.21.1 NeoForge 是主項目與主線；根目錄 `E:\RTSbuilding` 代表 1.21.1。GitHub default branch / mainline 應對應 1.21.1 NeoForge。
- 1.20.1 Forge 應放在獨立分支，例如 `forge-1.20.1`；發布到該分支時，分支根目錄應是 1.20.1 Forge 項目本身，而不是 `sister-projects/rtsbuilding-forge-1.20.1` 的嵌套目錄。
- 1.19.x 暫時不納入 GitHub 主發布策略；除非用戶明確要求，不要整理、發布或維護 1.19.x 分支。
- `sister-projects\rtsbuilding-neoforge-26.1` 仍然視為已廢棄/實驗性質，不要上傳到 GitHub，不要恢復，不要作為任何 1.21.1 主線依據。
- GitHub bundle 前要檢查並排除本機產物與快取：`build/`、`run/`、`.gradle/`、`.gradle-user-home/`、`outputs/`、`staging/`、`hs_err_pid*.log`、`replay_pid*.log` 等不應作為源碼提交，除非用戶明確指定某個文件需要保留。
