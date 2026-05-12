package com.example.commerce.inventory.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<StockEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM StockEntity s WHERE s.sku = :sku")
  Optional<StockEntity> findBySkuForUpdate(@Param("sku") String sku);
}
