package io.libry.service.impl;

import io.libry.entity.Librarian;
import io.libry.entity.LibrarianPrincipal;
import io.libry.repository.LibrarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LibrarianDetailsServiceImpl implements UserDetailsService {

    private final LibrarianRepository librarianRepository;

    @Autowired
    public LibrarianDetailsServiceImpl(LibrarianRepository librarianRepository) {
        this.librarianRepository = librarianRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Librarian librarian = librarianRepository.findByUsername(username);

        if (librarian == null) {
            System.out.println("User Not Found");
            throw new UsernameNotFoundException("user not found");
        }

        return new LibrarianPrincipal(librarian);
    }
}
