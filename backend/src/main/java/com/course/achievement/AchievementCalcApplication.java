package com.course.achievement;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AchievementCalcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AchievementCalcApplication.class, args);
    }
}

@Component
class StartupRunner implements ApplicationRunner {

    private final Environment environment;

    public StartupRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     毕业要求达成度统一计算平台 - 后端服务启动成功！        ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                            ║");
        System.out.println("║  🎉 后端服务已启动                                        ║");
        System.out.println("║                                                            ║");
        System.out.println("║  📍 API地址: http://localhost:" + String.format("%-40s", port + contextPath) + "║");
        System.out.println("║                                                            ║");
        System.out.println("║  📚 API文档: http://localhost:" + String.format("%-30s", port + contextPath + "/swagger-ui.html") + "║");
        System.out.println("║                                                            ║");
        System.out.println("║  ❤️  健康检查: http://localhost:" + String.format("%-30s", port + contextPath + "/health") + "║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }
}
