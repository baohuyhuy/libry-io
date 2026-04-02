package io.libry.security.principal;

import io.libry.entity.Librarian;
import io.libry.repository.LibrarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LibrarianRepository librarianRepository;

    @Autowired
    public UserDetailsServiceImpl(LibrarianRepository librarianRepository) {
        this.librarianRepository = librarianRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Librarian librarian =
                librarianRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        return new UserDetailsImpl(librarian);
    }
}
