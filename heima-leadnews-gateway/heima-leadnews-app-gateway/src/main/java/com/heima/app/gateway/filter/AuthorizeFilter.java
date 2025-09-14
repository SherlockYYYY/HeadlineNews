package com.heima.app.gateway.filter;
import com.heima.app.gateway.util.AppJwtUtil;
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

@Component
@Slf4j
public class AuthorizeFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request和response

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2.判断是否登陆
        if(request.getURI().getPath().contains("/login")){
            return chain.filter(exchange);
        }
        //3.获取token
        String token = request.getHeaders().getFirst("token");
        //4.判断token是否存在
        if(StringUtils.isBlank(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);  //返回401 未授权
            return response.setComplete();
        }
        //5.判断token是否有效
        try{
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            if(result==1 || result==2){
                response.setStatusCode(HttpStatus.UNAUTHORIZED);  //返回401 未授权
                return response.setComplete();
            }
            ///在保存用户搜索历史记录时新加入的获取token中的用户id，存入线程
            //获取用户信息
            Object userId = claimsBody.get("id",  Long.class);
            //存入header 以便后获取
            ServerHttpRequest se = request.mutate().headers(httpHeaders -> httpHeaders.add("userId", userId + "")).build();
            //重置请求
            exchange = exchange.mutate().request(se).build();
        }catch (Exception e){
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);  //返回401 未授权
            return response.setComplete();
        }
        //6.放行
        return chain.filter(exchange);
    }

    @Override  // 越小越先执行
    public int getOrder() {
        return 0;
    }

}
