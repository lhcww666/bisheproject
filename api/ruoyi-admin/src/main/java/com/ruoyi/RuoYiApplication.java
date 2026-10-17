package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        System.setProperty("spring.devtools.restart.enabled", "false");
        // 兼容 IDEA 只编译 Java、未把 src/main/resources 拷到 target/classes 的情况。
        // 正常打包运行时 classpath 中已有 application.yml，不会走到这里。
        if (RuoYiApplication.class.getResource("/application.yml") == null
                && System.getProperty("spring.config.additional-location") == null)
        {
            System.setProperty("spring.config.additional-location",
                    "optional:file:./src/main/resources/,optional:file:./ruoyi-admin/src/main/resources/");
        }
        // IDEA 的 Make 可能只编译 Java、不拷贝 mapper 配置；这里只在其缺失时回退到源码目录。
        if (RuoYiApplication.class.getResource("/mybatis/mybatis-config.xml") == null
                && System.getProperty("mybatis-plus.config-location") == null)
        {
            System.setProperty("mybatis-plus.config-location",
                    "file:./ruoyi-admin/src/main/resources/mybatis/mybatis-config.xml");
        }
        if (RuoYiApplication.class.getResource("/mapper/module/RoomMapper.xml") == null
                && System.getProperty("mybatis-plus.mapperLocations") == null)
        {
            System.setProperty("mybatis-plus.mapperLocations",
                    "file:./ruoyi-biz/src/main/resources/mapper/**/*Mapper.xml,"
                            + "file:./ruoyi-system/src/main/resources/mapper/**/*Mapper.xml,"
                            + "file:./ruoyi-quartz/src/main/resources/mapper/**/*Mapper.xml,"
                            + "file:./ruoyi-generator/src/main/resources/mapper/**/*Mapper.xml");
        }
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  若依启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
