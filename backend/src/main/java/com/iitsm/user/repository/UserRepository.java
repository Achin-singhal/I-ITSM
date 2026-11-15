package com.iitsm.user.repository;

import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User persistence.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.passwordHash = :passwordHash where u.email = :email and u.role = :role and u.active = true")
    int updateActiveUserPasswordHash(@Param("email") String email, @Param("role") Role role,
            @Param("passwordHash") String passwordHash);

    List<User> findByRoleAndActiveTrue(Role role);

    @Query(value = "select u from User u where (cast(:query as string) is null "
            + "or lower(u.email) like lower(concat('%', cast(:query as string), '%')) "
            + "or lower(u.fullName) like lower(concat('%', cast(:query as string), '%'))) ",
            countQuery = "select count(u) from User u where (cast(:query as string) is null "
                    + "or lower(u.email) like lower(concat('%', cast(:query as string), '%')) "
                    + "or lower(u.fullName) like lower(concat('%', cast(:query as string), '%'))) ")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    long countByRoleAndActiveTrue(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = :role and u.active = true order by u.id")
    List<User> lockActiveAdministrators(@Param("role") Role role);
}
