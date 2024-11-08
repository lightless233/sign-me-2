## 1.1.0 - 2024-11-08
- 添加了从 request 获取原始包体 rawRequest 及原始请求体 body 的支持；
- 增加了 RAW 操作类型，可以替换完整请求体；
- 添加了修改位置为 JSON、XML、XML_ATTR、MULTIPART_ATTR、BODY 时替换完整请求体的支持；

## 1.0.1 - 2023-11-16
- 修复了访问 127.0.0.1 时跨域的 bug；