package com.schemenavigator.repository;

import com.schemenavigator.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByProviderAndProviderSubject(String provider, String providerSubject);
}
