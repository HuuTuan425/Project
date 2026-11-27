package com.vuhuutuan.Project.config;

import com.vuhuutuan.Project.service.ExpiredStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ExpiredStockScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ExpiredStockScheduler.class);
    private final ExpiredStockService expiredStockService;
    private volatile boolean ready;

    public ExpiredStockScheduler(ExpiredStockService expiredStockService) {
        this.expiredStockService = expiredStockService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void processOnStartup() {
        ready = true;
        processExpiredStock();
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void processExpiredStock() {
        if (ready) {
            int removed = expiredStockService.removeExpiredStock(LocalDate.now());
            if (removed > 0) {
                logger.info("Đã tự động trừ {} sản phẩm hết hạn khỏi tồn kho.", removed);
            }
        }
    }
}
