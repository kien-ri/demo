package com.kien.book.mapper

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface BooksTestMapper {
    @Select("SELECT MAX(id) FROM books")
    fun getMaxId(): Long?

    @Update("ALTER TABLE books AUTO_INCREMENT = 5")
    fun resetAutoIncrement()

}
