package org.example.springboot.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.House;
import org.example.springboot.entity.LeaseRecord;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.LeaseRecordMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 租约到期处理定时任务
 */
@Slf4j
@Component
public class LeaseExpirationTask {

    @Resource
    private LeaseRecordMapper leaseRecordMapper;

    @Resource
    private HouseMapper houseMapper;

    /**
     * 每天凌晨2点执行，检查并处理到期租约
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void handleExpiredLeases() {
        LocalDate today = LocalDate.now();
        log.info("开始执行租约到期检查任务，当前日期：{}", today);

        try {
            // 查询所有到期的租约（状态为租赁中且结束日期小于等于今天）
            LambdaQueryWrapper<LeaseRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LeaseRecord::getStatus, 1) // 租赁中
                   .le(LeaseRecord::getEndDate, today); // 结束日期 <= 今天

            List<LeaseRecord> expiredLeases = leaseRecordMapper.selectList(wrapper);
            log.info("查询到 {} 条到期租约", expiredLeases.size());

            int processedCount = 0;
            int houseFreedCount = 0;

            for (LeaseRecord lease : expiredLeases) {
                // 检查是否有续签租约
                LambdaQueryWrapper<LeaseRecord> renewalWrapper = new LambdaQueryWrapper<>();
                renewalWrapper.eq(LeaseRecord::getOriginalLeaseId, lease.getId())
                             .eq(LeaseRecord::getIsRenewal, 1)
                             .eq(LeaseRecord::getStatus, 1); // 租赁中的续签租约

                LeaseRecord renewalLease = leaseRecordMapper.selectOne(renewalWrapper);

                // 更新原租约状态为已结束
                lease.setStatus(2);
                leaseRecordMapper.updateById(lease);
                processedCount++;

                log.info("租约 {} 已到期，状态已更新为已结束", lease.getId());

                // 如果没有续签租约，将房屋状态改为待出租
                if (renewalLease == null) {
                    House house = houseMapper.selectById(lease.getHouseId());
                    if (house != null && house.getStatus() == 2) { // 已出租
                        house.setStatus(1); // 待出租
                        houseMapper.updateById(house);
                        houseFreedCount++;

                        log.info("租约 {} 无续签，房屋 {} 状态已更新为待出租",
                                lease.getId(), house.getId());
                    }
                } else {
                    log.info("租约 {} 有续签租约 {}，房屋保持已出租状态",
                            lease.getId(), renewalLease.getId());
                }
            }

            log.info("租约到期检查任务完成，处理租约数：{}，释放房屋数：{}",
                    processedCount, houseFreedCount);

        } catch (Exception e) {
            log.error("租约到期检查任务执行失败", e);
        }
    }

    /**
     * 每天上午9点执行，发送续签提醒（到期前7天）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendRenewalReminders() {
        LocalDate today = LocalDate.now();
        LocalDate reminderDate = today.plusDays(7);

        log.info("开始执行续签提醒任务，提醒日期：{}", reminderDate);

        try {
            // 查询7天后到期的租约
            LambdaQueryWrapper<LeaseRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LeaseRecord::getStatus, 1) // 租赁中
                   .eq(LeaseRecord::getEndDate, reminderDate); // 7天后到期

            List<LeaseRecord> expiringSoonLeases = leaseRecordMapper.selectList(wrapper);
            log.info("查询到 {} 条需要提醒的租约", expiringSoonLeases.size());

            for (LeaseRecord lease : expiringSoonLeases) {
                // 检查是否已经有续签订单
                // TODO: 实现发送续签提醒通知给租客
                // 可以通过站内信、邮件、短信等方式通知
                log.info("租约 {} 将于7天后到期，需要发送续签提醒", lease.getId());
            }

            log.info("续签提醒任务完成");

        } catch (Exception e) {
            log.error("续签提醒任务执行失败", e);
        }
    }
}
