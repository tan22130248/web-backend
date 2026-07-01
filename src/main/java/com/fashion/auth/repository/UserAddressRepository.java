package com.fashion.auth.repository;

import com.fashion.auth.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, String> {
    List<UserAddress> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<UserAddress> findByUserIdAndPhoneAndAddressAndWardCode(String userId, String phone, String address, String wardCode);
}
