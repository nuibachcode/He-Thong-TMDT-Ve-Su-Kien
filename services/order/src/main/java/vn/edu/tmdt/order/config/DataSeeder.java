package vn.edu.tmdt.order.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.tmdt.order.domain.Voucher;
import vn.edu.tmdt.order.repository.VoucherRepository;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public ApplicationRunner seedVouchers(VoucherRepository vouchers) {
        return args -> {
            if (vouchers.count() > 0) {
                return;
            }
            voucher(vouchers, "WELCOME10", "Chào mừng khách mới", "PERCENT", 10, 500000,
                    BigDecimal.valueOf(300000), 200, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 6, 30));
            voucher(vouchers, "TOUR500", "Giảm 500k cho tour", "FIXED", 500000, 3000000,
                    null, 100, LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 31));
            voucher(vouchers, "HETHONG", "Mã đã hết hạn (demo lỗi)", "PERCENT", 20, 0,
                    null, 50, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
            log.info("Da seed 3 voucher demo");
        };
    }

    private void voucher(VoucherRepository repo, String code, String name, String type, long value,
                         long minOrder, BigDecimal maxDiscount, int limit, LocalDate from, LocalDate to) {
        Voucher voucher = new Voucher();
        voucher.code = code;
        voucher.name = name;
        voucher.discountType = type;
        voucher.discountValue = BigDecimal.valueOf(value);
        voucher.minOrderAmount = BigDecimal.valueOf(minOrder);
        voucher.maxDiscountAmount = maxDiscount;
        voucher.usageLimit = limit;
        voucher.usedCount = 0;
        voucher.validFrom = from.atStartOfDay(ZONE).toInstant();
        voucher.validTo = to.atTime(23, 59).atZone(ZONE).toInstant();
        voucher.active = true;
        repo.save(voucher);
    }
}
