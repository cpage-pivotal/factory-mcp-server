package org.tanzu.factory;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.tanzu.factory.factory.FactoryService;
import org.tanzu.factory.supplychain.SupplyChainService;

import java.util.List;

@SpringBootApplication
public class FactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FactoryApplication.class, args);
    }

    @Bean
    public List<ToolCallback> registerTools(FactoryService factoryService, SupplyChainService supplyChainService) {
        return List.of(ToolCallbacks.from(factoryService, supplyChainService));
    }
}
