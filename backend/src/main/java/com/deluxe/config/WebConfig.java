package com.deluxe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Phuc vu css, js, images tu static va cac file html tu templates
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/templates/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Dieu huong tu root "/" ve trang chu cong khai cho Guest: "/index.html"
        registry.addViewController("/").setViewName("forward:/index.html");
        // Tuong thich link giua room-single.html va rooms-single.html
        registry.addViewController("/room-single.html").setViewName("forward:/rooms-single.html");
        registry.addViewController("/portal/customer/room-single.html").setViewName("forward:/portal/customer/rooms-single.html");
        registry.addViewController("/login").setViewName("forward:/portal/auth/login.html");
        registry.addViewController("/login.html").setViewName("forward:/portal/auth/login.html");
        registry.addViewController("/register").setViewName("forward:/portal/auth/register.html");
        registry.addViewController("/register.html").setViewName("forward:/portal/auth/register.html");
        registry.addViewController("/access-denied").setViewName("forward:/portal/auth/access-denied.html");
        registry.addViewController("/access-denied.html").setViewName("forward:/portal/auth/access-denied.html");
    }
}
