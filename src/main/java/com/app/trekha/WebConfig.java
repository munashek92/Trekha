package com.app.trekha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${trekha.storage.location:uploads}")
    private String storageLocation;
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + storageLocation + "/**")
                .addResourceLocations("file:" + storageLocation + "/");
    }
    
}
