# ============================================================
# KnowAI 前端镜像（多阶段构建）
#  stage 1: Node 构建 Vite 产物
#  stage 2: Nginx 托管静态文件 + 反向代理后端
# ============================================================

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /build

# 先复制依赖清单，利用缓存层加速
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
