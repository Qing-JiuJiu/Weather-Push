package com.xinqi;

import com.xinqi.job.SendMessageJob;
import com.xinqi.utils.ProjectUtils;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @author XinQi
 */
public class Main {

    static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, SchedulerException {
        //获取类当前路径
        String configPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //处理路径
        configPath = configPath.replace(new File(configPath).getName(), "");
        configPath = (URLDecoder.decode(configPath, "UTF-8") + "config.yaml");
        configPath = new File(configPath).getPath();

        //读取配置文件
        logger.info("正在从当前目录下读取配置文件config.yaml，请检查配置文件是否存在，目录：" + configPath);
        Map<String, Object> config = ProjectUtils.readYamlConfig(configPath, logger);

        //根据配置文件time内容配置七子表达式
        String time = String.valueOf(config.get("time"));
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);
        if (0 > hour || hour > 23 || minute < 0 || minute > 59) {
            logger.error("请检查配置文件time时间规范");
            System.exit(0);
        }
        String cron = "0 " + minute + " " + hour + " * * ? *";
        logger.info("已根据配置文件time：" + time + "，建立的Cron表达式：" + cron);

        // 1.创建调度器 Scheduler
        SchedulerFactory factory = new StdSchedulerFactory();
        Scheduler scheduler = factory.getScheduler();

        // 2.创建JobDetail实例，并与MyJob类绑定(Job执行内容)
        JobDetail job = JobBuilder.newJob(SendMessageJob.class).withIdentity("job1", "group1").usingJobData("configPath", configPath).build();

        // 3.构建Trigger实例,根据七子表达式绑定时间
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group1").startNow().withSchedule(CronScheduleBuilder.cronSchedule(cron)).build();

        // 4.执行，开启调度器
        scheduler.scheduleJob(job, trigger);
        scheduler.start();
        logger.info("已成功启动调度器，将在每日" + time + "定时发送天气短信，请确保配置文件各项参数内容正确");
    }

}