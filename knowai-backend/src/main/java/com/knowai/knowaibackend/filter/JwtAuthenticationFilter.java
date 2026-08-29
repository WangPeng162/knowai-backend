package com.knowai.knowaibackend.filter;

import com.knowai.knowaibackend.common.UserContext;
import com.knowai.knowaibackend.common.WhiteList;
import com.knowai.knowaibackend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        //1.白名单放行
        if (WhiteList.isWhitelisted(uri)){
            filterChain.doFilter(request,response);
            return;
        }

        //2.获取请求头token
        String token = request.getHeader("Authorization");

        //3.判断token是否存在
        if (token == null || !token.startsWith("Bearer ")){

            response.setStatus(401);
            response.getWriter().write("请先登录");
            return;
        }
        try {
            //4.解析token
            token = token.substring(7);
            Long userId = JwtUtil.getUserId(token);
            UserContext.setUserId(userId);
            //5.放行
            filterChain.doFilter(request,response);
        } catch (Exception e){
            response.setStatus(401);
            response.getWriter().write("token无效");
        } finally {
            UserContext.remove();
        }
    }
}
