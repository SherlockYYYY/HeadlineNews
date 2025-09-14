package com.heima.search.config;

import com.heima.search.interceptor.AppTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    //添加拦截器生效  拦截器本身不是个组件 需要注册

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AppTokenInterceptor()).addPathPatterns("/**");
    }
}
