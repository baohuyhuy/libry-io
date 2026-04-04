package io.libry.repository;

import io.libry.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);

    List<Book> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT SUM(b.quantity) FROM Book b")
    Long sumQuantity();

    @Query("SELECT b.genre, COUNT(b), SUM(b.quantity) FROM Book b GROUP BY b.genre")
    List<Object[]> countByGenre();
}
