package vn.edu.tmdt.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.order.domain.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);
}
