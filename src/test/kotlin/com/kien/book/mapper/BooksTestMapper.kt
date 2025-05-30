package com.kien.book.mapper

import com.kien.book.model.Book
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface BooksTestMapper {
    @Select("SELECT MAX(id) FROM books")
    fun getMaxId(): Long?

    @Update("ALTER TABLE books AUTO_INCREMENT = 5")
    fun resetAutoIncrement()

    @Select("SELECT * FROM books WHERE id = #{id}")
    fun getByIdIncludingDeleted(id: Long): Book
}
