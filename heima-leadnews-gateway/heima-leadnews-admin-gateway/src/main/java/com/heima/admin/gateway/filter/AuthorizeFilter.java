package com.heima.admin.gateway.filter;

import com.heima.admin.gateway.util.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component //注册为组件
@Slf4j //日志
public class AuthorizeFilter implements Ordered, GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){
        //获取请求对象和响应
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();


        //2.判断是否是登陆
       if( request.getURI().getPath().contains("/login")){
           //放行
           return chain.filter(exchange);  //继续调用下一层。让用户 进入登陆页面
       }
       //3.如果不是进入登陆页面。那就开始判断用户 是否登录  以及 token是否有效
        String token = request.getHeaders().getFirst("token");

       //4. 看看token是否为空
        if(StringUtils.isBlank( token)){
            //这里不用调用chain  因为不能继续执行
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //5.判断token是否有效  过期没有
        Claims claimsBody = AppJwtUtil.getClaimsBody(token);//这个token
        int res = AppJwtUtil.verifyToken(claimsBody);
        //@return -1：有效，0：有效，1：过期，2：过期
        if(res == 1 || res == 2){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //如果有效的话
        //获取用户信息  AppJwtUtil.java 的31行 claimMaps.put("id",id);
        Object userId = claimsBody.get("id",  Long.class);  //拿到用户id 这个 "id"是在login里添加的，jwtutil里定义的字段名称
        if(userId == null){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.error("用户id为空");
            return response.setComplete();
        }
        //存入header 以便后获取
        ServerHttpRequest req = request.mutate().headers(httpHeaders -> httpHeaders.add("userId", userId + "")).build();
        //重置请求
        exchange.mutate().request(req).build();
        //放行
        return chain.filter(exchange);
    }

    @Override  //直接把优先级当成最高
    public int getOrder(){
        return 0;
    }

}
