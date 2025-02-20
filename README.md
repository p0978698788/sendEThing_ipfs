# SendEveryThing - 即時資訊分享平台

![Tech Stack](https://img.shields.io/badge/tech-Vue3%20%7C%20Spring%20Boot%203-blue) ![Security](https://img.shields.io/badge/security-End--to--End%20Encryption-orange)

## 專案簡介
SendEveryThing 是一個現代化的即時資訊分享平台，整合 Vue.js 與 Spring Boot 技術，提供高效能、安全的檔案分享與即時通訊功能。此專案特別著重於資料安全性，採用私有 IPFS 網路確保檔案傳輸的安全性與效能。

### 核心功能
🔹 **安全檔案分享**
- 私有 IPFS 集群架構
- 檔案節點複製備援
- 與外部網路隔離的安全傳輸
- 支援大檔案傳輸

🔹 **即時聊天室**
- 整合 WebSocket 技術實現即時通訊
- 採用 AES-GCM 加密確保訊息安全
- 支援多人群組聊天

🔹 **進階檔案管理**
- 檔案分類與標籤功能
- 支援檔案版本控制

🔹 **安全性設計**
- JWT + Spring Security 身份驗證
- OAuth 2.0 整合（Google 登入）
- 端對端加密保護

## 技術架構

### 前端
🔹 **核心框架**
- Vue 3 Composition API
- Pinia 狀態管理
- Vite 開發環境
- Web Worker 多執行緒處理

### 後端
🔹 **應用框架**
- Spring Boot 3
- Spring Security
- WebSocket
- JWT / OAuth 2.0

### 資料儲存
🔹 **混合式儲存架構**
- MySQL：集中式資料管理，儲存檔案元數據與使用者資訊
- MongoDB：聊天訊息儲存
- 私有 IPFS 集群：
  - 多節點互連架構
  - 檔案複製備援機制
  - 與外部 IPFS 網路隔離
  - 優化的檔案傳輸效能

### 部署架構
🔹 **容器化部署**
- Nginx 反向代理
- Docker 容器化服務



## 系統架構圖
![架構圖](https://i.imgur.com/FHc0gzW.png)


## 專案特色

### 安全性設計
- 私有 IPFS 網路隔離機制
- 檔案節點複製備援策略
- 全程 HTTPS 加密傳輸
- 完整的身份驗證機制

### 效能優化
- 私有 IPFS 集群優化傳輸效能
- 前端多執行緒處理大量運算
- 資料庫讀寫分離設計
- 快取機制優化響應時間

### 擴展性設計
- IPFS 節點可動態擴展
- 彈性的資料庫架構
