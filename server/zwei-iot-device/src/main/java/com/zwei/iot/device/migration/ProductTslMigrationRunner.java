package com.zwei.iot.device.migration;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.service.IProductTslService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@Slf4j
public class ProductTslMigrationRunner implements ApplicationRunner {

    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;
    private final IProductTslService productTslService;

    @Autowired
    public ProductTslMigrationRunner(ProductMapper productMapper,
                                      DeviceMapper deviceMapper,
                                      IProductTslService productTslService) {
        this.productMapper = productMapper;
        this.deviceMapper = deviceMapper;
        this.productTslService = productTslService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int existingProducts = productMapper.countAll();
        if (existingProducts > 0) {
            log.info("Product TSL migration already completed ({} products exist), skipping.", existingProducts);
            return;
        }

        List<Device> devices = deviceMapper.selectDeviceAll();
        if (devices == null || devices.isEmpty()) {
            log.info("No devices found, skipping TSL migration.");
            return;
        }

        log.info("Starting TSL migration for {} devices...", devices.size());
        int migrated = 0;
        int failed = 0;

        for (Device device : devices) {
            try {
                productTslService.regenerate(device.getId());
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate TSL for device id={} code={}: {}",
                        device.getId(), device.getCode(), e.getMessage());
                failed++;
            }
        }

        log.info("TSL migration complete: {} migrated, {} failed out of {} devices.",
                migrated, failed, devices.size());
    }
}
