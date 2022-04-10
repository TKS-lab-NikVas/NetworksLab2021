package com.example.delservice.repository;

import com.example.delservice.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    @Override
    Optional<User> findById(Long aLong);


    Optional<User> findByUsername(String username);

}
