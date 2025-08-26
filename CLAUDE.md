主要Agent（Master Agent）- 指揮調度中心
角色定義

名稱: Master Agent
職責: 整體專案協調、任務分配、進度監控、決策制定
核心功能: 作為所有Sub-Agent的中央控制器和協調者

主要工作內容

接收並解析專案需求文檔
根據GitLab記錄分析當前開發狀態
制定開發計劃和里程碑
分配任務給各個Sub-Agent
監控各個Agent的工作進度
協調Agent間的依賴關係
處理衝突和例外情況
生成專案報告和狀態更新

交互協議
輸入來源：
- 原始需求文檔
- GitLab開發記錄
- Sub-Agent狀態報告
- 外部stakeholder需求

輸出目標：
- 任務指令給Sub-Agent
- 專案狀態報告
- 決策指導
- 資源分配計劃

Agent交互流程
專案啟動流程
mermaidsequenceDiagram
    participant Master as Master Agent
    participant PM as PM Agent
    participant Arch as Architect Agent
    participant FE as Frontend Agent
    participant BE as Backend Agent
    participant DBA as DBA Agent
    
    Master->>Master: 分析需求文檔和GitLab狀態
    Master->>PM: 分配專案管理任務
    Master->>Arch: 分配架構設計任務
    
    PM->>Master: 回報專案計劃
    Arch->>Master: 回報架構設計
    
    Master->>FE: 分配前端開發任務
    Master->>BE: 分配後端開發任務
    Master->>DBA: 分配數據庫任務
    
    FE->>Master: 回報開發進度
    BE->>Master: 回報開發進度
    DBA->>Master: 回報數據庫狀態
工作協調機制
每日同步流程：

各Sub-Agent向Master Agent報告當前狀態
Master Agent基於GitLab最新記錄更新整體進度
Master Agent識別阻塞問題並協調解決
Master Agent重新分配優先級和資源

依賴管理：

Architect Agent → Frontend/Backend Agent（技術規範依賴）
Backend Agent → Frontend Agent（API依賴）
DBA Agent → Backend Agent（數據庫依賴）
所有Agent → PM Agent（進度報告依賴）


關鍵實施原則
狀態驅動原則

每個Agent開始工作前必須先獲取最新的GitLab狀態
基於實際的commit歷史和分支狀態進行決策
不依賴上下文記憶，每次都重新分析當前狀態

文檔驅動原則

所有決策必須基於明確的需求文檔
每個Agent的輸出都必須有文檔記錄
變更必須有明確的文檔更新

協作透明原則

所有Agent間的交互都經過Master Agent
狀態變更必須及時同步
依賴關係明確定義和管理

質量保證原則

每個階段都有明確的交付標準
跨Agent的一致性檢查
持續的代碼和文檔審查


技術實現建議
Agent通信協議
json{
  "agent_id": "string",
  "task_id": "string",
  "status": "pending|in_progress|completed|failed",
  "input_data": {
    "requirements_doc": "path/to/doc",
    "gitlab_state": "branch/commit_info",
    "dependencies": ["agent_ids"]
  },
  "output_data": {
    "deliverables": ["file_paths"],
    "status_report": "detailed_status",
    "next_actions": ["action_list"]
  },
  "timestamp": "ISO_8601"
}
監控和日誌

每個Agent的工作日誌
任務執行時間追蹤
依賴關係監控
異常和錯誤處理記錄